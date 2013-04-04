package tankery.app.family.photos.data;

import android.graphics.Bitmap;

public abstract class CachedBitmap {

    public interface BitmapHolder {
        void resetBitamp(CachedBitmap cbmp);
    }

    private String mKey;
    private Bitmap mBitmap;
    private boolean mInUse;

    private BitmapHolder mHolder;

    public CachedBitmap(String key) {
        mKey = key;
        mInUse = false;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getKey() {
        return mKey;
    }

    @Override
    public String toString() {
        return getKey();
    }

    public boolean isInUse() {
        return mInUse;
    }

    public void setInUse(boolean inUse) {
        if (inUse == mInUse)
            return;
        mInUse = inUse;
    }

    public void setHolder(BitmapHolder holder) {
        mHolder = holder;
    }

    protected void updateBitmap(Bitmap bmp) {
        mBitmap = bmp;
        if (mHolder != null && mInUse) {
            mHolder.resetBitamp(this);
        }
    }

}
