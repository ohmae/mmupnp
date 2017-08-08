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
     * 出力処理のインターフェース
     */
    public interface Print {
        /**
         * ログ出力を行う
         *
         * @param level   ログレベル
         * @param tag     タグ
         * @param message メッセージ
         */
        void println(int level, @Nonnull String tag, @Nonnull String message);
    }

    /**
     * System.outへ出力するデフォルトの出力処理
     */
    private static class DefaultPrint implements Print {
        private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public void println(final int level, @Nonnull final String tag, @Nonnull final String message) {
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

    public static final Print DEFAULT_PRINT = new DefaultPrint();
    @Nonnull
    private static Print sPrint = DEFAULT_PRINT;
    private static int sLogLevel = VERBOSE;
    private static boolean sAppendCaller = false;

    /**
     * 出力処理を変更する。
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
     * ERRORを指定した場合はERRORとASSERTのレベルのログが出力される。
     * <p>デフォルト値はVERBOSE
     *
     * @param level ログレベル。
     */
    public static void setLogLevel(final int level) {
        sLogLevel = level;
    }

    /**
     * 呼び出し元の情報をログに追加する。
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
    public static void v(@Nullable final String tag, @Nullable final String message) {
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
    public static void v(@Nullable final String tag, @Nullable final String message, @Nullable final Throwable tr) {
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
    public static void d(@Nullable final String tag, @Nullable final String message) {
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
    public static void d(@Nullable final String tag, @Nullable final String message, @Nullable final Throwable tr) {
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
    public static void i(@Nullable final String tag, @Nullable final String message) {
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
    public static void i(@Nullable final String tag, @Nullable final String message, @Nullable final Throwable tr) {
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
    public static void w(@Nullable final String tag, @Nullable final String message) {
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
    public static void w(@Nullable final String tag, @Nullable final Throwable tr) {
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
    public static void w(@Nullable final String tag, @Nullable final String message, @Nullable final Throwable tr) {
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
    public static void e(@Nullable final String tag, @Nullable final String message) {
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
    public static void e(@Nullable final String tag, @Nullable final String message, @Nullable final Throwable tr) {
        println(ERROR, tag, message, tr);
    }

    private static void println(
            final int level, @Nullable final String tag,
            @Nullable final String message, @Nullable final Throwable tr) {
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
    private static String makeTag(@Nullable final String tag, @Nullable final StackTraceElement element) {
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
    private static String makeMessage(@Nullable final String message, @Nullable final Throwable tr) {
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
