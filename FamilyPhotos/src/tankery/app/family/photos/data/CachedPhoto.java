package tankery.app.family.photos.data;

import tankery.app.family.photos.data.BitmapLoader.BitmapReceivedCallback;
import android.graphics.Bitmap;


public class CachedPhoto extends CachedBitmap
                             implements BitmapReceivedCallback {

    private BitmapLoader mLoader;

    public CachedPhoto(BitmapLoader loader, String key) {
        super(key);
        mLoader = loader;
    }

    /**
     * TODO: this method need to be removed after refactor.
     * @param initBmp
     * @param loader
     * @param key
     */
    public CachedPhoto(Bitmap initBmp, BitmapLoader loader, String key) {
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
        clearBitmapIfNotUse();
        mLoader.recycle(getKey());
    }


    /**************************************************
     * BitmapReceivedCallback Implements
     **************************************************/

    @Override
    public void onBitmapRecieved(Bitmap bmp) {
        updateBitmap(bmp);
    }
}
