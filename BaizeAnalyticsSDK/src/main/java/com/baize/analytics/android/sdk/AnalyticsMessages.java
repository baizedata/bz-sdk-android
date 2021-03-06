/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Hong Yu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baize.analytics.android.sdk;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.baize.analytics.android.sdk.exceptions.ConnectErrorException;
import com.baize.analytics.android.sdk.exceptions.DebugModeException;
import com.baize.analytics.android.sdk.exceptions.InvalidDataException;
import com.baize.analytics.android.sdk.exceptions.ResponseErrorException;
import com.baize.analytics.android.sdk.util.Base64Coder;
import com.baize.analytics.android.sdk.util.JSONUtils;
import com.baize.analytics.android.sdk.util.NetworkUtils;
import com.baize.analytics.android.sdk.data.DbAdapter;
import com.baize.analytics.android.sdk.data.DbParams;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;


/**
 * Manage communication of events with the internal database and the Baize servers.
 * This class straddles the thread boundary between user threads and
 * a logical Baize thread.
 */
class AnalyticsMessages {
    private static final String TAG = "SA.AnalyticsMessages";
    private static final int FLUSH_QUEUE = 3;
    private static final int DELETE_ALL = 4;
    private static final Map<Context, AnalyticsMessages> S_INSTANCES = new HashMap<>();
    private final Worker mWorker;
    private final Context mContext;
    private final DbAdapter mDbAdapter;

    /**
     * 不要直接调用，通过 getInstance 方法获取实例
     */
    private AnalyticsMessages(final Context context) {
        mContext = context;
        mDbAdapter = DbAdapter.getInstance();
        mWorker = new Worker();
    }

    /**
     * 获取 AnalyticsMessages 对象
     *
     * @param messageContext Context
     */
    public static AnalyticsMessages getInstance(final Context messageContext) {
        synchronized (S_INSTANCES) {
            final Context appContext = messageContext.getApplicationContext();
            final AnalyticsMessages ret;
            if (!S_INSTANCES.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext);
                S_INSTANCES.put(appContext, ret);
            } else {
                ret = S_INSTANCES.get(appContext);
            }
            return ret;
        }
    }

    private static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    void enqueueEventMessage(final String type, final JSONObject eventJson) {
        try {
            synchronized (mDbAdapter) {
                int ret = mDbAdapter.addJSON(eventJson);
                if (ret < 0) {
                    String error = "Failed to enqueue the event: " + eventJson;
                    if (BaizeAPI.sharedInstance(mContext).isDebugMode()) {
                        throw new DebugModeException(error);
                    } else {
                        BzLog.i(TAG, error);
                    }
                }

                final Message m = Message.obtain();
                m.what = FLUSH_QUEUE;

                if (BaizeAPI.sharedInstance(mContext).isDebugMode() || ret ==
                        DbParams.DB_OUT_OF_MEMORY_ERROR) {
                    mWorker.runMessage(m);
                } else {
                    // track_signup 立即发送
                    if (type.equals("track_signup") || ret > BaizeAPI.sharedInstance(mContext)
                            .getFlushBulkSize()) {
                        mWorker.runMessage(m);
                    } else {
                        final int interval = BaizeAPI.sharedInstance(mContext).getFlushInterval();
                        mWorker.runMessageOnce(m, interval);
                    }
                }
            }
        } catch (Exception e) {
            BzLog.i(TAG, "enqueueEventMessage error:" + e);
        }
    }

    void flush() {
        final Message m = Message.obtain();
        m.what = FLUSH_QUEUE;

        mWorker.runMessage(m);
    }

    void flush(long timeDelayMills) {
        final Message m = Message.obtain();
        m.what = FLUSH_QUEUE;

        mWorker.runMessageOnce(m, timeDelayMills);
    }

    void deleteAll() {
        final Message m = Message.obtain();
        m.what = DELETE_ALL;

        mWorker.runMessage(m);
    }

    private void sendData() {
        try {
            if (!BaizeAPI.sharedInstance(mContext).isNetworkRequestEnable()) {
                BzLog.i(TAG, "NetworkRequest 已关闭，不发送数据！");
                return;
            }

            if (TextUtils.isEmpty(BaizeAPI.sharedInstance(mContext).getServerUrl())) {
                BzLog.i(TAG, "Server url is null or empty.");
                return;
            }

            //不是主进程
            if (!BaizeAPI.mIsMainProcess) {
                return;
            }

            //无网络
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return;
            }

            //不符合同步数据的网络策略
            String networkType = NetworkUtils.networkType(mContext);
            if (!NetworkUtils.isShouldFlush(networkType, BaizeAPI.sharedInstance(mContext).getFlushNetworkPolicy())) {
                BzLog.i(TAG, String.format("您当前网络为 %s，无法发送数据，请确认您的网络发送策略！", networkType));
                return;
            }
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }
        int count = 100;
        Toast toast = null;
        while (count > 0) {
            boolean deleteEvents = true;
            String[] eventsData;
            synchronized (mDbAdapter) {
                if (BaizeAPI.sharedInstance(mContext).isDebugMode()) {
                    /* debug 模式下服务器只允许接收 1 条数据 */
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 1);
                } else {
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 50);
                }
            }
            if (eventsData == null) {
                return;
            }

            final String lastId = eventsData[0];
            final String rawMessage = eventsData[1];
            String errorMessage = null;

            try {
                String data;
                try {
                    data = encodeData(rawMessage);
                } catch (Exception e) {
                    // 格式错误，直接将数据删除
                    throw new InvalidDataException(e);
                }
                sendHttpRequest(BaizeAPI.sharedInstance(mContext).getServerUrl(), data, rawMessage, false);
            } catch (ConnectErrorException e) {
                deleteEvents = false;
                errorMessage = "Connection error: " + e.getMessage();
            } catch (InvalidDataException e) {
                errorMessage = "Invalid data: " + e.getMessage();
            } catch (ResponseErrorException e) {
                deleteEvents = isDeleteEventsByCode(e.getHttpCode());
                errorMessage = "ResponseErrorException: " + e.getMessage();
            } catch (Exception e) {
                deleteEvents = false;
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                boolean isDebugMode = BaizeAPI.sharedInstance(mContext).isDebugMode();
                if (!TextUtils.isEmpty(errorMessage)) {
                    if (isDebugMode || BzLog.isLogEnabled()) {
                        BzLog.i(TAG, errorMessage);
                        if (isDebugMode && BaizeAPI.SHOW_DEBUG_INFO_VIEW) {
                            try {
                                /*
                                 * 问题：https://www.jianshu.com/p/1445e330114b
                                 * 目前没有比较好的解决方案，暂时规避，只对开启 debug 模式下有影响
                                 */
                                if (Build.VERSION.SDK_INT != 25) {
                                    if (toast != null) {
                                        toast.cancel();
                                    }
                                    toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            } catch (Exception e) {
                                BzLog.printStackTrace(e);
                            }
                        }
                    }
                }

                if (deleteEvents || isDebugMode) {
                    count = mDbAdapter.cleanupEvents(lastId);
                    BzLog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                } else {
                    count = 0;
                }

            }
        }
    }

    private void sendHttpRequest(String path, String data, String rawMessage, boolean isRedirects) throws ConnectErrorException, ResponseErrorException {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        BzLog.i(TAG, "request:" + path);
        try {
            final URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                BzLog.i(TAG, String.format("can not connect %s, it shouldn't happen", url.toString()), null);
                return;
            }
            if (BaizeAPI.sharedInstance().getSSLSocketFactory() != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(BaizeAPI.sharedInstance().getSSLSocketFactory());
            }
            connection.setInstanceFollowRedirects(false);
            if (BaizeAPI.sharedInstance(mContext).getDebugMode() == BaizeAPI.DebugMode.DEBUG_ONLY) {
                connection.addRequestProperty("Dry-Run", "true");
            }

            connection.setRequestProperty("Cookie", BaizeAPI.sharedInstance(mContext).getCookie(false));

            Uri.Builder builder = new Uri.Builder();
            //先校验crc
            if (!TextUtils.isEmpty(data)) {
                builder.appendQueryParameter("crc", String.valueOf(data.hashCode()));
            }

            builder.appendQueryParameter("gzip", "1");
            builder.appendQueryParameter("data_list", data);

            String query = builder.build().getEncodedQuery();
            if (TextUtils.isEmpty(query)) {
                return;
            }

            connection.setFixedLengthStreamingMode(query.getBytes(Base64Coder.CHARSET_UTF8).length);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);
            bout.write(query.getBytes(Base64Coder.CHARSET_UTF8));
            bout.flush();

            int responseCode = connection.getResponseCode();
            BzLog.i(TAG, "responseCode: " + responseCode);
            if (!isRedirects && BaizeHttpURLConnectionHelper.needRedirects(responseCode)) {
                String location = BaizeHttpURLConnectionHelper.getLocation(connection, path);
                if (!TextUtils.isEmpty(location)) {
                    closeStream(bout, out, null, connection);
                    sendHttpRequest(location, data, rawMessage, true);
                    return;
                }
            }
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            in.close();
            in = null;

            String response = new String(responseBody, Base64Coder.CHARSET_UTF8);
            if (BzLog.isLogEnabled()) {
                String jsonMessage = JSONUtils.formatJson(rawMessage);
                // 状态码 200 - 300 间都认为正确
                if (responseCode >= HttpURLConnection.HTTP_OK &&
                        responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    BzLog.i(TAG, "valid message: \n" + jsonMessage);
                } else {
                    BzLog.i(TAG, "invalid message: \n" + jsonMessage);
                    BzLog.i(TAG, String.format(Locale.CHINA, "ret_code: %d", responseCode));
                    BzLog.i(TAG, String.format(Locale.CHINA, "ret_content: %s", response));
                }
            }
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误
                throw new ResponseErrorException(String.format("flush failure with response '%s', the response code is '%d'",
                        response, responseCode), responseCode);
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    /**
     * 在服务器正常返回状态码的情况下，目前只有 (>= 500 && < 600) || 404 || 403 才不删数据
     *
     * @param httpCode 状态码
     * @return true: 删除数据，false: 不删数据
     */
    private boolean isDeleteEventsByCode(int httpCode) {
        boolean shouldDelete = true;
        if (httpCode == HttpURLConnection.HTTP_NOT_FOUND ||
                httpCode == HttpURLConnection.HTTP_FORBIDDEN ||
                (httpCode >= HttpURLConnection.HTTP_INTERNAL_ERROR && httpCode < 600)) {
            shouldDelete = false;
        }
        return shouldDelete;
    }

    private void closeStream(BufferedOutputStream bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                BzLog.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                BzLog.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                BzLog.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                BzLog.i(TAG, e.getMessage());
            }
        }
    }

    private String encodeData(final String rawMessage) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes(Base64Coder.CHARSET_UTF8).length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(rawMessage.getBytes(Base64Coder.CHARSET_UTF8));
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return new String(Base64Coder.encode(compressed));
    }

    // Worker will manage the (at most single) IO thread associated with
    // this AnalyticsMessages instance.
    // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
    private class Worker {

        private final Object mHandlerLock = new Object();
        private Handler mHandler;

        Worker() {
            final HandlerThread thread =
                    new HandlerThread("AnalyticsMessages.Worker",
                            Thread.MIN_PRIORITY);
            thread.start();
            mHandler = new AnalyticsMessageHandler(thread.getLooper());
        }

        void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    BzLog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    BzLog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
                        mHandler.sendMessageDelayed(msg, delay);
                    }
                }
            }
        }

        private class AnalyticsMessageHandler extends Handler {

            AnalyticsMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (msg.what == FLUSH_QUEUE) {
                        sendData();
                    } else if (msg.what == DELETE_ALL) {
                        try {
                            mDbAdapter.deleteAllEvents();
                        } catch (Exception e) {
                            BzLog.printStackTrace(e);
                        }
                    } else {
                        BzLog.i(TAG, "Unexpected message received by Baize worker: " + msg);
                    }
                } catch (final RuntimeException e) {
                    BzLog.i(TAG, "Worker threw an unhandled exception", e);
                }
            }
        }
    }
}