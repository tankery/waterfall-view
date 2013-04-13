package tankery.app.family.photos.data;

import tankery.app.family.photos.data.PhotoLoader.BitmapReceivedCallback;
import tankery.app.modules.waterfall.data.CachedBitmap;
import android.graphics.Bitmap;


public class CachedPhoto extends CachedBitmap
                             implements BitmapReceivedCallback {

    private PhotoLoader mLoader;

    public CachedPhoto(PhotoLoader loader, String key) {
        super(key);
        mLoader = loader;
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
    }


    /**************************************************
     * BitmapReceivedCallback Implements
     **************************************************/

    @Override
    public void onBitmapRecieved(Bitmap bmp) {
        updateBitmap(bmp);
    }
}
