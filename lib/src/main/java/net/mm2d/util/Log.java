/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * デバッグログ出力クラス。
 *
 * <p>android.util.Logと同様のインターフェースで作成。
 * 出力先は{@link Print}インターフェースを実装したクラスで置換可能。
 * また、{@link #setLogLevel(int)}によりログレベルを動的に変更することが可能で
 * 指定したレベル以下のログを表示させないようにすることができる。
 *
 * <p>TAGを省略することも可能、nullを指定した場合も同様に動作する。
 * 省略もしくはnullを指定した場合、StackTraceから呼び出し元のクラスをTAGとして使用する。
 *
 * <p>デフォルトではすべてのログが{@code System.out}に出力される。
 *
 * <p>ログ出力を停止させる場合は初期化時に以下のように書けばよい。
 * <pre>{@code
 * Log.setLogLevel(Log.ASSERT);
 * Log.setPrint(Log.EMPTY_PRINT);
 * }</pre>
 *
 * <p>Androidで使用する場合の初期化例
 * <pre>{@code
 * if (BuildConfig.DEBUG) {
 *   Log.setAppendCaller(true);
 *   Log.setLogLevel(Log.VERBOSE);
 *   Log.setPrint((level, tag, message) -> {
 *     final String[] lines = message.split("\n");
 *     for (final String line : lines) {
 *       android.util.Log.println(level, tag, line);
 *     }
 *   });
 *   return;
 * }
 * Log.setLogLevel(Log.ASSERT);
 * Log.setPrint(Log.EMPTY_PRINT);
 * }</pre>
 *
 * <p>{@link #setAppendCaller(boolean)}でtrueを指定することにより、
 * IntelliJのコンソールからログ出力元へのリンクとなる出力を追加することができる。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Log {
    /**
     * ログレベルVERBOSE
     */
    public static final int VERBOSE = 2;
    /**
     * ログレベルDEBUG
     */
    public static final int DEBUG = 3;
    /**
     * ログレベルINFO
     */
    public static final int INFO = 4;
    /**
     * ログレベルWARN
     */
    public static final int WARN = 5;
    /**
     * ログレベルERROR
     */
    public static final int ERROR = 6;
    /**
     * ログレベルASSERT
     */
    public static final int ASSERT = 7;

    /**
     * 出力処理のインターフェース。
     *
     * このインターフェースを実装し
     * {@link #setPrint(Print)}で設定することによりログ出力方法を変更することができる。
     */
    public interface Print {
        /**
         * ログ出力を行う。
         *
         * @param level   ログレベル
         * @param tag     タグ
         * @param message メッセージ
         * @see #VERBOSE
         * @see #DEBUG
         * @see #INFO
         * @see #WARN
         * @see #ERROR
         * @see #ASSERT
         */
        void println(
                int level,
                @Nonnull String tag,
                @Nonnull String message);
    }

    /**
     * System.outへ出力するデフォルトの出力処理。
     */
    private static class DefaultPrint implements Print {
        private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public void println(
                final int level,
                @Nonnull final String tag,
                @Nonnull final String message) {
            final String[] lines = message.split("\n");
            final String prefix = getDateString() + levelToString(level) + "[" + tag + "] ";
            for (final String line : lines) {
                System.out.println(prefix + line);
            }
        }

        @Nonnull
        private String getDateString() {
            synchronized (FORMAT) {
                return FORMAT.format(new Date(System.currentTimeMillis()));
            }
        }

        @Nonnull
        private String levelToString(final int level) {
            switch (level) {
                case VERBOSE:
                    return " V ";
                case DEBUG:
                    return " D ";
                case INFO:
                    return " I ";
                case WARN:
                    return " W ";
                case ERROR:
                    return " E ";
                default:
                    return "   ";
            }
        }
    }

    /**
     * 何も行わない出力クラス。
     */
    private static class EmptyPrint implements Print {
        @Override
        public void println(
                final int level,
                @Nonnull final String tag,
                @Nonnull final String message) {
        }
    }

    /**
     * デフォルトのログ出力クラス。
     */
    public static final Print DEFAULT_PRINT = new DefaultPrint();
    /**
     * 何も行わない出力クラス。
     */
    public static final Print EMPTY_PRINT = new EmptyPrint();
    @Nonnull
    private static Print sPrint = DEFAULT_PRINT;
    private static int sLogLevel = VERBOSE;
    private static boolean sAppendCaller = false;

    /**
     * 出力処理を変更する。
     *
     * <p>指定しなければ{@link #DEFAULT_PRINT}が設定され、System.outに出力される。
     * {@link Print}インターフェースを実装したクラスを指定するコットで任意の出力処理に変更することができる。
     * {@link #EMPTY_PRINT}を設定することで出力処理を止めることができるが、出力の最終段までの処理は実行されるため、
     * 出力を停止させたい場合は、併せて{@link #setLogLevel(int)}で{@link #ASSERT}を指定するようにする。
     *
     * @param print 出力処理
     */
    public static void setPrint(@Nonnull final Print print) {
        sPrint = print;
    }

    /**
     * ログレベルを変更する。
     *
     * <p>設定した値以上のログを出力する。
     * ログレベルは低い順に
     * {@link #VERBOSE}、{@link #DEBUG}、{@link #INFO}、{@link #WARN}、{@link #ERROR}、{@link #ASSERT}、
     * が定義されている。
     * {@link #ERROR}を指定した場合は{@link #ERROR}と{@link #ASSERT}のレベルのログが出力される。
     *
     * <p>出力レベル未満の場合は出力のフォーマット処理も行われなくなるため、出力を抑止する場合は
     * {@link #setPrint(Print)}を変更するだけでなく、このメソッドで{@link #ASSERT}に出力レベルを変更することを推奨。
     *
     * <p>デフォルト値は{@link #VERBOSE}
     *
     * @param level ログレベル。
     * @see #VERBOSE
     * @see #DEBUG
     * @see #INFO
     * @see #WARN
     * @see #ERROR
     * @see #ASSERT
     */
    public static void setLogLevel(final int level) {
        sLogLevel = level;
    }

    /**
     * 呼び出し元の情報をログに追加する。
     *
     * <p>trueを指定すると、IntelliJのログコンソールから
     * ログ出力元のコードへのリンクとなる出力を追加することができる。
     * デバッグ時はtrueを指定することを推奨。
     *
     * <p>デフォルト値はfalse
     *
     * @param append 追加する場合true
     */
    public static void setAppendCaller(final boolean append) {
        sAppendCaller = append;
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * @param message メッセージ
     */
    public static void v(@Nullable final String message) {
        println(VERBOSE, null, message, null);
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * @param tag     タグ
     * @param message メッセージ
     */
    public static void v(
            @Nullable final String tag,
            @Nullable final String message) {
        println(VERBOSE, tag, message, null);
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tr Throwable
     */
    public static void v(@Nullable final Throwable tr) {
        println(VERBOSE, null, null, tr);
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag     タグ
     * @param message メッセージ
     * @param tr      Throwable
     */
    public static void v(
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        println(VERBOSE, tag, message, tr);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * @param message メッセージ
     */
    public static void d(@Nullable final String message) {
        println(DEBUG, null, message, null);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * @param tag     タグ
     * @param message メッセージ
     */
    public static void d(
            @Nullable final String tag,
            @Nullable final String message) {
        println(DEBUG, tag, message, null);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tr Throwable
     */
    public static void d(@Nullable final Throwable tr) {
        println(DEBUG, null, null, tr);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag     タグ
     * @param message メッセージ
     * @param tr      Throwable
     */
    public static void d(
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        println(DEBUG, tag, message, tr);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * @param message メッセージ
     */
    public static void i(@Nullable final String message) {
        println(INFO, null, message, null);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * @param tag     タグ
     * @param message メッセージ
     */
    public static void i(
            @Nullable final String tag,
            @Nullable final String message) {
        println(INFO, tag, message, null);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tr Throwable
     */
    public static void i(@Nullable final Throwable tr) {
        println(INFO, null, null, tr);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag     タグ
     * @param message メッセージ
     * @param tr      Throwable
     */
    public static void i(
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        println(INFO, tag, message, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * @param message メッセージ
     */
    public static void w(@Nullable final String message) {
        println(WARN, null, message, null);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * @param tag     タグ
     * @param message メッセージ
     */
    public static void w(
            @Nullable final String tag,
            @Nullable final String message) {
        println(WARN, tag, message, null);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tr Throwable
     */
    public static void w(@Nullable final Throwable tr) {
        println(WARN, null, null, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tag タグ
     * @param tr  Throwable
     */
    public static void w(
            @Nullable final String tag,
            @Nullable final Throwable tr) {
        println(WARN, tag, null, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag     タグ
     * @param message メッセージ
     * @param tr      Throwable
     */
    public static void w(
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        println(WARN, tag, message, tr);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * @param message メッセージ
     */
    public static void e(@Nullable final String message) {
        println(ERROR, null, message, null);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * @param tag     タグ
     * @param message メッセージ
     */
    public static void e(
            @Nullable final String tag,
            @Nullable final String message) {
        println(ERROR, tag, message, null);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを表示する。
     *
     * @param tr Throwable
     */
    public static void e(@Nullable final Throwable tr) {
        println(ERROR, null, null, tr);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * <p>引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag     タグ
     * @param message メッセージ
     * @param tr      Throwable
     */
    public static void e(
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        println(ERROR, tag, message, tr);
    }

    // VisibleForTesting
    static void println(
            final int level,
            @Nullable final String tag,
            @Nullable final String message,
            @Nullable final Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        if (!sAppendCaller) {
            sPrint.println(level, makeTag(tag, null), makeMessage(message, tr));
            return;
        }
        final StackTraceElement[] trace = new Throwable().getStackTrace();
        // println -> v/d/i/w/e -> ログコール場所
        if (trace.length < 3) {
            sPrint.println(level, makeTag(tag, null), makeMessage(message, tr));
            return;
        }
        final StackTraceElement element = trace[2];
        sPrint.println(level, makeTag(tag, element), element.toString() + " : " + makeMessage(message, tr));

    }

    @Nonnull
    private static String makeTag(
            @Nullable final String tag,
            @Nullable final StackTraceElement element) {
        if (tag != null) {
            return tag;
        }
        if (element != null) {
            return makeTag(element);
        }
        final StackTraceElement[] trace = new Throwable().getStackTrace();
        // makeTag -> println -> v/d/i/w/e -> ログコール場所
        if (trace.length < 4) {
            return "tag";
        }
        return makeTag(trace[3]);
    }

    @Nonnull
    private static String makeTag(@Nonnull final StackTraceElement element) {
        final String className = extractSimpleClassName(element);
        if (className.length() > 23) {
            return className.substring(0, 23);
        }
        return className;
    }

    @Nonnull
    private static String extractSimpleClassName(@Nonnull final StackTraceElement element) {
        String className = element.getClassName();
        final int dot = className.lastIndexOf('.');
        if (dot >= 0) {
            className = className.substring(dot + 1);
        }
        final int dollar = className.indexOf('$');
        if (dollar >= 0) {
            return className.substring(0, dollar);
        }
        return className;
    }

    @Nonnull
    private static String makeMessage(
            @Nullable final String message,
            @Nullable final Throwable tr) {
        if (message == null) {
            if (tr == null) {
                return "";
            }
            return getStackTraceString(tr);
        }
        if (tr == null) {
            return message;
        }
        return message + "\n" + getStackTraceString(tr);
    }

    @Nonnull
    private static String getStackTraceString(@Nonnull final Throwable tr) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
