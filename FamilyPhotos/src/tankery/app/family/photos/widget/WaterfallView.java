package tankery.app.family.photos.widget;

import java.util.ArrayList;
import java.util.Random;

import tankery.app.family.photos.data.CachedBitmap;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
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

    static final String LOGTAG = "WaterfallView";

    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int DEFAULT_COLUMN_COUNT = 3;

    private int columnCount = 0;
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

    public void init() {
        init(DEFAULT_COLUMN_COUNT);
    }

    public void init(int colCount) {
        initLayout();
        setColumnCount(colCount);
    }

    public void setColumnCount(int count) {
        if (columnCount == count)
            return;

        int oldCount = columnCount;
        columnCount = count;
        columnWidth = getDisplayWidth();
        updateColumns(oldCount, columnCount);
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public void showUserMessage(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void showUserMessage(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
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

        super.setChild(child);
    }

    private void updateColumns(int oldCount, int newCount) {

        if (itemColumns == null) {
            assert oldCount == 0;
            itemColumns = new ArrayList<WaterfallItemColumn>(newCount);
        }

        int deltaCount = 0;
        if (oldCount < newCount) {
            deltaCount = newCount - oldCount;
        }
        else {
            getChild().removeAllViews();
            deltaCount = newCount;
        }

        // Add item columns to item columns array.
        for (int i = 0; i < deltaCount; i++) {
            WaterfallItemColumn itemColumn =
                    new WaterfallItemColumn(getContext());
            itemColumns.add(itemColumn);
            getChild().addView(itemColumn);

            itemColumn.init(getColumnWidth());
        }
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
    protected void onVScrollChanged(int t, int oldt) {
        if (t != oldt) {
            for (WaterfallItemColumn col : itemColumns) {
                col.updateViewport(t, getMeasuredHeight());
            }
        }
        super.onVScrollChanged(t, oldt);
    }

    @Override
    public void onTopReached() {
        for (WaterfallItemColumn column : itemColumns) {
            column.clear();
        }
        super.onTopReached();
    }

    @Override
    public void onBottomReached() {
        super.onBottomReached();
    }

    @Override
    public void onScrolling() {
        super.onScrolling();
    }

    public void appendNewBitmaps(ArrayList<CachedBitmap> bitmaps) {
        for (CachedBitmap cbmp : bitmaps) {
            columnNeedAdding().addBitmap(cbmp);
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

    @Override
    protected LinearLayout getChild() {
        return (LinearLayout) super.getChild();
    }

}
