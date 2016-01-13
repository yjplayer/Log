package com.log;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Log输出工具，自动获取TAG，支持通过log输出进入类。查看{@link #getCallInfo(int)}，{@link #i(String, boolean)}
 * 1:控制log是否打印{@link #init(boolean)}
 * 2:json的格式化输出{@link #logJson(String)}
 * 3:sharedPreferences的json形式打印，{@link #logSP(SharedPreferences)}
 * 4:支持log等级，可打印基本信息，Exception。{@link #v(String)}},{@link #e(String, Throwable)}
 * Created by air on 15/12/30.
 */
public class XLog {

    public static String TAG = "XLog";

    private static boolean DEBUG = true;

    /*json 的缩进距离*/
    public static final int INDENT_SPACES = 4;

    //    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /*换行*/
    public static final String LINE_SEPARATOR = "\n";
    /*Tab 缩进*/
    public static final String LINE_TAB = "\t";
    /*log的content为null的提示*/
    public static final String NULL_NOTIFICATION = "----- the content is null -----";
    /*log等级*/
    public final static int V = 0x0001;
    public final static int D = 0x0002;
    public final static int I = 0x0004;
    public final static int W = 0x0008;
    public final static int E = 0x0010;

    public final static int FLAG = 0xFFFF;

    /**
     * 在Application中初始化，是否打印log
     *
     * @param showLog
     */
    public static void init(boolean showLog) {
        DEBUG = showLog;
    }

    public static void v(String message) {
        log(V, message);
    }
    public static void d(String message) {
        log(D, message);
    }
    public static void i(String message) {
        log(I, message);
    }
    public static void w(String message) {
        log(W, message);
    }
    public static void e(String message) {
        log(E, message);
    }

    /**
     * 是否需要callInfo
     * @param message
     * @param needCallInfo
     */
    public static void v(String message,boolean needCallInfo){
        if (needCallInfo){
            log(V,message);
        }else if(DEBUG){
            if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;
            Log.v(TAG, message);
        }
    }
    public static void d(String message,boolean needCallInfo){
        if (needCallInfo){
            log(D,message);
        }else if(DEBUG){
            if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;
            Log.d(TAG, message);
        }
    }
    public static void i(String message,boolean needCallInfo){
        if (needCallInfo){
            log(I,message);
        }else if(DEBUG){
            if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;
            Log.i(TAG, message);
        }
    }
    public static void w(String message,boolean needCallInfo){
        if (needCallInfo){
            log(W,message);
        }else if(DEBUG){
            if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;
            Log.w(TAG, message);
        }
    }
    public static void e(String message,boolean needCallInfo){
        if (needCallInfo){
            log(E,message);
        }else if(DEBUG){
            if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;
            Log.e(TAG, message);
        }
    }

    /**
     * Exception 的打印
     *
     * @param message
     * @param tr
     */
    public static void e(String message, Throwable tr) {
        String error = getError(tr);
        log(E, message + LINE_SEPARATOR + error);
    }

    /**
     * 获取错误信息
     *
     * @param t
     * @return
     */
    public static String getError(Throwable t) {
        if (t == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            t.printStackTrace(new PrintStream(baos));
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return baos.toString();
    }

    private static void log(int level, String message) {
        if (!DEBUG) {
            return;
        }

        if (TextUtils.isEmpty(message)) message = NULL_NOTIFICATION;

        message = getCallInfo(2) + message;
        switch (level & FLAG) {
            case V:
                Log.v(TAG, message);
                break;
            case D:
                Log.d(TAG, message);
                break;
            case I:
                Log.i(TAG, message);
                break;
            case W:
                Log.w(TAG, message);
                break;
            case E:
                Log.e(TAG, message);
                break;
        }
    }

    /**
     * log 更美观的打印json
     *
     * @param json
     */
    public static void logJson(String json) {
        try {
            log(I, formatJson(json));
        } catch (Exception e) {
            log(E, "parse json error" + LINE_SEPARATOR + getError(e));
        }
    }

    /**
     * json的格式化,方便log查看
     *
     * @param json
     * @return
     */
    public static String formatJson(String json) throws Exception {
        String message = null;
        if (json.startsWith("{")) {
            JSONObject jsonObject = new JSONObject(json);
            message = jsonObject.toString(INDENT_SPACES);
        } else if (json.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(json);
            message = jsonArray.toString(INDENT_SPACES);
        }
        return message;
    }

    /**
     * 获取调用Xlog日志输出的类，方法信息。可以通过log进入方法。
     * example：MainActivity.onCreate (MainActivity.java:15)
     *
     * @param i
     * @return
     */
    private static String getCallInfo(int i) {
        int index = 3 + i;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement element = stackTrace[index];
//        for (StackTraceElement e : stackTrace) {
//            Log.i("LOG",e.getClassName()+"\t"+e.getMethodName());
//        }
            String fileName = element.getFileName();
            String call = null;
            int i1 = fileName.indexOf(".");
            if (i1 > 0) {
                call = fileName.substring(0, i1 + 1);
                TAG = fileName.substring(0, i1);
            }
            stringBuilder.append(call)
                    .append(element.getMethodName())
                    .append(LINE_TAB)
                    .append("(")
                    .append(element.getFileName()).append(":").append(element.getLineNumber())
                    .append(")")
                    .append(LINE_SEPARATOR);
        } catch (Exception e) {
            log(E, "getCallInfo error" + LINE_SEPARATOR + getError(e));
        }
        return stringBuilder.toString();
    }

    /**
     * json格式打印sharedPreferences中存储的信息
     *
     * @param sharedPreferences
     */
    public static void logSP(SharedPreferences sharedPreferences) {
        String message = null;
        try {
            Map<String, ?> all = sharedPreferences.getAll();
            JSONObject jsonObject = new JSONObject();
            Set<? extends Map.Entry<String, ?>> entries = all.entrySet();
            for (Map.Entry<String, ?> entry : entries) {
                String key = entry.getKey();
                Object value = entry.getValue();
                jsonObject.put(key, value);
            }
            message = jsonObject.toString();
        } catch (Exception e) {
            log(E, "parse sharedPreferences error" + LINE_SEPARATOR + getError(e));
        }
        try {
            log(I, formatJson(message));
        } catch (Exception e) {
            log(E, "parse json error" + LINE_SEPARATOR + getError(e));
        }
    }

    public static String formatXml(String xml) throws Exception {
        Source xmlInput = new StreamSource(new StringReader(xml));
        StreamResult xmlOutput = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString().replaceFirst(">", ">\n");
    }

    /**
     * xml的输出，{@link #logXml(File)}
     * @param xml
     */
    public static void logXml(String xml){
        try {
            log(I, formatXml(xml));
        } catch (Exception e) {
            log(E, "format xml error" + LINE_SEPARATOR + getError(e));
        }
    }

    public static void logXml(File file) {
        StringBuilder builder = new StringBuilder();
        String line;
        BufferedReader reader = null;
        try {
            FileReader fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                builder.append(line);
            }
        } catch (Exception e) {
            log(E, "read xml error" + LINE_SEPARATOR + getError(e));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            log(I, formatXml(builder.toString()));
        } catch (Exception e) {
            log(E, "format xml error" + LINE_SEPARATOR + getError(e));
        }
    }
}
