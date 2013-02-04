package tankery.app.family.photos.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import tankery.app.family.photos.net.ResourceLoaderTask;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

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

    private int photoCompressedWidth = 0;

    private Context applicationContext = null;
    private ArrayList<String> tempPhotoFileList = null;
    private SparseArray<String> tempFileUrlTable = new SparseArray<String>();

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

    public void setPhotoCompressedWidth(int width) {
        photoCompressedWidth = width;
    }

    public void useTempPhotoFile(Context appContext) {
        if (appContext == null)
            return;
        applicationContext = appContext;
        tempPhotoFileList =
                new ArrayList<String>(Arrays.asList(applicationContext.fileList()));
    }

    /* (non-Javadoc)
     * @see tankery.app.family.photos.data.PhotoLoader#addLoadRequest(java.lang.String)
     */
    @Override
    public void addLoadRequest(String urlStr) {
        String tempFilename = generateTempFileName(urlStr);
        if (fileInTempFolder(tempFilename)) {
            // photo is stored in temperate directory, change to load with it.
            tempFileUrlTable.append(Integer.parseInt(tempFilename), urlStr);
            urlStr = generateTempFileUrl(tempFilename);
        }
        else {
            urlStr = longUrl(urlStr);
        }
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

    private ResourceLoaderTask resourceLoaderTask = new ResourceLoaderTask();;

    private ResourceLoaderTask.ResourceLoaderTaskListener taskListener =
            new ResourceLoaderTask.ResourceLoaderTaskListener() {

                @Override
                public void onResourceReceived(Object obj) {
                    if (obj == null)
                        return;
                    if (obj instanceof WebBitmap) {
                        WebBitmap bmp = (WebBitmap) obj;
                        photoLoaderListener.onReceivedPhoto(bmp);
                        saveBitmapToFileIfNeed(bmp);
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
            Bitmap bmp = null;
            boolean compress = photoCompressedWidth != 0;
            bmp = decodeStream(is, compress);

            if (bmp == null) {
                photoLoaderListener.onErrorOccurred(ResourceLoadingError.BITMAP_DECODE_NULL,
                                                      url);
                return null;
            }
            String mabeFilename = parseTempFileUrl(url);
            if (mabeFilename != null) {
                url = tempFileUrlTable.get(Integer.parseInt(mabeFilename));
            }
            return new WebBitmap(shortUrl(url), bmp);
        }

        private Bitmap decodeStream(InputStream is, boolean compress) {
            // catch OOM exception.
            Bitmap bmp = null;
            try {
                bmp = BitmapFactory.decodeStream(is);
            } catch (OutOfMemoryError e) {
                Log.e(tag, "Out of Memory Error!");
                bmp = null;
            }
            if (bmp != null && compress) {
                int width = photoCompressedWidth;
                int height = (bmp.getHeight() * width) / bmp.getWidth();
                Bitmap newBitmap = Bitmap.createScaledBitmap(bmp, width,
                                                             height,
                                                             false);
                bmp.recycle();
                bmp = newBitmap;
            }
            return bmp;
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

    private Handler updateLoadingHandle = new UpdateLoadingHandler(this);

    private final static class UpdateLoadingHandler extends Handler {

        private WeakReference<NetworkPhotoLoader> mLoader;

        UpdateLoadingHandler(NetworkPhotoLoader loader) {
            mLoader = new WeakReference<NetworkPhotoLoader>(loader);
        }

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
                mLoader.get().doCancelLoading();
                break;
            case WEB_PHOTO_LIST:
                mLoader.get().doLoadingWebPhotoList();
                break;
            case PHOTO_CONTENT:
                mLoader.get().doLoadingPhotoContent();
                break;
            default:
                Log.e(tag, types[msg.what].name() + " is invalidate.");
                break;
            }
        }
    }

    private void updateLoading() {
        updateLoadingHandle.obtainMessage(loadingType.ordinal()).sendToTarget();
    }

    private void doCancelLoading() {
        resourceLoaderTask.cancel(false);
    }

    private void doLoadingWebPhotoList() {
        resourceLoaderTask.setResourceLoaderTaskListener(taskListener);
        resourceLoaderTask.setStreamDecoder(photoListStreamDecoder);
        resourceLoaderTask.execute(fetchWebPhotoUrl);
        Log.d(tag, "start loading " + fetchWebPhotoUrl);
    }

    private void doLoadingPhotoContent() {
        int count = urlRequests.size();
        if (count == 0)
            return;
        String[] requests = new String[count];
        for (int i = 0; i < count; i++) {
            requests[i] = urlRequests.poll();
        }
        resourceLoaderTask.setResourceLoaderTaskListener(taskListener);
        resourceLoaderTask.setStreamDecoder(bitmapStreamDecoder);
        resourceLoaderTask.execute(requests);
        Log.d(tag,
              "start loading [" + TextUtils.join(", ", requests) + "]");
    }

    private void saveBitmapToFileIfNeed(WebBitmap bmp) {
        if (tempPhotoFileList == null)
            return;

        // save photo to temp file if not exist yet.
        String filename = generateTempFileName(bmp.url);
        if (!fileInTempFolder(filename)) {
            tempPhotoFileList.add(filename);
            FileOutputStream fos;
            try {
                fos = applicationContext.openFileOutput(filename,
                                                        Context.MODE_PRIVATE);
                bmp.bitmap.compress(Bitmap.CompressFormat.PNG,
                                    90, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private String shortUrl(String url) {
        return url.contains(HOST_ADDRESS) ?
                url.substring(HOST_ADDRESS.length()) :
                url;
    }

    private String longUrl(String url) {
        return url.contains(HOST_ADDRESS) ?
                url :
                HOST_ADDRESS + url;
    }

    private boolean fileInTempFolder(String filename) {
        return tempPhotoFileList != null && tempPhotoFileList.contains(filename);
    }

    private String generateTempFileName(String url) {
        return String.valueOf(shortUrl(url).hashCode());
    }

    private String generateTempFileUrl(String filename) {
        return "file://" + applicationContext.getFilesDir() + "/" + filename;
    }

    private String parseTempFileUrl(String url) {
        String prefix = "file://" + applicationContext.getFilesDir() + "/";
        return url.contains(prefix) ? url.substring(prefix.length()) : null;
    }

}
