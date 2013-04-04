package tankery.app.family.photos.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * FIXME: Now, the BitmapLoader just do the reload/recycle work.
 * in the future, we should merge the NetworkPhotoLoader's work to this class.
 *
 * @author tankery
 *
 */
public class BitmapLoader {

    static final String tag = "BitmapLoader";

    public interface BitmapReceivedCallback {
        void onBitmapRecieved(Bitmap bmp);
    }

    private static final int MSG_RELOAD = 1;
    private static final int MSG_RECYCLE = 2;

    private static final int MSG_BITMAP_RECIEVED = 10;


    private Context mApplicationContext = null;
    private HashMap<String, BitmapReceivedCallback> mListenerMap;

    private LoadRecycleHandler mLoadRecycleHandler;
    private UiTaskHandler mUiTaskHandler;


    public BitmapLoader(Context appContext) {
        mApplicationContext = appContext;
        mListenerMap = new HashMap<String, BitmapReceivedCallback>();

        // Create handler in an seperate thread.
        new Thread("LoadRecycleThread") {
            public void run() {
                Looper.prepare();
                mLoadRecycleHandler = new LoadRecycleHandler(BitmapLoader.this);
                Looper.loop();
            }
        }.start();
        // Create handler in UI thread.
        mUiTaskHandler = new UiTaskHandler(Looper.getMainLooper(), this);
    }

    public Context getContext() {
        return mApplicationContext;
    }

    public void startLoading(String urlStr, BitmapReceivedCallback listener) {
        mListenerMap.put(urlStr, listener);
        reload(urlStr);
    }

    private Handler getUiTaskHandler() {
        return mUiTaskHandler;
    }

    private static class LoadRecycleHandler extends Handler {

        private WeakReference<BitmapLoader> mLoader;

        LoadRecycleHandler(BitmapLoader loader) {
            mLoader = new WeakReference<BitmapLoader>(loader);
        }

        @Override
        public void handleMessage(Message msg) {
            String url = (String) msg.obj;
            switch (msg.what) {
            case MSG_RELOAD:
                doReload(url);
                break;
            case MSG_RECYCLE:
                doRecycle(url);
                break;
            default:
                break;
            }
        }

        void doReload(String key) {
            Log.d(tag, "reload " + key);
            if (key.isEmpty())
                return;

            int photoId = PhotoStorage.generateId(key);
            Bitmap bmp = PhotoStorage.getInstance().getPhoto(photoId);
            if (bmp == null || bmp.isRecycled()) {
                String name = String.valueOf(photoId);
                try {
                    FileInputStream fis = getFileInputStream(name);
                    final Bitmap newBmp = BitmapFactory.decodeStream(fis);
                    if (newBmp == null || newBmp.getWidth() <= 0 ||
                        newBmp.getHeight() <= 0) {
                        Log.e(tag, "Bitmap is null decode from " + name);
                        return;
                    }

                    PhotoStorage.getInstance().setPhoto(photoId, newBmp);

                    mLoader.get().getUiTaskHandler()
                                 .obtainMessage(MSG_BITMAP_RECIEVED, key)
                                 .sendToTarget();

                } catch (FileNotFoundException e) {
                    Log.e(tag, e.getMessage());
                    return;
                }
            }

        }

        void doRecycle(String key) {
            Log.d(tag, "recycle " + key);
            // set the bitmap in PhotoStorage will cause the old one
            // recycle.
            int photoId = PhotoStorage.generateId(key);
            PhotoStorage.getInstance().setPhoto(photoId, null);
        }

        // TODO: this really shouldn't be here!! Refactor, refactor, refactor....
        private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
            return new FileInputStream(mLoader.get()
                                .getContext()
                                .getApplicationContext()
                                .getCacheDir() + "/" + name);
        }
    }

    private static class UiTaskHandler extends Handler {

        private WeakReference<BitmapLoader> mLoader;

        public UiTaskHandler(Looper looper, BitmapLoader loader) {
            super(looper);
            mLoader = new WeakReference<BitmapLoader>(loader);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_BITMAP_RECIEVED:
                String key = (String) msg.obj;
                int photoId = PhotoStorage.generateId(key);
                Bitmap bmp = PhotoStorage.getInstance().getPhoto(photoId);
                mLoader.get().onBitmapRecieved(key, bmp);
                break;
            default:
                break;
            }
        }
    }

    /**
     * TODO: this part of code need to be refactored.
     * @param key
     */
    public void reload(String key) {
        if (mLoadRecycleHandler == null) {
            Log.w(tag, "mLoadRecycleHandler is null when reload.");
            return;
        }

        mLoadRecycleHandler.obtainMessage(MSG_RELOAD, key).sendToTarget();
    }

    /**
     * TODO: this method need been removed.
     * we should manager the resource life in the PhotoStorage.
     * @param key
     */
    public void recycle(String key) {
        if (mLoadRecycleHandler == null) {
            Log.w(tag, "mLoadRecycleHandler is null when recycle.");
            return;
        }

        if (mListenerMap.get(key) != null) {
            Log.i(tag, "cancle reload " + key);
            mListenerMap.remove(key);
        }
        mLoadRecycleHandler.obtainMessage(MSG_RECYCLE, key).sendToTarget();
    }

    private void onBitmapRecieved(String key, Bitmap bmp) {
        if (mListenerMap.get(key) != null) {
            mListenerMap.get(key).onBitmapRecieved(bmp);
            mListenerMap.remove(key);
        }
    }

}
