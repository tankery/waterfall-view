package tankery.app.family.photos.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import tankery.app.family.photos.net.ResourceLoaderTask;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class PhotoLoader {

    static final String tag = "PhotoLoader";

    public class WebBitmap {
        public WebBitmap(String url, Bitmap bmp) {
            this.url = url;
            this.bitmap = bmp;
        }

        public Bitmap bitmap = null;
        public String url = null;
    }

    static final String HOST_ADDRESS = "http://192.168.0.211";
    static final String MYHOME_SERVER_URL = HOST_ADDRESS + "/myhome/";

    static final String BITMAP_LOAD_FAILED = "Bitmap Load Failed";

    static final String HTTP_REQUEST_FAILED = null;
    
    enum ResourceLoadingType {
        NOT_START,
        WEB_PHOTO_LIST,
        PHOTO_CONTENT
    }

    private OnStateChangeListener onStateChangeListener;
    
    private String fetchWebPhotoUrl = MYHOME_SERVER_URL +
                                      "?action=ls&act_para=/test_waterfall&";
    private Queue<String> urlRequests = new LinkedList<String>();
    
    private ResourceLoadingType loadingType = ResourceLoadingType.NOT_START;

    /**
     * listener interface who will be called when load state changed.
     */
    public interface OnStateChangeListener {
        void onReceivedPhotoList(String[] list);
        void onReceivedPhoto(WebBitmap bmp);
        void onFinishedPhotoFetching();
        void onErrorOccurred(String err);
    }

    public PhotoLoader() {
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    public void addLoadRequest(String urlStr) {
        // add the url string to load queue,
        urlRequests.add(urlStr);

        // and start loading.
        if (loadingType == ResourceLoadingType.NOT_START) {
            loadingType = ResourceLoadingType.PHOTO_CONTENT;
            startLoading();
        }
    }

    public void fetchWebPhotoList() {
        if (loadingType == ResourceLoadingType.NOT_START) {
            loadingType = ResourceLoadingType.WEB_PHOTO_LIST;
            startLoading();
        }
    }


    ResourceLoaderTask.StreamDecoder bitmapStreamDecoder = new ResourceLoaderTask.StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) {
                onStateChangeListener.onErrorOccurred(BITMAP_LOAD_FAILED + ": " + url);
                return null;
            }
            return new WebBitmap(url, bmp);
        }
    };

    ResourceLoaderTask.StreamDecoder photoListStreamDecoder = new ResourceLoaderTask.StreamDecoder() {

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

    ResourceLoaderTask.ResourceLoaderTaskListener taskListener =
            new ResourceLoaderTask.ResourceLoaderTaskListener() {

                @Override
                public void onResourceReceived(Object obj) {
                    if (obj == null)
                        return;
                    if (obj instanceof WebBitmap) {
                        WebBitmap bmp = (WebBitmap) obj;
                        onStateChangeListener.onReceivedPhoto(bmp);
                    }
                    else if (obj instanceof String) {
                        String result = (String) obj;
                        String[] list = result.isEmpty() ? null : result.split(";");
                        onStateChangeListener.onReceivedPhotoList(list);
                    }
                }

                @Override
                public void onFinished(int count) {
                    if (loadingType == ResourceLoadingType.PHOTO_CONTENT)
                        onStateChangeListener.onFinishedPhotoFetching();
                    loadingType = ResourceLoadingType.NOT_START;
                }
            };

    Handler startLoadingHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (ResourceLoadingType.values()[msg.what]) {
            case WEB_PHOTO_LIST: {
                ResourceLoaderTask task = new ResourceLoaderTask();
                task.setStreamDecoder(photoListStreamDecoder);
                task.setResourceLoaderTaskListener(taskListener);
                task.execute(fetchWebPhotoUrl);
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
                ResourceLoaderTask task = new ResourceLoaderTask();
                task.setStreamDecoder(bitmapStreamDecoder);
                task.setResourceLoaderTaskListener(taskListener);
                task.execute(requests);
                Log.d(tag, "start loading " + urlRequests.toArray());
                break;
            }
            default:
                break;
            }
        }
    };

    public void startLoading() {
        startLoadingHandle.obtainMessage(loadingType.ordinal()).sendToTarget();
    }

}
