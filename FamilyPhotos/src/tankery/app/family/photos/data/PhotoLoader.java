package tankery.app.family.photos.data;


public interface PhotoLoader {

    public abstract void setPhotoLoaderListener(PhotoLoaderListener listener);

    public abstract void addLoadRequest(String urlStr);

    public abstract void fetchWebPhotoList();

    public abstract void stopLoading();

}
