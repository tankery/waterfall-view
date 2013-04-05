package tankery.app.family.photos.data;

import java.io.InputStream;
import java.util.HashMap;

import tankery.app.family.photos.net.WebResourceLoader;
import tankery.app.family.photos.net.WebResourceLoader.NetworkError;
import tankery.app.family.photos.net.WebResourceLoader.StreamDecoder;
import tankery.app.family.photos.net.WebResourceLoader.WebResourceCallback;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * The loader to load photos.
 *
 * @author tankery
 *
 */
public class PhotoLoader implements WebResourceCallback {

    static final String tag = "PhotoLoader";

    public interface BitmapReceivedCallback {
        void onBitmapRecieved(Bitmap bmp);
    }

    private HashMap<String, BitmapReceivedCallback> mListenerMap;

    private WebResourceLoader mWebResourceLoader;

    private PhotoDiskCache mDiskCache;

    private int mPhotoCompressedWidth;
    private StreamDecoder mBitmapStreamDecoder = new StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            return decodeStream(is, mPhotoCompressedWidth != 0);
        }

        private Bitmap decodeStream(InputStream is, boolean compress) {
            Bitmap bmp = null;
            try {
                bmp = BitmapFactory.decodeStream(is);
            } catch (OutOfMemoryError e) {
                Log.e(tag, "Out of Memory Error!");
                bmp = null;
            }
            if (bmp != null && compress) {
                int width = mPhotoCompressedWidth;
                int height = (bmp.getHeight() * width) / bmp.getWidth();
                Bitmap newBitmap = Bitmap.createScaledBitmap(bmp, width,
                                                             height,
                                                             false);
                bmp = newBitmap;
            }
            return bmp;
        }
    };


    public PhotoLoader() {
        mListenerMap = new HashMap<String, BitmapReceivedCallback>();
        mWebResourceLoader = WebResourceLoader.getInstance();
        mDiskCache = new PhotoDiskCache(this);
    }

    public void setCacheDir(String dir) {
        mDiskCache.setCacheDir(dir);
    }

    public void setPhotoCompressedWidth(int width) {
        mPhotoCompressedWidth = width;
    }

    public void startLoading(String urlStr, BitmapReceivedCallback listener) {
        mListenerMap.put(urlStr, listener);
        if (mDiskCache.resourceInCached(urlStr))
            mDiskCache.addReloadRequest(urlStr);
        else
            mWebResourceLoader.addResourceLoadRequest(urlStr,
                                                      this,
                                                      mBitmapStreamDecoder);
    }

    public void stopLoading() {
        mListenerMap.clear();
        mWebResourceLoader.stopLoading();
    }

    public boolean hasPendingLoad() {
        return !mListenerMap.isEmpty();
    }

    @Override
    public void onResourceReceived(String key, Object resource) {
        Bitmap bmp = (Bitmap) resource;
        mDiskCache.addSaveRequest(key, bmp);
        if (mListenerMap.get(key) != null) {
            mListenerMap.get(key).onBitmapRecieved(bmp);
            mListenerMap.remove(key);
        }
    }

    @Override
    public void onError(String key, NetworkError error) {
        mListenerMap.remove(key);
    }

}
