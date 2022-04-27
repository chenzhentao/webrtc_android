package com.dds.skywebrtc;


import android.util.Log;

public class Logger   {


    private static String LOG_ID_MAIN = "screencast";

    public static void v(String tag, String msg) {
       Log.v(tag,msg);
    }

    /**
     * Send a {VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
       Log.v(LOG_ID_MAIN," tag = "+tag+" msg = "+msg,tr);
    }

    /**
     * Send a {DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
       Log.d(LOG_ID_MAIN," tag = "+tag+" msg = "+msg);
    }

    /**
     * Send a {DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
       Log.d(LOG_ID_MAIN," tag = "+tag+" msg = "+msg,tr);
    }

    /**
     * Send an {INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
       Log.i(LOG_ID_MAIN," tag = "+tag+" msg = "+msg);
    }

    /**
     * Send a {INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
       Log.i(LOG_ID_MAIN," tag = "+tag+" msg = "+msg,tr);
    }

    /**
     * Send a {WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
       Log.w(LOG_ID_MAIN,  " tag = "+tag+" msg = "+msg);
    }

    /**
     * Send a {WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
       Log.w(LOG_ID_MAIN,  " tag = "+tag+" msg = "+msg + '\n' ,tr);
    }

    /*
     * Send a {WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static void w(String tag, Throwable tr) {
       Log.w(LOG_ID_MAIN,  tag, tr);
    }

    /**
     * Send an {ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
       Log.e(LOG_ID_MAIN, " tag = "+tag+" msg = "+ msg);
    }

    /**
     * Send a {ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
       Log.e(LOG_ID_MAIN,  " tag = "+tag+" msg = "+msg ,tr);
    }
}
