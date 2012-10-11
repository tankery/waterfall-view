/**
 * 
 */
package tankery.app.family.photos.widget;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import tankery.app.family.photos.data.PhotoStorage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * @author tankery
 *
 */
public class WaterfallItemColumn extends LinearLayout {
    
    static final String tag = "WaterfallItemColumn";
    
    private static final int RETAIN_AREA_HEIGHT_MUTIPLE = 3;

    private int columnWidth = 0;

    /**
     * use SparseArray to map item's top coordination to item id. the key of
     * table is the item top coordinate, and the value is the item id.
     */
    private SparseArray<Integer> itemIdTable = new SparseArray<Integer>();

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

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int id = msg.what;

            // get photo from photo table by id.
            Bitmap bmp = PhotoStorage.getInstance().getPhoto(id);
            if (bmp == null) {
                Log.e(tag, "Bitmap [" + id + "] is null when addPhoto.");
                return;
            }

            // create item view and map current position to its id
            ImageView view = new ImageView(getContext());
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
            view.setImageBitmap(bmp);

            itemIdTable.append(getMeasuredHeight(), id);

            // at last, add it to item column.
            addView(view);
        }
    };

    public void addPhoto(int id) {
        handler.obtainMessage(id).sendToTarget();
    }

    // the view port changed, we should recycle the view that far away from
    // viewport.
    public void updateViewport(int top, int height) {
        // viewport not change or new top not invalidate, return;
        if (top == currentTop || top < 0 || top > getMeasuredHeight())
            return;

        // if the top is near to currentTop, ignore it.
        if (Math.abs(top - currentTop) < height)
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
        ArrayList<Integer> itemsNeedReload = null;
        ArrayList<Integer> itemsNeedRecycle = null;
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
        // TODO: put in new thread.
        reloadItems(itemsNeedReload);
        recycleItems(itemsNeedRecycle);
    }

    private ArrayList<Integer> getItemsInArea(int top, int bottom) {
        if (top >= bottom)
            return null;

        int topItemIndex = -1;
        int bottomItemIndex = -1;

        // using binary search to find the items at top and bottom.
        int start, end, midPt = 0;
        start = 0;
        end = itemIdTable.size() - 1;
        // first, find the item between the search area.
        while (start <= end) {
            midPt = (start + end) / 2;
            int itemToTop = positionRelatedToLine(midPt, top);
            int itemToBottom = positionRelatedToLine(midPt, bottom);
            if (itemToTop == 0) {
                topItemIndex = midPt;
                break;
            } else if (itemToBottom == 0) {
                bottomItemIndex = midPt;
                break;
            } else if (itemToTop > 0 && itemToBottom < 0) {
                // find the center item, just break;
                break;
            } else if (itemToTop < 0) {
                start = midPt + 1;
            } else if (itemToBottom > 0) {
                end = midPt - 1;
            }
        }

        if (start > end)
            return null; // not found.

        // if item on top not found, find it
        if (topItemIndex == -1) {
            int s, e, m;
            s = start;
            e = midPt - 1;
            while (s <= e) {
                m = (s + e) / 2;
                int itemToTop = positionRelatedToLine(m, top);
                if (itemToTop == 0) {
                    topItemIndex = m;
                    break;
                } else if (itemToTop < 0) {
                    s = m + 1;
                } else if (itemToTop > 0) {
                    e = m - 1;
                }
            }
            if (s > e)
                return null; // not found.
        }

        // if item on bottom not found, find it
        if (bottomItemIndex == -1) {
            int s, e, m;
            s = midPt + 1;
            e = end;
            while (s <= e) {
                m = (s + e) / 2;
                int itemToBottom = positionRelatedToLine(m, bottom);
                if (itemToBottom == 0) {
                    bottomItemIndex = m;
                    break;
                } else if (itemToBottom < 0) {
                    s = m + 1;
                } else if (itemToBottom > 0) {
                    e = m - 1;
                }
            }
            if (s > e)
                return null; // not found.
        }

        // one item on top and bottom at the same time, treat as not found.
        if (topItemIndex == bottomItemIndex)
            return null;

        ArrayList<Integer> result =
                new ArrayList<Integer>(bottomItemIndex - topItemIndex);
        for (int i = topItemIndex; i < bottomItemIndex; i++) {
            result.add(itemIdTable.valueAt(i));
        }

        return result;
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
    private int positionRelatedToLine(int index, int topLine) {
        if (index >= itemIdTable.size())
            return 1;
        else if (index < 0)
            return -1;

        int nextItemTop = index + 1 == itemIdTable.size() ? getMeasuredHeight() : itemIdTable.keyAt(index + 1);
        int itemTop = itemIdTable.keyAt(index);

        if (nextItemTop <= topLine)
            return -1;
        else if (itemTop <= topLine && nextItemTop > topLine)
            return 0;
        else
            return 1;
    }

    private void reloadItems(final ArrayList<Integer> itemsNeedReload) {
        if (itemsNeedReload == null || itemsNeedReload.isEmpty())
            return;

        Log.d(tag, "reload items: " + itemsNeedReload.toString());

        new Thread() {
            public void run() {
                for (int id : itemsNeedReload) {
                    Bitmap bmp = PhotoStorage.getInstance().getPhoto(id);
                    if (bmp == null || bmp.isRecycled()) {
                        String name = String.valueOf(id);
                        try {
                            FileInputStream fis =
                                    getContext().getApplicationContext()
                                                .openFileInput(name);
                            Bitmap newBmp = BitmapFactory.decodeStream(fis);
                            if (newBmp != null)
                                PhotoStorage.getInstance().setPhoto(id, newBmp);
                        } catch (FileNotFoundException e) {
                            Log.e(tag, e.getMessage());
                            return;
                        }
                    }
                }
            }
        }.start();

    }

    private void recycleItems(final ArrayList<Integer> itemsNeedRecycle) {
        if (itemsNeedRecycle == null || itemsNeedRecycle.isEmpty())
            return;

        Log.d(tag, "recycle items: " + itemsNeedRecycle.toString());

        new Thread() {
            public void run() {
                for (int id : itemsNeedRecycle) {
                    // set the bitmap in PhotoStorage will cause the old one
                    // recycle.
                    PhotoStorage.getInstance().setPhoto(id, null);
                }
            }
        }.start();

    }

}
