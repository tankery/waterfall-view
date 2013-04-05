package tankery.app.family.photos.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import tankery.app.family.photos.net.WebResourceLoader.WebResourceCallback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

class PhotoDiskCache {

    final static String tag = "PhotoDiskCache";

    private static final int MSG_RELOAD = 1;
    private static final int MSG_SAVE = 2;

    private static final int MSG_BITMAP_RECIEVED = 10;
    private static final int MSG_LOADING_ERROR = 11;

    private static class WebBitmap {
        public String mUrl;
        public Bitmap mBitmap;

        public WebBitmap(String url, Bitmap bmp) {
            mUrl = url;
            mBitmap = bmp;
        }
    }

    private static class DiskIOHandler extends Handler {

        private WeakReference<PhotoDiskCache> mHost;

        DiskIOHandler(PhotoDiskCache host) {
            mHost = new WeakReference<PhotoDiskCache>(host);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_RELOAD:
                String url = (String) msg.obj;
                if (!doReload(url))
                    mHost.get().getUiTaskHandler()
                                 .obtainMessage(MSG_LOADING_ERROR, url)
                                 .sendToTarget();
                break;
            case MSG_SAVE:
                WebBitmap wbmp = (WebBitmap) msg.obj;
                String key = wbmp.mUrl;
                Bitmap bmp = wbmp.mBitmap;
                doSave(key, bmp);
                break;
            default:
                break;
            }
        }

        boolean doReload(String key) {
            Log.d(tag, "reload " + key);
            if (key.isEmpty())
                return false;

            Bitmap newBmp = mHost.get().getResource(key);

            if (newBmp == null)
                return false;

            mHost.get().getUiTaskHandler()
                         .obtainMessage(MSG_BITMAP_RECIEVED, new WebBitmap(key, newBmp))
                         .sendToTarget();
            return true;
        }

        void doSave(String key, Bitmap bmp) {
            mHost.get().saveResourceToFile(key, bmp);
        }
    }

    private static class UiTaskHandler extends Handler {

        private WeakReference<PhotoDiskCache> mHost;

        public UiTaskHandler(Looper looper, PhotoDiskCache host) {
            super(looper);
            mHost = new WeakReference<PhotoDiskCache>(host);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_BITMAP_RECIEVED:
                WebBitmap wbmp = (WebBitmap) msg.obj;
                mHost.get().onBitmapRecieved(wbmp.mUrl, wbmp.mBitmap);
                break;
            case MSG_LOADING_ERROR:
                String url = (String) msg.obj;
                mHost.get().onLoadingError(url);
                break;
            default:
                break;
            }
        }
    }

    private String mCacheDirName;
    private ArrayList<String> mCachedFileList = null;

    private Object mDiskIOThreadNotifier = new Object();
    private DiskIOHandler mDiskIOHandler;
    private UiTaskHandler mUiTaskHandler;
    private WebResourceCallback mWebResourceCallback;

    public PhotoDiskCache(WebResourceCallback callback) {
        mWebResourceCallback = callback;
        mCachedFileList = new ArrayList<String>();

        // Create handler in an seperate thread.
        new Thread("DiskIOThread") {
            public void run() {
                Looper.prepare();
                mDiskIOHandler = new DiskIOHandler(PhotoDiskCache.this);
                synchronized (mDiskIOThreadNotifier) {
                    mDiskIOThreadNotifier.notify();
                }
                Looper.loop();
            }
        }.start();
        // Wait to create.
        synchronized (mDiskIOThreadNotifier) {
            try {
                mDiskIOThreadNotifier.wait();
            } catch (InterruptedException e) {
            }
        }
        // Create handler in UI thread.
        mUiTaskHandler = new UiTaskHandler(Looper.getMainLooper(), this);
    }

    public void setCacheDir(String dir) {
        mCacheDirName = dir;
        mCachedFileList.clear();
        mCachedFileList.addAll(Arrays.asList(new File(mCacheDirName).list()));
    }

    public void addReloadRequest(String key) {
        mDiskIOHandler.obtainMessage(MSG_RELOAD, key).sendToTarget();
    }

    public void addSaveRequest(String key, Bitmap bmp) {
        mDiskIOHandler.obtainMessage(MSG_SAVE, new WebBitmap(key, bmp)).sendToTarget();
    }

    public boolean resourceInCached(String url) {
        return mCachedFileList.contains(generateFileName(url));
    }

    private Bitmap getResource(String url) {
        if (!resourceInCached(url))
            return null;

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(generateFilePath(url));
            final Bitmap newBmp = BitmapFactory.decodeStream(stream);
            if (newBmp == null || newBmp.getWidth() <= 0 ||
                newBmp.getHeight() <= 0) {
                Log.e(tag, "Bitmap is null decode from cache, key: " + url);
                return null;
            }

            return newBmp;
        } catch (FileNotFoundException e) {
            Log.e(tag, "Cache file not found for " + url);
        }
        return null;
    }

    private void saveResourceToFile(String url, Bitmap bmp) {
        if (mCacheDirName == null ||
                resourceInCached(url) ||
                bmp == null)
            return;

        try {
            File outputFile = new File(generateFilePath(url));
            FileOutputStream fos = new FileOutputStream(outputFile);
            bmp.compress(Bitmap.CompressFormat.PNG,
                                90, fos);
            fos.close();
            mCachedFileList.add(generateFileName(url));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateFileName(String url) {
        if (url == null)
            return null;
        return String.valueOf(url.hashCode());
    }

    private String generateFilePath(String url) {
        return mCacheDirName + "/" + generateFileName(url);
    }

    private void onBitmapRecieved(String key, Bitmap bmp) {
        mWebResourceCallback.onResourceReceived(key, bmp);
    }

    private void onLoadingError(String key) {
        mWebResourceCallback.onError(key, null);
    }

    private Handler getUiTaskHandler() {
        return mUiTaskHandler;
    }

}