package tankery.app.family.photos;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import tankery.app.family.photos.R;
import tankery.app.family.photos.data.CachedBitmap;
import tankery.app.family.photos.data.CachedPhoto;
import tankery.app.family.photos.data.PhotoStorage;
import tankery.app.family.photos.data.PhotoStorage.PhotoStorageError;
import tankery.app.family.photos.data.PhotoStorage.PhotoStorageListener;
import tankery.app.family.photos.data.BitmapLoader;
import tankery.app.family.photos.widget.WaterfallView;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

public class FamilyPhotosView extends WaterfallView
                              implements PhotoStorageListener {

    private static final String LOGTAG = "FamilyPhotosView";

    private enum WaterfallErrorType {
        UNKNOW_ERROR,
        PHOTO_FETCHING_TIMEOUT
    }

    private static final int PHOTO_FETCHING_COUNT = 30;

    private BitmapLoader mBitmapLoader = null;

    public FamilyPhotosView(Context context) {
        super(context);
    }

    public FamilyPhotosView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FamilyPhotosView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void init() {
        super.init();
        initStorage();
        mBitmapLoader = new BitmapLoader(getContext());
    }

    @Override
    public void init(int column) {
        super.init(column);
        initStorage();
    }

    private void initStorage() {
        PhotoStorage storage = PhotoStorage.getInstance();
        storage.setPhotoCompressedWidth(getColumnWidth());
        storage.setUseTempPhotoFile(getContext());
        storage.setOnFetchingFinishedListener(this);
        storage.refreshPhotoList();
        // append new item at initial.
        needAppendNewItems();
    }

    private final WaterfallErrorHandler waterfallErrorHandler =
            new WaterfallErrorHandler(this);

    private final static class WaterfallErrorHandler extends Handler {

        private WeakReference<WaterfallView> mView;

        WaterfallErrorHandler(WaterfallView view) {
            mView = new WeakReference<WaterfallView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            final WaterfallErrorType types[] = WaterfallErrorType.values();
            if (msg.what >= types.length) {
                Log.e(LOGTAG, "msg.what =" + msg.what +
                           " is out of WaterfallErrorType.");
                return;
            }
            switch (types[msg.what]) {
            case UNKNOW_ERROR:
                mView.get().showUserMessage((String) msg.obj);
                break;
            case PHOTO_FETCHING_TIMEOUT:
                mView.get().showUserMessage(R.string.app_err_connection_timeout);
                break;
            default:
                Log.e(LOGTAG, types[msg.what].name() + " is invalidate.");
                break;
            }
        }
    }

    // Note this function can only calling in UI thread.
    private void needAppendNewItems() {
        PhotoStorage storage = PhotoStorage.getInstance();
        storage.fetchMorePhotos(PHOTO_FETCHING_COUNT);
    }


    /**************************************************
     * WaterfallView Overrides
     **************************************************/

    @Override
    public void onVScrollChanged(int t, int oldt) {
        super.onVScrollChanged(t, oldt);
    }

    @Override
    public void onTopReached() {
        Log.d(LOGTAG, "Scrolled to top");
        showUserMessage(R.string.waterfall_refresh);
        PhotoStorage.getInstance().refreshPhotoList();
        super.onTopReached();
    }

    @Override
    public void onBottomReached() {
        Log.d(LOGTAG, "Scrolled to bottom");
        showUserMessage(R.string.waterfall_adding_item);
        needAppendNewItems();
        super.onBottomReached();
    }

    @Override
    public void onScrolling() {
        super.onScrolling();
    }


    /**************************************************
     * PhotoStorageListener Implements
     **************************************************/

    @Override
    public void onListFetchingFinished() {
        needAppendNewItems();
    }

    @Override
    public void onPhotoReceived(String key) {
        ArrayList<CachedBitmap> list = new ArrayList<CachedBitmap>();
        // FIXME: should refactor this part, we should delay load the bitmap.
        Bitmap bmp = PhotoStorage.getInstance()
                .getPhoto(PhotoStorage.generateId(key));
        CachedPhoto cbmp = new CachedPhoto(bmp, mBitmapLoader, key);
        list.add(cbmp);
        super.appendNewBitmaps(list);
    }

    @Override
    public void onPhotoFetchingFinished(
            ArrayList<Integer> updatedIdList) {
        // don't append new item when finished
        // cause i have move the new item adding to the time
        // each photo received.
        // doAppendNewItems(updatedIdList);
    }

    @Override
    public void onStorageErrorOccurred(PhotoStorageError err) {
        switch (err) {
        case HTTP_CONNETION_TIMEOUT:
            int what = WaterfallErrorType.PHOTO_FETCHING_TIMEOUT.ordinal();
            waterfallErrorHandler.obtainMessage(what)
                                 .sendToTarget();
            break;
        default:
            what = WaterfallErrorType.UNKNOW_ERROR.ordinal();
            waterfallErrorHandler.obtainMessage(what, err.name())
                                 .sendToTarget();
            break;
        }
    }

}
