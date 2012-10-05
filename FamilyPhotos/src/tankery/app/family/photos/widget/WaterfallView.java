package tankery.app.family.photos.widget;

import java.util.ArrayList;
import java.util.Random;

import tankery.app.family.photos.data.PhotoStorage;
import tankery.app.family.photos.data.PhotoStorage.PhotoStorageListener;
import tankery.app.familyphotos.R;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * The Waterfall view is a lazy vertical scroll view,
 * who contains an horizon linear child with several
 * WaterfallItemColumn to manage the items.
 * 
 * @author tankery
 *
 */
public class WaterfallView extends LazyVScrollView {

    private static final String tag = "WaterfallView";

    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int PHOTO_FETCHING_COUNT = 30;

    private int columnCount = 3;
    private int columnWidth;

    private ArrayList<WaterfallItemColumn> itemColumns;

    public WaterfallView(Context context) {
        super(context);
    }

    public WaterfallView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaterfallView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(int colCount) {
        columnCount = colCount;
        init();
    }

    public void init() {
        initLayout();
        initEvent();
        PhotoStorage storage = PhotoStorage.getInstance();
        storage.setPhotoCompressedWidth(columnWidth);
        storage.setOnFetchingFinishedListener(
               new PhotoStorageListener() {

                   @Override
                   public void onListFetchingFinished() {
                       needAppendNewItems();
                   }

                   @Override
                   public void onPhotoReceived(int id) {
                       ArrayList<Integer> list = new ArrayList<Integer>();
                       list.add(id);
                       doAppendNewItems(list);
                   }

                   @Override
                   public void onPhotoFetchingFinished(
                           ArrayList<Integer> updatedIdList) {
                       // don't append new item when finished
                       // cause i have move the new item adding to the time
                       // each photo received.
                       // doAppendNewItems(updatedIdList);
                   }
               });
        storage.refreshPhotoList();
        // append new item at initial.
        needAppendNewItems();
    }

    private void initLayout() {
        Context context = getContext();

        // Initialize the LazyVScrollView with an horizon linear layout.
        LinearLayout child = new LinearLayout(context);
        LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        child.setOrientation(LinearLayout.HORIZONTAL);
        child.setLayoutParams(itemParam);
        child.setBackgroundColor(BACKGROUND_COLOR);
        super.init(child);

        columnWidth = getDisplayWidth();

        // Add item columns to item columns array.
        itemColumns = new ArrayList<WaterfallItemColumn>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            WaterfallItemColumn itemColumn = new WaterfallItemColumn(context);
            itemColumns.add(itemColumn);
            getChild().addView(itemColumn);

            itemColumn.init(columnWidth);
        }
    }

    private void initEvent() {
        setOnScrollListener(new OnScrollListener() {

            @Override
            public void onTopReached() {
                Log.d(tag, "Scrolled to top");
                Toast.makeText(getContext(), R.string.waterfall_refresh,
                               Toast.LENGTH_SHORT)
                     .show();
                PhotoStorage.getInstance().refreshPhotoList();
                for (WaterfallItemColumn column : itemColumns) {
                    column.removeAllViews();
                }
            }

            @Override
            public void onBottomReached() {
                Log.d(tag, "Scrolled to bottom");
                Toast.makeText(getContext(), R.string.waterfall_adding_item,
                               Toast.LENGTH_SHORT)
                     .show();
                needAppendNewItems();
            }

            @Override
            public void onScrolling() {
            }

            @Override
            public void onScrollChanged(int t, int oldt) {
            }
        });
    }

    Handler needAppendNewItemsHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PhotoStorage storage = PhotoStorage.getInstance();
            storage.fetchMorePhotos(PHOTO_FETCHING_COUNT);
        }
    };

    private void needAppendNewItems() {
        needAppendNewItemsHandler.sendMessageDelayed(needAppendNewItemsHandler.obtainMessage(),
                                                     200);
    }

    private void doAppendNewItems(ArrayList<Integer> updatedIdList) {
        for (int id : updatedIdList) {
            columnNeedAdding().addPhoto(id);
        }
    }

    Random random = new Random(System.currentTimeMillis());

    private WaterfallItemColumn columnNeedAdding() {
        if (itemColumns == null || itemColumns.size() == 0)
            return null;

        // int r = random.nextInt(itemColumns.size());
        //
        // return itemColumns.get(r);

        WaterfallItemColumn shortest = itemColumns.get(0);
        for (WaterfallItemColumn column : itemColumns) {
            if (column.getMeasuredHeight() < shortest.getMeasuredHeight()) {
                shortest = column;
            }
        }
        return shortest;
    }

    // This method should support low level SDK.
    @SuppressWarnings("deprecation")
    private int getDisplayWidth() {
        // calculate the column width for item columns.
        WindowManager manager = (WindowManager)
                getContext().getSystemService(Context.WINDOW_SERVICE);
        return manager.getDefaultDisplay().getWidth() / columnCount;
    }

    @Override
    protected LinearLayout getChild() {
        return (LinearLayout) super.getChild();
    }

}
