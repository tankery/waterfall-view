package tankery.app.modules.waterfall.widget;

import tankery.app.modules.waterfall.data.CachedBitmap;
import tankery.app.modules.waterfall.data.CachedBitmap.BitmapHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class WaterfallItem extends ImageView implements BitmapHolder {

    static final String tag = "WaterfallItem";

    private CachedBitmap mCachedBitmap;

    public WaterfallItem(Context context) {
        super(context);
    }

    @Override
    public String toString() {
        return "Resource " + mCachedBitmap.toString();
    }

    public boolean setCachedBitmap(CachedBitmap cbmp) {
        mCachedBitmap = cbmp;
        mCachedBitmap.setHolder(this);
        resetBitamp(mCachedBitmap);

        return true;
    }

    ///////////////////////////////////
    // memory management.

    public void reload() {
        Log.d(tag, "reload " + toString());
        assert mCachedBitmap != null;

        mCachedBitmap.setInUse(true);
    }

    public void recycle() {
        Log.d(tag, "recycle Bitmap " + mCachedBitmap.getBitmap() + ", key " + toString());
        assert mCachedBitmap != null;

        // set this null bitmap to item
        setImageBitmap(null);
        mCachedBitmap.setInUse(false);
    }


    /**************************************************
     * BitmapHolder Implements
     **************************************************/

    @Override
    public void resetBitamp(CachedBitmap cbmp) {
        if (cbmp == null)
            return;

        if (cbmp.getKey().compareTo(mCachedBitmap.getKey()) != 0) {
            Log.e(tag, "Can't reset a bitmap mismatch key, returned");
            return;
        }

        Bitmap bmp = cbmp.getBitmap();
        if (bmp == null || bmp.isRecycled()) {
            Log.e(tag, "Bitmap [" + cbmp + "] is " +
                    (bmp == null ? "null" : "recycled") +
                    " when resetBitamp.");
            return;
        }

        Log.d(tag, "reset Bitmap " + bmp + ", key " + toString());
        setImageBitmap(bmp);
    }

}
