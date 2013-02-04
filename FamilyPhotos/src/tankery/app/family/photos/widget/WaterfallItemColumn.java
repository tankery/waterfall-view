/**
 *
 */
package tankery.app.family.photos.widget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import tankery.app.family.photos.data.PhotoStorage;
import tankery.app.family.photos.utils.AlgorithmHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.widget.LinearLayout;

/**
 * @author tankery
 *
 */
public class WaterfallItemColumn extends LinearLayout {

    static final String tag = "WaterfallItemColumn";

    private static final int RETAIN_AREA_HEIGHT_MUTIPLE = 2;

    private int columnWidth = 0;

    /**
     * use SparseArray to map item's top coordination to item id. the key of
     * table is the item top coordinate, and the value is the item id.
     */
    private SparseArray<WaterfallItem> itemIdTable = new SparseArray<WaterfallItem>();
    Lock itemIdTableLock = new ReentrantLock();

    private int currentTop = 0;

    public WaterfallItemColumn(Context context) {
        super(context);
    }

    public WaterfallItemColumn(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaterfallItemColumn(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(int width) {
        columnWidth = width;

        LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
                width, LayoutParams.WRAP_CONTENT);

        setPadding(2, 2, 2, 2);
        setOrientation(LinearLayout.VERTICAL);

        setLayoutParams(itemParam);
    }

    private static final int MSG_CLEAR_VIEWS = 0;
    private static final int MSG_ADD_PHOTO = 1;
    private static final int MSG_SET_PHOTO = 2;

    private final Handler itemsChangeHandler = new ItemsChangeHandler(this);

    private final static class ItemsChangeHandler extends Handler {

        private WeakReference<WaterfallItemColumn> mColumn;

        ItemsChangeHandler(WaterfallItemColumn column) {
            mColumn = new WeakReference<WaterfallItemColumn>(column);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_CLEAR_VIEWS:
                mColumn.get().doClear();
                break;
            case MSG_ADD_PHOTO:
                mColumn.get().doAddPhoto(msg.arg1);
                break;
            case MSG_SET_PHOTO:
                mColumn.get().doSetPhoto(msg.arg1, msg.arg2);
                break;
            default:
                break;
            }
        }
    }

    public void clear() {
        itemsChangeHandler.obtainMessage(MSG_CLEAR_VIEWS).sendToTarget();
    }

    public void addPhoto(int id) {
        itemsChangeHandler.obtainMessage(MSG_ADD_PHOTO, id, 0).sendToTarget();
    }

    public void setPhoto(WaterfallItem item, int photoId) {
        itemsChangeHandler.obtainMessage(MSG_SET_PHOTO, photoId, item.getId()).sendToTarget();
    }

    private void doClear() {
        itemIdTableLock.lock();
        itemIdTable.clear();
        itemIdTableLock.unlock();
        removeAllViews();
    }

    private void doAddPhoto(int id) {
        // get photo from photo table by id.
        Bitmap bmp = PhotoStorage.getInstance().getPhoto(id);
        if (bmp == null) {
            Log.e(tag, "Bitmap [" + id + "] is null when addPhoto.");
            return;
        }

        // create item view and map current position to its id
        WaterfallItem view = new WaterfallItem(getContext());
        view.setPadding(0, 2, 0, 2);

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int layoutHeight = (height * columnWidth) / width;
        // The measured height must increase when adding a new item.
        if (layoutHeight == 0)
            layoutHeight = 1;
        LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, layoutHeight);
        view.setLayoutParams(itemParam);
        view.setImageBitmap(id);

        // measure the layout before adding new photo.
        measure(0, 0);

        itemIdTableLock.lock();
        view.setId(itemIdTable.size());
        itemIdTable.append(getMeasuredHeight(), view);
        itemIdTableLock.unlock();

        // at last, add it to item column.
        addView(view);
    }

    private void doSetPhoto(int photoId, int itemId) {
        WaterfallItem item = (WaterfallItem) findViewById(itemId);
        if (item == null) {
            Log.e(tag, "WaterfallItem [" + itemId + "] is null when setPhoto.");
            return;
        }
        // get photo from photo table by id.
        Bitmap bmp = PhotoStorage.getInstance().getPhoto(photoId);
        // set null bitmap is validate.
        item.setImageBitmap(bmp);
    }

    /**
     * the view port changed, we should recycle the view that far away from
     * viewport.
     *
     * @param top the new scroll top of column
     * @param height the parent visible height
     */
    public void updateViewport(int top, int height) {
        // viewport not change or new top not invalidate, return;
        if (top == currentTop || top < 0 || top > getMeasuredHeight())
            return;

        // if the top is near to currentTop, ignore it.
        if (Math.abs(top - currentTop) < (height * RETAIN_AREA_HEIGHT_MUTIPLE / 4))
            return;

        // calculate the current/new retain area top and bottom.
        int rCurTop = currentTop - height * (RETAIN_AREA_HEIGHT_MUTIPLE - 1);
        int rCurBottom = currentTop + height * RETAIN_AREA_HEIGHT_MUTIPLE;
        int rNewTop = top - height * (RETAIN_AREA_HEIGHT_MUTIPLE - 1);
        int rNewBottom = top + height * RETAIN_AREA_HEIGHT_MUTIPLE;

        // make them reasonable
        int minTop = 0;
        // max bottom must lager than last item's bottom, so, do a plus 1.
        int maxBottom = getMeasuredHeight() + 1;
        if (rCurTop < minTop)
            rCurTop = minTop;
        if (rCurBottom > maxBottom)
            rCurBottom = maxBottom;
        if (rNewTop < minTop)
            rNewTop = minTop;
        if (rNewBottom > maxBottom)
            rNewBottom = maxBottom;

        // calculate the views need recycle and reload.
        ArrayList<WaterfallItem> itemsNeedReload = null;
        ArrayList<WaterfallItem> itemsNeedRecycle = null;
        if (top > currentTop) {
            // viewport move down (scroll up).
            itemsNeedReload = getItemsInArea(Math.max(rCurBottom, rNewTop),
                                             rNewBottom);
            itemsNeedRecycle = getItemsInArea(rCurTop, rNewTop);
        }
        else {
            // viewport move up (scroll down).
            itemsNeedReload = getItemsInArea(rNewTop,
                                             Math.min(rNewBottom, rCurTop));
            itemsNeedRecycle = getItemsInArea(rNewBottom, rCurBottom);
        }

        currentTop = top;

        // do recycle and reload.
        reloadItems(itemsNeedReload);
        recycleItems(itemsNeedRecycle);
    }

    private AlgorithmHelper.Comparetor<Integer>
        positionToLineComparetor = new AlgorithmHelper.Comparetor<Integer>() {

        @Override
        public int compareTarget(Integer obj1, Integer obj2) {
            return obj1 - obj2;
        }

        /**
         * the position related to a line.
         *
         * @param index
         *            index of waterfall item in itemIdTable to compare
         * @param topLine
         *            the height of the top line
         * @return if return < 0, item on the top of topLine, = 0, item overlap
         *         line, > 0, item on the bottom of topLine.
         */
        @Override
        public int itemRelatedToTarget(int index, Integer topLine) {
            if (index >= itemIdTable.size())
                return 1;
            else if (index < 0)
                return -1;

            int nextItemTop = (index + 1 == itemIdTable.size()) ?
                    getMeasuredHeight() :
                    itemIdTable.keyAt(index + 1);
            int itemTop = itemIdTable.keyAt(index);

            if (nextItemTop < topLine)
                return -1;
            else if (itemTop < topLine && nextItemTop >= topLine)
                return 0;
            else
                return 1;
        }
    };

    private ArrayList<WaterfallItem> getItemsInArea(int top, int bottom) {
        itemIdTableLock.lock();
        try {
            int[] indexes =
                    AlgorithmHelper.binaryFindBetween(0,
                                                      itemIdTable.size(),
                                                      top, bottom,
                                                      positionToLineComparetor);

            if (indexes == null)
                return null;

            ArrayList<WaterfallItem> result =
                    new ArrayList<WaterfallItem>(indexes[1] - indexes[0]);
            for (int i = indexes[0]; i < indexes[1]; i++) {
                result.add(itemIdTable.valueAt(i));
            }

            return result;
        }
        finally {
            itemIdTableLock.unlock();
        }
    }

    private Queue<WaterfallItem> dirtyItemsList = new LinkedList<WaterfallItem>();
    Lock dirtyItemsListLock = new ReentrantLock();

    private class ItemsReloadRecycleTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... obj) {
            Log.d(tag, "new task.");
            while (dirtyItemsList.size() > 0) {
                dirtyItemsListLock.lock();
                WaterfallItem item = dirtyItemsList.poll();
                dirtyItemsListLock.unlock();
                if (item.needRecycle())
                    item.recycle();
                else if (item.needReload())
                    item.reload();
            }
            Log.d(tag, "end task.");
            return null;
        }

        @Override
        protected void onPostExecute(Object obj) {
            itemsReloadRecycleTask = null;
        }
    }

    private ItemsReloadRecycleTask itemsReloadRecycleTask = null;

    private void reloadItems(final ArrayList<WaterfallItem> itemsNeedReload) {
        if (itemsNeedReload == null || itemsNeedReload.isEmpty())
            return;

        Log.d(tag, "reload items: " + itemsNeedReload.toString());

        for (WaterfallItem view : itemsNeedReload) {
            view.reloadIfNeed();
            if (!dirtyItemsList.contains(view)) {
                dirtyItemsListLock.lock();
                dirtyItemsList.add(view);
                dirtyItemsListLock.unlock();
            }
        }

        if (itemsReloadRecycleTask == null) {
            itemsReloadRecycleTask = new ItemsReloadRecycleTask();
            itemsReloadRecycleTask.execute();
        }
    }

    private void recycleItems(final ArrayList<WaterfallItem> itemsNeedRecycle) {
        if (itemsNeedRecycle == null || itemsNeedRecycle.isEmpty())
            return;

        Log.d(tag, "recycle items: " + itemsNeedRecycle.toString());

        for (WaterfallItem view : itemsNeedRecycle) {
            view.recycleIfNeed();
            if (!dirtyItemsList.contains(view)) {
                dirtyItemsListLock.lock();
                dirtyItemsList.add(view);
                dirtyItemsListLock.unlock();
            }
        }

        if (itemsReloadRecycleTask == null) {
            itemsReloadRecycleTask = new ItemsReloadRecycleTask();
            itemsReloadRecycleTask.execute();
        }
    }

}
