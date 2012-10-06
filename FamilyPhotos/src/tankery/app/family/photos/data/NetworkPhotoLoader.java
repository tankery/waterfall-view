package tankery.app.family.photos.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import tankery.app.family.photos.net.ResourceLoaderTask;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NetworkPhotoLoader implements PhotoLoader {

    private static final String tag = "PhotoLoader";

    public class WebBitmap {
        public WebBitmap(String url, Bitmap bmp) {
            this.url = url;
            this.bitmap = bmp;
        }

        public Bitmap bitmap = null;
        public String url = null;
    }

    private static final String HOST_ADDRESS = "http://192.168.0.211";
    private static final String MYHOME_SERVER_URL = HOST_ADDRESS + "/myhome/";

    public enum ResourceLoadingError {
        UNKNOW_ERROR,
        HTTP_CONNETION_TIMEOUT,
        BITMAP_DECODE_NULL
    }

    public enum ResourceLoadingType {
        NOT_START,
        WEB_PHOTO_LIST,
        PHOTO_CONTENT
    }

    private PhotoLoaderListener photoLoaderListener;
    
    private String fetchWebPhotoUrl = MYHOME_SERVER_URL +
                                      "?action=ls&act_para=/test_waterfall&";
    private Queue<String> urlRequests = new LinkedList<String>();
    
    private ResourceLoadingType loadingType = ResourceLoadingType.NOT_START;

    public NetworkPhotoLoader() {
    }

    /* (non-Javadoc)
     * @see tankery.app.family.photos.data.PhotoLoader#setOnStateChangeListener(tankery.app.family.photos.data.NetworkPhotoLoader.OnStateChangeListener)
     */
    @Override
    public void setPhotoLoaderListener(PhotoLoaderListener listener) {
        this.photoLoaderListener = listener;
    }

    /* (non-Javadoc)
     * @see tankery.app.family.photos.data.PhotoLoader#addLoadRequest(java.lang.String)
     */
    @Override
    public void addLoadRequest(String urlStr) {
        // add the url string to load queue,
        urlRequests.add(urlStr);

        // and start loading.
        if (loadingType == ResourceLoadingType.NOT_START) {
            loadingType = ResourceLoadingType.PHOTO_CONTENT;
            updateLoading();
        }
    }

    /* (non-Javadoc)
     * @see tankery.app.family.photos.data.PhotoLoader#fetchWebPhotoList()
     */
    @Override
    public void fetchWebPhotoList() {
        if (loadingType == ResourceLoadingType.NOT_START) {
            loadingType = ResourceLoadingType.WEB_PHOTO_LIST;
            updateLoading();
        }
    }

    /* (non-Javadoc)
     * @see tankery.app.family.photos.data.PhotoLoader#stopLoading()
     */
    @Override
    public void stopLoading() {
        loadingType = ResourceLoadingType.NOT_START;
        updateLoading();
    }

    private ResourceLoaderTask resourceLoaderTask;

    private ResourceLoaderTask.ResourceLoaderTaskListener taskListener =
            new ResourceLoaderTask.ResourceLoaderTaskListener() {

                @Override
                public void onResourceReceived(Object obj) {
                    if (obj == null)
                        return;
                    if (obj instanceof WebBitmap) {
                        WebBitmap bmp = (WebBitmap) obj;
                        photoLoaderListener.onReceivedPhoto(bmp);
                    }
                    else if (obj instanceof String) {
                        String result = (String) obj;
                        String[] list = result.isEmpty() ? null : result.split(";");
                        photoLoaderListener.onReceivedPhotoList(list);
                    }
                }

                @Override
                public void onFinished(int count) {
                    if (loadingType == ResourceLoadingType.PHOTO_CONTENT)
                        photoLoaderListener.onFinishedPhotoFetching();
                    loadingType = ResourceLoadingType.NOT_START;
                }

                @Override
                public void onConnectionTimeout() {
                    stopLoading();
                    photoLoaderListener.onErrorOccurred(ResourceLoadingError.HTTP_CONNETION_TIMEOUT,
                                                          "");
                }
            };

    private ResourceLoaderTask.StreamDecoder bitmapStreamDecoder = new ResourceLoaderTask.StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) {
                photoLoaderListener.onErrorOccurred(ResourceLoadingError.BITMAP_DECODE_NULL,
                                                      url);
                return null;
            }
            return new WebBitmap(url, bmp);
        }
    };

    private ResourceLoaderTask.StreamDecoder photoListStreamDecoder = new ResourceLoaderTask.StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            String result = "";
            try {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(is));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    result = result.concat(inputLine);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    };

    private Handler updateLoadingHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final ResourceLoadingType types[] = ResourceLoadingType.values();
            if (msg.what >= types.length) {
                Log.e(tag, "msg.what =" + msg.what +
                           " is out of ResourceLoadingType.");
                return;
            }
            switch (types[msg.what]) {
            case NOT_START:
                if (resourceLoaderTask != null)
                    resourceLoaderTask.cancel(false);
                break;
            case WEB_PHOTO_LIST: {
                resourceLoaderTask = new ResourceLoaderTask();
                resourceLoaderTask.setResourceLoaderTaskListener(taskListener);
                resourceLoaderTask.setStreamDecoder(photoListStreamDecoder);
                resourceLoaderTask.execute(fetchWebPhotoUrl);
                Log.d(tag, "start loading " + fetchWebPhotoUrl);
                break;
            }
            case PHOTO_CONTENT: {
                int count = urlRequests.size();
                if (count == 0)
                    break;
                String[] requests = new String[count];
                for (int i = 0; i < count; i++) {
                    requests[i] = HOST_ADDRESS + urlRequests.poll();
                }
                resourceLoaderTask = new ResourceLoaderTask();
                resourceLoaderTask.setResourceLoaderTaskListener(taskListener);
                resourceLoaderTask.setStreamDecoder(bitmapStreamDecoder);
                resourceLoaderTask.execute(requests);
                Log.d(tag, "start loading " + urlRequests.toArray());
                break;
            }
            default:
                Log.e(tag, types[msg.what].name() + " is invalidate.");
                break;
            }
        }
    };

    private void updateLoading() {
        updateLoadingHandle.obtainMessage(loadingType.ordinal()).sendToTarget();
    }

}
