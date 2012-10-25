/**
 * 
 */
package tankery.app.family.photos.widget;

import java.util.ArrayList;

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

    private static final int MSG_CLEAR = 0;
    private static final int MSG_ADD_PHOTO = 1;
    private static final int MSG_SET_PHOTO = 2;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int id;
            Bitmap bmp = null;
            switch(msg.what) {
            case MSG_CLEAR:
                itemIdTable.clear();
                removeAllViews();
                break;
            case MSG_ADD_PHOTO:
                id = (Integer) msg.arg1;
                // get photo from photo table by id.
                bmp = PhotoStorage.getInstance().getPhoto(id);
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

                itemIdTable.append(getMeasuredHeight(), view);
                view.setId(itemIdTable.size());

                // at last, add it to item column.
                addView(view);
                break;
            case MSG_SET_PHOTO:
                WaterfallItem item = (WaterfallItem) findViewById(msg.arg2);
                if (item == null) {
                    Log.e(tag, "WaterfallItem [" + msg.arg2 + "] is null when setPhoto.");
                    return;
                }
                // get photo from photo table by id.
                bmp = PhotoStorage.getInstance().getPhoto(msg.arg1);
                // set null bitmap is validate.
                item.setImageBitmap(bmp);
            default:
                break;
            }
        }
    };

    public void clear() {
        handler.obtainMessage(MSG_CLEAR).sendToTarget();
    }

    public void addPhoto(int id) {
        handler.obtainMessage(MSG_ADD_PHOTO, id, 0).sendToTarget();
    }

    public void setPhoto(WaterfallItem item, int photoId) {
        handler.obtainMessage(MSG_SET_PHOTO, photoId, item.getId()).sendToTarget();
    }

    /**
     * the view port changed, we should recycle the view that far away from
     * viewport.
     * 
     * FIXME: when scroll to the top or bottom of the column, some photos may
     * not been reloaded.
     * @param top the new scroll top of column
     * @param height the parent visible height
     */
    public void updateViewport(int top, int height) {
        // viewport not change or new top not invalidate, return;
        if (top == currentTop || top < 0 || top > getMeasuredHeight())
            return;

        // if the top is near to currentTop, and not near top or bottom, ignore it.
        if (Math.abs(top - currentTop) < (height / 2) &&
                top - 0 > (height / 2) &&
                top + height < (getMeasuredHeight() - height / 2))
            return;

        // calculate the current/new retain area top and bottom.
        int rCurTop = currentTop - height * (RETAIN_AREA_HEIGHT_MUTIPLE - 1);
        int rCurBottom = currentTop + height * RETAIN_AREA_HEIGHT_MUTIPLE;
        int rNewTop = top - height * (RETAIN_AREA_HEIGHT_MUTIPLE - 1);
        int rNewBottom = top + height * RETAIN_AREA_HEIGHT_MUTIPLE;
        // make them reasonable
        if (rCurTop < 0)
            rCurTop = 0;
        if (rCurBottom > getMeasuredHeight())
            rCurBottom = getMeasuredHeight();
        if (rNewTop < 0)
            rNewTop = 0;
        if (rNewBottom > getMeasuredHeight())
            rNewBottom = getMeasuredHeight();

        // calculate the views need recycle and reload.
        ArrayList<WaterfallItem> itemsNeedReload = null;
        ArrayList<WaterfallItem> itemsNeedRecycle = null;
        if (top > currentTop) {
            // viewport move down (scroll up).
            itemsNeedReload = getItemsInArea(Math.max(rCurBottom, rNewTop),
                                             rNewBottom);
            itemsNeedRecycle = getItemsInArea(rCurTop,
                                              Math.min(rCurBottom, rNewTop));
        }
        else {
            // viewport move up (scroll down).
            itemsNeedReload = getItemsInArea(rNewTop,
                                             Math.min(rNewBottom, rCurTop));
            itemsNeedRecycle = getItemsInArea(Math.max(rNewBottom, rCurTop),
                                              rCurBottom);
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

            if (nextItemTop <= topLine)
                return -1;
            else if (itemTop <= topLine && nextItemTop > topLine)
                return 0;
            else
                return 1;
        }
    };

    private ArrayList<WaterfallItem> getItemsInArea(int top, int bottom) {
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

    private class ItemsReloadTask extends AsyncTask<WaterfallItem, Object, Object> {
        @Override
        protected Object doInBackground(WaterfallItem... items) {
            for (WaterfallItem item : items) {
                item.reload();

                if (isCancelled())
                    break;
            }
            return null;
        }
    }

    private void reloadItems(final ArrayList<WaterfallItem> itemsNeedReload) {
        if (itemsNeedReload == null || itemsNeedReload.isEmpty())
            return;

        Log.d(tag, "reload items: " + itemsNeedReload.toString());

        WaterfallItem[] items = new WaterfallItem[itemsNeedReload.size()];
        itemsNeedReload.toArray(items);
        ItemsReloadTask itemsReloadTask = new ItemsReloadTask();
        itemsReloadTask.execute(items);
    }

    private void recycleItems(final ArrayList<WaterfallItem> itemsNeedRecycle) {
        if (itemsNeedRecycle == null || itemsNeedRecycle.isEmpty())
            return;

        Log.d(tag, "recycle items: " + itemsNeedRecycle.toString());

        for (WaterfallItem view : itemsNeedRecycle) {
            view.recycle();
        }

    }

}
