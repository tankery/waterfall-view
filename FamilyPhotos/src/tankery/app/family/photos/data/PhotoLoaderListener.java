package tankery.app.family.photos.data;

import tankery.app.family.photos.data.NetworkPhotoLoader.ResourceLoadingError;
import tankery.app.family.photos.data.NetworkPhotoLoader.WebBitmap;

/**
 * listener interface who will be called when load state changed.
 */
public interface PhotoLoaderListener {
    void onReceivedPhotoList(String[] list);
    void onReceivedPhoto(WebBitmap bmp);
    void onFinishedPhotoFetching();
    void onErrorOccurred(ResourceLoadingError err, String url);
}