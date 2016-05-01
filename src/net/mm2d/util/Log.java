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

/**
 * デバッグログ出力クラス。
 *
 * android.util.Logと同様のインターフェースで作成。
 * 出力先は{@link Print}インターフェースを実装したクラスで置換可能。
 * また、{@link #setLogLevel(int)}によりログレベルを動的に変更することが可能で
 * 指定したレベル以下のログを表示させないようにすることができる。
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
     * ログれレベルARRERT
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
        void println(int level, String tag, String message);
    }

    /**
     * System.outへ出力するデフォルトの出力処理
     */
    private static class DefaultPrint implements Print {
        private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public void println(int level, String tag, String message) {
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
    public static void setPrint(Print print) {
        sPrint = print;
    }

    /**
     * ログレベルを変更する。
     *
     * 設定した値以上のログを出力する。
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
    public static void v(String tag, String message) {
        println(VERBOSE, tag, message);
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
    public static void v(String tag, String message, Throwable tr) {
        println(VERBOSE, tag, message, tr);
    }

    /**
     * DEBUGレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void d(String tag, String message) {
        println(DEBUG, tag, message);
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
    public static void d(String tag, String message, Throwable tr) {
        println(DEBUG, tag, message, tr);
    }

    /**
     * INFOレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void i(String tag, String message) {
        println(INFO, tag, message);
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
    public static void i(String tag, String message, Throwable tr) {
        println(INFO, tag, message, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void w(String tag, String message) {
        println(WARN, tag, message);
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
    public static void w(String tag, String message, Throwable tr) {
        println(WARN, tag, message, tr);
    }

    /**
     * WARNレベルでのログ出力を行う。
     *
     * 引数のThrowableのスタックトレースをメッセージとして表示する。
     *
     * @param tag タグ
     * @param tr Throwable
     */
    public static void w(String tag, Throwable tr) {
        println(WARN, tag, tr);
    }

    /**
     * ERRORレベルでのログ出力を行う。
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public static void e(String tag, String message) {
        println(ERROR, tag, message);
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
    public static void e(String tag, String message, Throwable tr) {
        println(ERROR, tag, message, tr);
    }

    private static void println(int level, String tag, Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        println(level, tag, getStackTraceString(tr));
    }

    private static void println(int level, String tag, String message, Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        println(level, tag, message + "\n" + getStackTraceString(tr));
    }

    private static void println(int level, String tag, String message) {
        if (level < sLogLevel) {
            return;
        }
        sPrint.println(level, tag, message);
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

}
