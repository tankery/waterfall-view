package tankery.app.family.photos.data;

import java.util.ArrayList;
import java.util.Arrays;

import tankery.app.family.photos.net.WebResourceLoader.NetworkError;
import tankery.app.family.photos.net.WebResourceLoader.WebResourceCallback;
import tankery.app.modules.waterfall.data.CachedBitmap;
import tankery.app.modules.waterfall.data.CachedBitmap.BitmapHolder;

import android.content.Context;
import android.util.Log;

public class PhotoList implements WebResourceCallback,
                                  BitmapHolder {
    static final String tag = "PhotoList";

    private static final String HOST_ADDRESS = "http://192.168.0.211";
    private static final String MYHOME_SERVER_URL = HOST_ADDRESS + "/myhome/";
    private static final String DEFAULT_PHOTO_LIST_FETCHING_URL =
            MYHOME_SERVER_URL + "?action=ls&act_para=/test_waterfall&";

    private static final String CACHE_FILE_SUBDIR = "";

    private String mPhotoListFetchingUrl = DEFAULT_PHOTO_LIST_FETCHING_URL;

    public enum PhotoListError {
        UNKNOW_ERROR,
        HTTP_CONNETION_TIMEOUT
    }

    /**
     * listener interface who will be called when photo fetching finished.
     */
    public interface PhotoListListener {
        void onListFetchingFinished();
        void onPhotoReceived(CachedBitmap cbmp);
        void onErrorOccurred(PhotoListError err);
    }

    private PhotoLoader mPhotoLoader;
    private PhotoListLoader mListLoader;
    private PhotoListListener mPhotoListListener;

    private ArrayList<String> mPhotoListOnWeb;
    private int mCurrentIndex = 0;


    public PhotoList(Context context) {
        mListLoader = new PhotoListLoader(this);
        mPhotoLoader = new PhotoLoader();
        mPhotoLoader.setCacheDir(context.getCacheDir() + CACHE_FILE_SUBDIR);
    }

    public void setFetchingFinishedListener(
            PhotoListListener onFetchingFinishedListener) {
        this.mPhotoListListener = onFetchingFinishedListener;
    }

    public void setPhotoCompressedWidth(int width) {
        mPhotoLoader.setPhotoCompressedWidth(width);
    }

    public void refreshPhotoList() {
        if (mPhotoListOnWeb != null)
            mPhotoListOnWeb.clear();
        mCurrentIndex = 0;

        mPhotoLoader.stopLoading();
        mListLoader.stopLoading();
        mListLoader.startLoading(mPhotoListFetchingUrl);
    }

    // Note this function can only calling in UI thread.
    public void fetchMorePhotos(int number) {
        if (mPhotoListOnWeb == null || mPhotoLoader.hasPendingLoad())
            return;

        int expectedIndex = Math.min(mPhotoListOnWeb.size(),
                                     mCurrentIndex + number);
        for (;mCurrentIndex < expectedIndex; mCurrentIndex++) {
            String photoUrl = mPhotoListOnWeb.get(mCurrentIndex);
            CachedPhoto cbmp = new CachedPhoto(mPhotoLoader, photoUrl);
            cbmp.setHolder(this);
            cbmp.setInUse(true);
        }
    }


    /**************************************************************
     * implements WebResourceCallback, used to fetch photo list.
     **************************************************************/

    @Override
    public void onResourceReceived(String url, Object obj) {
        String result = (String) obj;
        if (result == null || result.isEmpty()) {
            Log.e(tag, "PhotoList: web photo list is empty");
            return;
        }

        String[] list = result.split(";");
        // generate the full url.
        for (int i = 0; i < list.length; ++i) {
            list[i] = HOST_ADDRESS + list[i];
        }
        mPhotoListOnWeb = new ArrayList<String>(Arrays.asList(list));
        mPhotoListListener.onListFetchingFinished();
    }

    @Override
    public void onError(String url, NetworkError error) {
        if (error == NetworkError.CONNECTION_TIMEOUT) {
            mListLoader.stopLoading();
            mPhotoListListener.onErrorOccurred(PhotoListError.HTTP_CONNETION_TIMEOUT);
        }
        else {
            mPhotoListListener.onErrorOccurred(PhotoListError.UNKNOW_ERROR);
        }
    }


    /**************************************************************
     * implements BitmapHolder
     **************************************************************/

    @Override
    public void resetBitamp(CachedBitmap cbmp) {
        if (cbmp.getBitmap().isRecycled())
            Log.v(tag, "photo " + cbmp + ", recycled.");
        mPhotoListListener.onPhotoReceived(cbmp);
    }

}
