package tankery.app.family.photos.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * Vertical Scroll View who lazy load the content.
 *
 * @author tankery
 *
 */
public class LazyVScrollView extends ScrollView {

    static final String LOGTAG = "LazyVScrollView";

    private static final int CONTENT_LOAD_THRESHOLD_HEIGHT = 20;

    private View child = null;

    public LazyVScrollView(Context context) {
        super(context);
    }

    public LazyVScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LazyVScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean setChild(View child) {
        if (child != null) {
            this.addView(child);
        }
        this.child = getChildAt(0);
        return true;
    }

    protected View getChild() {
        return this.child;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            onTouchFinished();
            break;

        default:
            break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        onVScrollChanged(t, oldt);
    }

    private void onTouchFinished() {
        if (child != null) {

            // the on top reached event should judged before bottom reached.
            if (getScrollY() == 0) {
                onTopReached();
            } else if (child.getMeasuredHeight() - CONTENT_LOAD_THRESHOLD_HEIGHT <=
                    getScrollY() + getHeight()) {
                onBottomReached();
            } else {
                onScrolling();
            }
        }
    }

    protected void onVScrollChanged(int t, int oldt) {
    }

    protected void onTopReached() {
    }

    protected void onBottomReached() {
    }

    protected void onScrolling() {
    }

}
