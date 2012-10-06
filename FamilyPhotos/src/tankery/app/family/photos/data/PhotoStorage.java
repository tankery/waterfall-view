package tankery.app.family.photos.data;

import java.util.ArrayList;
import java.util.Arrays;

import tankery.app.family.photos.data.NetworkPhotoLoader.ResourceLoadingError;
import tankery.app.family.photos.data.NetworkPhotoLoader.WebBitmap;

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
        void onPhotoReceived(int id);
        void onPhotoFetchingFinished(ArrayList<Integer> updatedIdList);
        void onStorageErrorOccurred(PhotoStorageError err);
    }

    public enum PhotoStorageError {
        UNKNOW_ERROR,
        HTTP_CONNETION_TIMEOUT
    }

    private ArrayList<String> photoListOnWeb;
    private SparseArray<WebBitmap> photoTable = new SparseArray<WebBitmap>();
    private PhotoLoader photoLoader = new NetworkPhotoLoader();

    private ArrayList<Integer> updatedPhotoIdList = new ArrayList<Integer>();
    private PhotoStorageListener photoStorageListener;

    private int currentIndex = 0;

    private boolean inPhotoFetchingNow = false;

    private int photoCompressedWidth = 0;

    private static PhotoStorage instance = null;

    private PhotoStorage() {
        photoLoader.setPhotoLoaderListener(new PhotoLoaderListener() {

            @Override
            public void onReceivedPhotoList(String[] list) {
                if (list == null || list.length == 0) {
                    Log.e(tag, "PhotoStorage: web photo list is empty");
                }
                photoListOnWeb = new ArrayList<String>(Arrays.asList(list));
                photoStorageListener.onListFetchingFinished();
            }

            @Override
            public void onReceivedPhoto(WebBitmap bmp) {
                int id = generateId(bmp.url);

                if (photoTable.get(id) != null) {
                    // already have this picture, delete the new bitmap and use
                    // the old one.
                    bmp.bitmap.recycle();
                    bmp = photoTable.get(id);
                }
                else {
                    // add this new bitmap to table and compress it.
                    photoTable.put(id, bmp);
                    compressBitmap(bmp);
                }
                updatedPhotoIdList.add(id);
                photoStorageListener.onPhotoReceived(id);
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
        Log.d(tag, "getPhoto " + wbmp.url);
        return wbmp.bitmap;
    }

    private int generateId(String url) {
        return url.hashCode();
    }

    private void compressBitmap(WebBitmap bmp) {
        if (photoCompressedWidth == 0)
            // no need to compress.
            return;
        int width = photoCompressedWidth;
        int height = (bmp.bitmap.getHeight() * width) / bmp.bitmap.getWidth();
        Bitmap newBitmap = Bitmap.createScaledBitmap(bmp.bitmap, width, height,
                                                     false);
        bmp.bitmap.recycle();
        bmp.bitmap = newBitmap;
    }

    public void setPhotoCompressedWidth(int width) {
        photoCompressedWidth = width;
    }

}
