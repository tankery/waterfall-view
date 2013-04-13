package tankery.app.waterfall.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;
import tankery.app.modules.waterfall.data.CachedBitmap;
import tankery.app.modules.waterfall.data.CachedBitmap.BitmapHolder;
import tankery.app.modules.waterfall.widget.WaterfallView;

public class SampleView extends WaterfallView
                        implements BitmapHolder {

    private static final String LOGTAG = "SampleView";

    private static final String PHOTO_PATH = "images";
    private static final int PHOTO_FETCHING_COUNT = 30;

    private AssetManager mAssetManager;
    private List<String> mPhotoList;

    public SampleView(Context context) {
        super(context);
    }

    public SampleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SampleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(AssetManager asset) {
        mAssetManager = asset;
        super.init();
    }

    @Override
    public void init(int column) {
        super.init(column);
        initPhotoList();
        appendNewItems();
    }

    private void initPhotoList() {
        if (mAssetManager == null) {
            Log.e(LOGTAG, "Asset Manager is null!!!");
            return;
        }
        if (mPhotoList == null)
            mPhotoList = new ArrayList<String>();

        try {
            for (String filename : mAssetManager.list(PHOTO_PATH)) {
                mPhotoList.add(PHOTO_PATH + "/" + filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Note this function can only calling in UI thread.
    private void appendNewItems() {
        Random rand = new Random();

        for (int i = 0; i < PHOTO_FETCHING_COUNT; i++) {
            int r = rand.nextInt(mPhotoList.size());
            startLoadNewPhoto(mPhotoList.get(r));
        }
    }

    private void startLoadNewPhoto(String key) {
        CachedPhoto photo = new CachedPhoto(getContext(), key);
        photo.setHolder(this);
        photo.setInUse(true);
    }

    public void showUserMessage(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }


    /**************************************************
     * WaterfallView Overrides
     **************************************************/

    @Override
    public void onTopReached() {
        Log.d(LOGTAG, "Scrolled to top");
        showUserMessage(R.string.waterfall_refresh);
        super.onTopReached();
        appendNewItems();
    }

    @Override
    public void onBottomReached() {
        Log.d(LOGTAG, "Scrolled to bottom");
        showUserMessage(R.string.waterfall_adding_item);
        appendNewItems();
        super.onBottomReached();
    }


    /**************************************************************
     * implements BitmapHolder
     **************************************************************/

    @Override
    public void resetBitamp(CachedBitmap cbmp) {
        if (cbmp.getBitmap().isRecycled())
            Log.w(LOGTAG, "photo " + cbmp + ", recycled.");

        super.appendBitmap(cbmp);
    }

}
