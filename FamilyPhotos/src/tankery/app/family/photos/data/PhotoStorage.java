package tankery.app.family.photos.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import tankery.app.family.photos.data.NetworkPhotoLoader.ResourceLoadingError;
import tankery.app.family.photos.data.NetworkPhotoLoader.WebBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

public class PhotoStorage {

    static final String tag = "PhotoStorage";

    /**
     * listener interface who will be called when photo fetching finished.
     */
    public interface PhotoStorageListener {
        void onListFetchingFinished();
        void onPhotoReceived(String key);
        void onPhotoFetchingFinished(ArrayList<Integer> updatedIdList);
        void onStorageErrorOccurred(PhotoStorageError err);
    }

    public enum PhotoStorageError {
        UNKNOW_ERROR,
        HTTP_CONNETION_TIMEOUT
    }

    private ArrayList<String> photoListOnWeb;
    private SparseArray<WebBitmap> photoTable = new SparseArray<WebBitmap>();
    private NetworkPhotoLoader photoLoader = new NetworkPhotoLoader();

    Lock photoTableLock = new ReentrantLock();

    private ArrayList<Integer> updatedPhotoIdList = new ArrayList<Integer>();
    private PhotoStorageListener photoStorageListener;

    private int currentIndex = 0;
    private boolean inPhotoFetchingNow = false;

    private static PhotoStorage instance = null;

    private PhotoStorage() {
        photoLoader.setPhotoLoaderListener(new PhotoLoaderListener() {

            @Override
            public void onReceivedPhotoList(String[] list) {
                if (list == null || list.length == 0) {
                    Log.e(tag, "PhotoStorage: web photo list is empty");
                    return;
                }
                photoListOnWeb = new ArrayList<String>(Arrays.asList(list));
                photoStorageListener.onListFetchingFinished();
            }

            @Override
            public void onReceivedPhoto(WebBitmap bmp) {
                int id = generateId(bmp.url);

                WebBitmap wbmp = photoTable.get(id);
                if (wbmp != null && wbmp.bitmap != null &&
                        !wbmp.bitmap.isRecycled()) {
                    // already have this picture, delete the new bitmap.
                    if (bmp.bitmap != null &&
                        !bmp.bitmap.isRecycled() &&
                        bmp.bitmap != wbmp.bitmap) {
                        bmp.bitmap = null;
                    }
                    bmp = wbmp;
                }
                else {
                    photoTableLock.lock();
                    // put this new bitmap to table
                    photoTable.put(id, bmp);
                    photoTableLock.unlock();
                }
                updatedPhotoIdList.add(id);
                if (bmp.bitmap.isRecycled())
                    Log.v(tag, "photo " + bmp.url + ", recycled.");
                photoStorageListener.onPhotoReceived(bmp.url);
            }

            @Override
            public void onFinishedPhotoFetching() {
                currentIndex += updatedPhotoIdList.size();
                photoStorageListener.onPhotoFetchingFinished(updatedPhotoIdList);
                inPhotoFetchingNow = false;
            }

            @Override
            public void onErrorOccurred(ResourceLoadingError err, String url) {
                switch (err) {
                case HTTP_CONNETION_TIMEOUT:
                    photoStorageListener.onStorageErrorOccurred(PhotoStorageError.HTTP_CONNETION_TIMEOUT);
                    break;
                default:
                    Log.e(tag, err.name() +
                               (url.isEmpty() ? "" : " on fetching " + url));
                    break;
                }
            }

        });
    }

    public static PhotoStorage getInstance() {
        if (instance == null) {
            instance = new PhotoStorage();
        }

        return instance;
    }

    public void setOnFetchingFinishedListener(
            PhotoStorageListener onFetchingFinishedListener) {
        this.photoStorageListener = onFetchingFinishedListener;
    }

    public void setPhotoCompressedWidth(int width) {
        photoLoader.setPhotoCompressedWidth(width);
    }

    public void setUseTempPhotoFile(Context appContext) {
        photoLoader.useTempPhotoFile(appContext);
    }

    public void refreshPhotoList() {
        if (photoListOnWeb != null)
            photoListOnWeb.clear();
        currentIndex = 0;
        photoLoader.stopLoading();
        photoLoader.fetchWebPhotoList();
        inPhotoFetchingNow = false;
    }

    // Note this function can only calling in UI thread.
    public void fetchMorePhotos(int number) {
        if (photoListOnWeb == null || inPhotoFetchingNow)
            return;

        updatedPhotoIdList.clear();
        inPhotoFetchingNow = true;

        int expectedIndex = Math.min(photoListOnWeb.size(),
                                     currentIndex + number);
        for (int i = currentIndex; i < expectedIndex; i++) {
            photoLoader.addLoadRequest(photoListOnWeb.get(i));
        }
    }

    public Bitmap getPhoto(int id) {
        WebBitmap wbmp = photoTable.get(id);
        if (wbmp == null)
            return null;
        return wbmp.bitmap;
    }

    public void setPhoto(int id, Bitmap bmp) {
        WebBitmap wbmp = photoTable.get(id);
        if (wbmp == null || wbmp.bitmap == bmp)
            return;
        photoTableLock.lock();
        wbmp.bitmap = bmp;
        photoTableLock.unlock();
    }

    public static int generateId(String url) {
        return url.hashCode();
    }

}
