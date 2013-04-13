package tankery.app.waterfall.sample;

import android.content.Context;
import android.graphics.Bitmap;
import tankery.app.modules.waterfall.data.CachedBitmap;
import tankery.app.waterfall.sample.PhotoLoadingThread.BitmapReceivedCallback;

public class CachedPhoto extends CachedBitmap implements BitmapReceivedCallback {

    private static PhotoLoadingThread mPhotoLoadingThread;

    public CachedPhoto(Context context, String key) {
        super(key);
        if (mPhotoLoadingThread == null) {
            mPhotoLoadingThread = new PhotoLoadingThread(context);
            mPhotoLoadingThread.start();
        }
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

        if (bmp == null || bmp.isRecycled()) {
            mPhotoLoadingThread.startLoading(getKey(), this);
            return;
        }

        updateBitmap(bmp);
    }

    public void recycle() {
        clearBitmapIfNotUse();
    }

    @Override
    public void onBitmapRecieved(Bitmap bmp) {
        updateBitmap(bmp);
    }

}
