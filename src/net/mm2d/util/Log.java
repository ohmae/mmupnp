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
 * <p>TAGにnullを指定することもできる。
 * nullを指定した場合はStackTraceから呼び出し元の場所をTAGとして使用する。
 * コストが大きいため常時出力されるログには使用しないこと。
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
         * @param level ログレベル
         * @param tag タグ
         * @param message メッセージ
         */
        void println(int level, @Nullable String tag, @Nullable String message);
    }

    /**
     * System.outへ出力するデフォルトの出力処理
     */
    private static class DefaultPrint implements Print {
        private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public void println(int level, @Nullable String tag, @Nullable String message) {
            synchronized (FORMAT) {
                final StringBuilder sb = new StringBuilder();
                sb.append(FORMAT.format(new Date(System.currentTimeMillis())));
                switch (level) {
                    default:
                    case VERBOSE:
                        sb.append(" V ");
                        break;
                    case DEBUG:
                        sb.append(" D ");
                        break;
                    case INFO:
                        sb.append(" I ");
                        break;
                    case WARN:
                        sb.append(" W ");
                        break;
                    case ERROR:
                        sb.append(" E ");
                        break;
                }
                sb.append("[");
                sb.append(tag);
                sb.append("] ");
                sb.append(message);
                System.out.println(sb.toString());
            }
        }
    }

// for Android Logcat
//    private static class AndroidPrint implements Print {
//        @Override
//        public void println(int level, String tag, String message) {
//          switch(level) {
//              default:
//              case VERBOSE:
//                  android.util.Log.v(tag, message);
//                  break;
//              case DEBUG:
//                  android.util.Log.d(tag, message);
//                  break;
//              case INFO:
//                  android.util.Log.i(tag, message);
//                  break;
//              case WARN:
//                  android.util.Log.w(tag, message);
//                  break;
//              case ERROR:
//                  android.util.Log.e(tag, message);
//                  break;
//          }
//        }
//    }

    private static Print sPrint = new DefaultPrint();
    private static int sLogLevel = VERBOSE;

    /**
     * 出力処理を変更する。
     *
     * @param print 出力処理
     */
    public static void setPrint(@Nonnull Print print) {
        sPrint = print;
    }

    /**
     * ログレベルを変更する。
     *
     * <p>設定した値以上のログを出力する。
     * ERRORを指定した場合はERRORとASSERTのレベルのログが出力される。
     *
     * @param level ログレベル。
     */
    public static void setLogLevel(int level) {
        sLogLevel = level;
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void v(@Nullable String tag, @Nullable String message) {
        log(VERBOSE, tag, message);
    }

    /**
     * VERBOSEレベルでのログ出力を行う。
     *
     * 引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag タグ
     * @param message メッセージ
     * @param tr Throwable
     */
    public static void v(@Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        log(VERBOSE, tag, message, tr);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void d(@Nullable String tag, @Nullable String message) {
        log(DEBUG, tag, message);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * 引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag タグ
     * @param message メッセージ
     * @param tr Throwable
     */
    public static void d(@Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        log(DEBUG, tag, message, tr);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void i(@Nullable String tag, @Nullable String message) {
        log(INFO, tag, message);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * 引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag タグ
     * @param message メッセージ
     * @param tr Throwable
     */
    public static void i(@Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        log(INFO, tag, message, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void w(@Nullable String tag, @Nullable String message) {
        log(WARN, tag, message);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * 引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag タグ
     * @param message メッセージ
     * @param tr Throwable
     */
    public static void w(@Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        log(WARN, tag, message, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * 引数のThrowableのスタックトレースをメッセージとして表示する。
     *
     * @param tag タグ
     * @param tr Throwable
     */
    public static void w(@Nullable String tag, @Nullable Throwable tr) {
        log(WARN, tag, tr);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void e(@Nullable String tag, @Nullable String message) {
        log(ERROR, tag, message);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * 引数のThrowableを元にスタックトレースを合わせて表示する。
     *
     * @param tag タグ
     * @param message メッセージ
     * @param tr Throwable
     */
    public static void e(@Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        log(ERROR, tag, message, tr);
    }

    private static void log(int level, @Nullable String tag, @Nullable Throwable tr) {
        println(level, tag, null, tr);
    }

    private static void log(int level, @Nullable String tag, @Nullable String message) {
        println(level, tag, message, null);
    }

    private static void log(
            int level, @Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        println(level, tag, message, tr);
    }

    private static void println(
            int level, @Nullable String tag, @Nullable String message, @Nullable Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        if (tag == null) {
            try {
                // println -> log -> v/d/i/w/e -> ログコール場所
                tag = new Throwable().getStackTrace()[3].toString();
            } catch (final Exception ignored) { // 念のため
                tag = "tag";
            }
        }
        if (tr == null) {
            sPrint.println(level, tag, message);
        } else if (message == null) {
            sPrint.println(level, tag, getStackTraceString(tr));
        } else {
            sPrint.println(level, tag, message + "\n" + getStackTraceString(tr));
        }
    }

    @Nonnull
    private static String getStackTraceString(@Nonnull Throwable tr) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
