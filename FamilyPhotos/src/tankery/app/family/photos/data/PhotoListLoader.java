package tankery.app.family.photos.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

import tankery.app.family.photos.net.WebResourceLoader;
import tankery.app.family.photos.net.WebResourceLoader.StreamDecoder;
import tankery.app.family.photos.net.WebResourceLoader.WebResourceCallback;

public class PhotoListLoader {

    static final String tag = "PhotoListLoader";

    private WebResourceCallback mWebResourceCallback;
    private WebResourceLoader mWebResourceLoader;

    private Queue<String> urlRequests;

    public PhotoListLoader(WebResourceCallback callback) {
        mWebResourceCallback = callback;
        mWebResourceLoader = WebResourceLoader.getInstance();

        urlRequests = new LinkedList<String>();
    }

    public void startLoading(String request) {
        mWebResourceLoader.addResourceLoadRequest(request,
                                                  mWebResourceCallback,
                                                  mPhotoListStreamDecoder);
        Log.d(tag, "schedule loading " + request);
    }

    public void stopLoading() {
        mWebResourceLoader.stopLoading();
        urlRequests.clear();
    }

    private StreamDecoder mPhotoListStreamDecoder = new WebResourceLoader.StreamDecoder() {

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

}
