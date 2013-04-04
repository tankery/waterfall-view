package tankery.app.family.photos.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class WebResourceLoader extends Thread {

    static final String tag = "WebResourceLoader";

    public enum NetworkError {
        NO_ERROR,
        BROCKEN_URL,
        UNKNOW_IO_ERROR,
        CONNECTION_TIMEOUT,
        DATA_READING_TIMEOUT,
        RESOURCE_NOT_FOUNT,
        UNKNOW_NETWORK_ERROR
    }

    public interface StreamDecoder {
        Object decodeFromStream(String url, InputStream is);
    }

    public interface WebResourceCallback {
        void onResourceReceived(String url, Object resource);
        void onError(String url, NetworkError error);
    }

    private class ResourceRequest {
        public String mUrl;
        public WebResourceCallback mCallback;
        public StreamDecoder mDecoder;
        public Object mResponseData;

        public ResourceRequest(String url,
            WebResourceCallback callback, StreamDecoder decoder) {
            mUrl = url;
            mCallback = callback;
            mDecoder = decoder;
        }
    }

    private static class UiTaskHandler extends Handler {

        public UiTaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            final NetworkError[] errors = NetworkError.values();
            if (msg.what >= errors.length)
                return;

            ResourceRequest request = (ResourceRequest) msg.obj;
            WebResourceCallback callback = request.mCallback;

            if (errors[msg.what] == NetworkError.NO_ERROR) {
                callback.onResourceReceived(request.mUrl, request.mResponseData);
            }
            else
                callback.onError(request.mUrl, errors[msg.what]);
        }
    }

    static final int HTTP_CONNECT_TIMEOUT = 3000;
    static final int HTTP_READ_TIMEOUT = 5000;

    // Default stream decoder will do nothing but return the stream directly.
    final static private StreamDecoder mDefaultStreamDecoder = new StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            return is;
        }

    };

    private UiTaskHandler mUiTaskHandler;

    private Queue<ResourceRequest> mResourceRequestList;

    public WebResourceLoader() {
        super("WebResourceLoader");

        // Create handler in UI thread.
        mUiTaskHandler = new UiTaskHandler(Looper.getMainLooper());

        mResourceRequestList = new ConcurrentLinkedQueue<ResourceRequest>();

        // start the thread.
        start();
    }

    public synchronized void addResourceLoadRequest(String url,
            WebResourceCallback callback, StreamDecoder decoder) {
        if (decoder == null)
            decoder = mDefaultStreamDecoder;
        mResourceRequestList.add(new ResourceRequest(url, callback, decoder));
        wakeUp();
    }

    // stop loading will remove all request.
    public void stopLoading() {
        mUiTaskHandler.removeMessages(NetworkError.NO_ERROR.ordinal());
        mResourceRequestList.clear();
        wakeUp();
    }

    private void wakeUp() {
        synchronized (mResourceRequestList) {
            mResourceRequestList.notify();
        }
    }


    /**************************************************
     * WebResourceLoader thread
     **************************************************/
    @Override
    public void run() {
        while (!isInterrupted()) {
            while (!mResourceRequestList.isEmpty()) {
                ResourceRequest request = mResourceRequestList.poll();
                startRequest(request);
            }

            try {
                synchronized (mResourceRequestList) {
                    // wait for new request.
                    mResourceRequestList.wait();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void startRequest(ResourceRequest request) {
        String urlStr = request.mUrl;
        if (urlStr == null || urlStr.isEmpty() || request.mCallback == null)
            return;

        try {
            Object obj = null;
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            try {
                conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
                conn.setReadTimeout(HTTP_READ_TIMEOUT);
                conn.setDoInput(true);
                String protocol = url.getProtocol();
                if (protocol.equals("file") ||
                    (protocol.equals("http") && ((HttpURLConnection) conn).getResponseCode()
                        == HttpURLConnection.HTTP_OK)) {
                    InputStream is = conn.getInputStream();
                    obj = request.mDecoder.decodeFromStream(urlStr, is);
                }

                if (obj != null) {
                    request.mResponseData = obj;
                    mUiTaskHandler.obtainMessage(NetworkError.NO_ERROR.ordinal(),
                                                 request).sendToTarget();
                } else
                    mUiTaskHandler.obtainMessage(NetworkError.RESOURCE_NOT_FOUNT.ordinal(),
                                                 request).sendToTarget();
            } catch (SocketException e) {
                Log.e(tag, (e.getMessage() == null ?
                        "Unknow Socket exception" :
                        e.getMessage()));
                mUiTaskHandler.obtainMessage(NetworkError.CONNECTION_TIMEOUT.ordinal(),
                                             request).sendToTarget();
            } catch (SocketTimeoutException e) {
                Log.e(tag, (e.getMessage() == null ?
                        "Unknow SocketTimeout exception" :
                        e.getMessage()));
                mUiTaskHandler.obtainMessage(NetworkError.DATA_READING_TIMEOUT.ordinal(),
                                             request).sendToTarget();
            } catch (IOException e) {
                Log.e(tag, e.getClass().getName() + ": " +
                           (e.getMessage() == null ?
                                   "Unknow IO exception" :
                                   e.getMessage()));
                mUiTaskHandler.obtainMessage(NetworkError.UNKNOW_NETWORK_ERROR.ordinal(),
                                             request).sendToTarget();
            }
        } catch (MalformedURLException e) {
            Log.e(tag, e.getMessage());
            mUiTaskHandler.obtainMessage(NetworkError.BROCKEN_URL.ordinal(),
                                         request).sendToTarget();
        } catch (IOException e) {
            Log.e(tag, e.getClass().getName() + ": " +
                       (e.getMessage() == null ?
                               "Unknow IO exception" :
                               e.getMessage()));
            mUiTaskHandler.obtainMessage(NetworkError.UNKNOW_IO_ERROR.ordinal(),
                                         request).sendToTarget();
        }
    }

}
