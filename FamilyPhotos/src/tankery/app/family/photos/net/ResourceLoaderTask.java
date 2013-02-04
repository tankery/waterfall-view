package tankery.app.family.photos.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This Class will create some threads to load the resources
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
public class ResourceLoaderTask {

    static final String tag = "ResourceLoaderTask";

    static final int HTTP_CONNECT_TIMEOUT = 3000;
    static final int HTTP_READ_TIMEOUT = 5000;

    public interface StreamDecoder {
        Object decodeFromStream(String url, InputStream is);
    }

    public interface ResourceLoaderTaskListener {
        void onResourceReceived(Object obj);
        void onFinished(int count);
        void onConnectionTimeout();
    }

    // Default stream decoder will do nothing but return the stream directly.
    private StreamDecoder streamDecoder = new StreamDecoder() {

        @Override
        public Object decodeFromStream(String url, InputStream is) {
            return is;
        }

    };
    private ResourceLoaderTaskListener resourceLoaderTaskListener;

    public synchronized void setStreamDecoder(StreamDecoder streamDecoder) {
        this.streamDecoder = streamDecoder;
    }

    public synchronized void setResourceLoaderTaskListener(
            ResourceLoaderTaskListener resourceLoaderTaskListener) {
        this.resourceLoaderTaskListener = resourceLoaderTaskListener;
    }

    private synchronized Integer doInBackground(String... urls) {
        int count = 0;

        Log.d(tag, "new task.");

        for (String urlStr : urls) {
            if (urlStr == null || urlStr.isEmpty())
                continue;

            try {
                Object obj = null;
                URL url = new URL(urlStr);
                URLConnection conn = url.openConnection();
                try {
                    conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
                    conn.setReadTimeout(HTTP_READ_TIMEOUT);
                    conn.setDoInput(true);
                    String protocol = url.getProtocol();
                    if (protocol.equals("file") ||
                        (protocol.equals("http") && ((HttpURLConnection) conn).getResponseCode()
                            == HttpURLConnection.HTTP_OK)) {
                        InputStream is = conn.getInputStream();
                        obj = streamDecoder.decodeFromStream(urlStr, is);
                    }
                } catch (SocketException e) {
                    Log.e(tag, (e.getMessage() == null ?
                            "Unknow Socket exception" :
                            e.getMessage()));
                    resourceLoaderTaskListener.onConnectionTimeout();
                } catch (SocketTimeoutException e) {
                    Log.e(tag, (e.getMessage() == null ?
                            "Unknow SocketTimeout exception" :
                            e.getMessage()));
                } catch (IOException e) {
                    Log.e(tag, e.getClass().getName() + ": " +
                               (e.getMessage() == null ?
                                       "Unknow IO exception" :
                                       e.getMessage()));
                }

                if (obj != null)
                    count++;
                publishProgress(obj);

            } catch (MalformedURLException e) {
                Log.e(tag, e.getMessage());
            } catch (IOException e) {
                Log.e(tag, e.getClass().getName() + ": " +
                           (e.getMessage() == null ?
                                   "Unknow IO exception" :
                                   e.getMessage()));
            }

            // Escape early if cancel() is called
            if (isCancelled())
                break;
        }

        Log.d(tag, "end task.");
        return count;
    }

    final private class ResourceLoaderThread implements Runnable {

        private final String[] resourceUrls;

        public ResourceLoaderThread(String... urls) {
            resourceUrls = urls;
        }

        @Override
        public void run() {
            Integer count = doInBackground(resourceUrls);
            if (isCancelled())
                taskHandler.obtainMessage(TaskHandlerMessage.CANCEL.ordinal(),
                                          new TaskResult(ResourceLoaderTask.this)).sendToTarget();
            else
                taskHandler.obtainMessage(TaskHandlerMessage.FINISH.ordinal(),
                                          new TaskResult(ResourceLoaderTask.this, count))
                                          .sendToTarget();
        }

    };

    static private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private enum TaskState {
        IDLE,
        EXEUTE,
        CANCELLED,
    }

    private TaskState taskState = TaskState.IDLE;

    public synchronized void execute(String...urls) {
        ResourceLoaderThread thread = new ResourceLoaderThread(urls);
        // thread.setPriority(Thread.NORM_PRIORITY-1);
        executorService.execute(thread);
    }

    public synchronized void cancel(boolean noImplement) {
        if (taskState == TaskState.EXEUTE)
            taskState = TaskState.CANCELLED;
    }

    private boolean isCancelled() {
        return taskState == TaskState.CANCELLED;
    }

    private enum TaskHandlerMessage {
        PROG_UPDATE,
        FINISH,
        CANCEL,
    }

    private static class TaskResult {
        public final ResourceLoaderTask mTask;
        public final Object[] mData;

        public TaskResult(ResourceLoaderTask task, Object... data) {
            mTask = task;
            mData = data;
        }
    }

    private static final Handler taskHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final TaskHandlerMessage types[] = TaskHandlerMessage.values();
            if (msg.what >= types.length) {
                Log.e(tag, "msg.what =" + msg.what +
                           " is out of TaskHandlerMessage.");
                return;
            }
            TaskResult result = (TaskResult) msg.obj;
            switch (types[msg.what]) {
            case PROG_UPDATE:
                result.mTask.onProgressUpdate(result.mData);
                break;
            case FINISH:
                result.mTask.onPostExecute((Integer) result.mData[0]);
                break;
            case CANCEL:
                result.mTask.onCancelled();
                break;
            default:
                Log.e(tag, types[msg.what] + " not invalidate.");
                break;
            }
        }

    };

    private void publishProgress(Object... objs) {
        taskHandler.obtainMessage(TaskHandlerMessage.PROG_UPDATE.ordinal(),
                                  new TaskResult(this, objs)
                                  ).sendToTarget();
    }

    private void onProgressUpdate(Object... objs) {
        if (objs == null || objs.length == 0)
            return;
        Object obj = objs[0];
        resourceLoaderTaskListener.onResourceReceived(obj);
    }

    private void onPostExecute(Integer count) {
        Log.d(tag, "Task finished.");
        resourceLoaderTaskListener.onFinished(count);
    }

    private void onCancelled() {
        Log.d(tag, "Task cancelled.");
    }

}
