package tankery.app.family.photos.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

/**
 * This Class is a AsyncTask to load the resources
 * 
 * User must first set the StreamDecoder, to provide an way to decode the input
 * stream. And then execute with urls.
 * 
 * ResourceLoaderTask will publish an Object decode from the stream each time an
 * content has received. And return the count of Object success decoded.
 * 
 * @author tankery
 * 
 */
public class ResourceLoaderTask extends AsyncTask<String, Object, Integer> {

    static final String tag = "ResourceLoaderTask";

    public interface StreamDecoder {
        Object decodeFromStream(String url, InputStream is);
    }

    public interface ResourceLoaderTaskListener {
        void onResourceReceived(Object obj);

        void onFinished(int count);
    }

    // Default stream decoder will do nothing but return the stream directly.
    private StreamDecoder streamDecoder = new StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            return is;
        }

    };
    private ResourceLoaderTaskListener resourceLoaderTaskListener;

    public void setStreamDecoder(StreamDecoder streamDecoder) {
        this.streamDecoder = streamDecoder;
    }

    public void setResourceLoaderTaskListener(
            ResourceLoaderTaskListener resourceLoaderTaskListener) {
        this.resourceLoaderTaskListener = resourceLoaderTaskListener;
    }

    @Override
    protected Integer doInBackground(String... urls) {
        int count = 0;

        for (String urlStr : urls) {
            if (urlStr == null || urlStr.isEmpty())
                continue;

            Object obj = null;
            try {
                URL url = new URL(urlStr);
                InputStream is = (InputStream) url.getContent();
                obj = streamDecoder.decodeFromStream(urlStr, is);
            } catch (IOException e) {
                Log.e(tag, e.getMessage());
            } finally {
                if (obj != null)
                    count++;
                publishProgress(obj);
            }

            // Escape early if cancel() is called
            if (isCancelled())
                break;
        }

        return count;
    }

    @Override
    protected void onProgressUpdate(Object... objs) {
        if (objs == null || objs.length == 0)
            return;
        Object obj = objs[0];
        Log.d(tag, "Received " + (obj == null ? "null" : obj.toString()));
        resourceLoaderTaskListener.onResourceReceived(obj);
    }

    @Override
    protected void onPostExecute(Integer count) {
        Log.d(tag, "Task finished.");
        resourceLoaderTaskListener.onFinished(count);
    }

}
