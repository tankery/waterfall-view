package tankery.app.family.photos.data;

import tankery.app.family.photos.data.BitmapLoader.BitmapLoaderListener;
import android.graphics.Bitmap;


public class CachedWebBitmap extends CachedBitmap
                             implements BitmapLoaderListener {

    private BitmapLoader mLoader;

    public CachedWebBitmap(BitmapLoader loader, String key) {
        super(key);
        mLoader = loader;
    }

    /**
     * TODO: this method need to be removed after refactor.
     * @param initBmp
     * @param loader
     * @param key
     */
    public CachedWebBitmap(Bitmap initBmp, BitmapLoader loader, String key) {
        this(loader, key);

        super.setInUse(true);
        onBitmapRecieved(initBmp);
    }

    @Override
    public void setInUse(boolean inUse) {
        super.setInUse(inUse);

        if (isInUse())
            load();
        else
            recycle();
    }

    public void load() {
        Bitmap bmp = getBitmap();
        if (bmp != null && !bmp.isRecycled()) {
            onBitmapRecieved(bmp);
            return;
        }

        mLoader.startLoading(getKey(), this);
    }

    public void recycle() {
        mLoader.recycle(getKey());
    }


    /**************************************************
     * BitmapLoaderListener Implements
     **************************************************/

    @Override
    public void onBitmapRecieved(Bitmap bmp) {
        updateBitmap(bmp);
    }
}
