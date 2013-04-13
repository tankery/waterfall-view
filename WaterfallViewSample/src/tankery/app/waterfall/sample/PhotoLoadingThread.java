package tankery.app.waterfall.sample;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class PhotoLoadingThread extends Thread  {

    public interface BitmapReceivedCallback {
        void onBitmapRecieved(Bitmap bmp);
    }

    private class LoadingRequest {
        public String mKey;
        public BitmapReceivedCallback mCallback;
        public Bitmap mResultBitmap;

        LoadingRequest(String key, BitmapReceivedCallback callback) {
            mKey = key;
            mCallback = callback;
        }
    }

    private Context mContext;
    private Queue<LoadingRequest> mLoadingRequestQueue;
    private Object mThreadSignal = new Object();
    private UiTaskHandler mUiTaskHandler;

    public PhotoLoadingThread(Context context) {
        super("PhotoLoadingThread");
        mContext = context;
        mLoadingRequestQueue = new ConcurrentLinkedQueue<LoadingRequest>();
        // Create handler in UI thread.
        mUiTaskHandler = new UiTaskHandler(Looper.getMainLooper());
    }

    public void startLoading(String key, BitmapReceivedCallback listener) {
        mLoadingRequestQueue.add(new LoadingRequest(key, listener));
        wakeUp();
    }

    public void stopLoading() {
        mLoadingRequestQueue.clear();
    }

    public boolean hasPendingLoad() {
        return !mLoadingRequestQueue.isEmpty();
    }

    private void wakeUp() {
        synchronized (mThreadSignal) {
            mThreadSignal.notify();
        }
    }

    public void run() {
        while (true) {
            while (!mLoadingRequestQueue.isEmpty()) {
                LoadingRequest request = mLoadingRequestQueue.poll();
                doLoading(request);
            }

            synchronized (mThreadSignal) {
                try {
                    mThreadSignal.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void doLoading(LoadingRequest request) {
        BufferedInputStream buf;
        try {
            buf = new BufferedInputStream(mContext.getAssets()
                    .open(request.mKey));
            request.mResultBitmap = BitmapFactory.decodeStream(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mUiTaskHandler.obtainMessage(0, request).sendToTarget();
    }

    private static class UiTaskHandler extends Handler {

        public UiTaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            LoadingRequest request = (LoadingRequest) msg.obj;
            BitmapReceivedCallback callback = request.mCallback;
            if (callback != null) {
                callback.onBitmapRecieved(request.mResultBitmap);
            }
        }
    }

}
