/**
 *
 */
package tankery.app.modules.waterfall.widget;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import tankery.app.modules.utils.AlgorithmHelper;
import tankery.app.modules.waterfall.data.CachedBitmap;

import android.content.Context;
import android.graphics.Bitmap;
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

    public void clear() {
        itemIdTableLock.lock();
        itemIdTable.clear();
        itemIdTableLock.unlock();
        removeAllViews();
    }

    public void addBitmap(CachedBitmap cbmp) {
        // get photo from photo table by id.
        Bitmap bmp = cbmp.getBitmap();
        if (bmp == null || bmp.isRecycled()) {
            Log.e(tag, "Bitmap [" + cbmp + "] is " +
                    (bmp == null ? "null" : "recycled") +
                    " when addPhoto.");
            return;
        }

        // create item view and map current position to its id
        WaterfallItem item = new WaterfallItem(getContext());
        item.setPadding(0, 2, 0, 2);

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int layoutHeight = (height * columnWidth) / width;
        // The measured height must increase when adding a new item.
        if (layoutHeight == 0)
            layoutHeight = 1;
        LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, layoutHeight);
        item.setLayoutParams(itemParam);
        item.setCachedBitmap(cbmp);

        // measure the layout before adding new photo.
        measure(0, 0);

        itemIdTableLock.lock();
        item.setId(itemIdTable.size());
        itemIdTable.append(getMeasuredHeight(), item);
        itemIdTableLock.unlock();

        // at last, add it to item column.
        addView(item);
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

    private void reloadItems(final ArrayList<WaterfallItem> itemsNeedReload) {
        if (itemsNeedReload == null || itemsNeedReload.isEmpty())
            return;

        Log.d(tag, "reload items: " + itemsNeedReload.toString());

        for (WaterfallItem view : itemsNeedReload) {
            view.reload();
        }
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
