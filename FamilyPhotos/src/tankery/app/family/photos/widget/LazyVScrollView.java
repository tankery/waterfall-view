package tankery.app.family.photos.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
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

    private static final String tag = "LazyVScrollView";
    
    private static final int SCROLLING_MESSAGE_DELAY = 200;
    private static final int CONTENT_LOAD_THRESHOLD_HEIGHT = 20;

    /**
     * listener interface who will be called when scroll changed.
     */
    public interface OnScrollListener {
        void onTopReached();
        void onBottomReached();
        void onScrolling();

        void onScrollChanged(int t, int oldt);
    }

    private OnScrollListener onScrollListener = null;
    private View child = null;

    private OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                onTouchFinished();
                break;

            default:
                break;
            }
            return false;
        }

    };

    public LazyVScrollView(Context context) {
        super(context);
    }

    public LazyVScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LazyVScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean init(View child) {
        if (child != null) {
            this.addView(child);
        }
        this.child = getChildAt(0);

        if (this.child != null) {
            setOnTouchListener(onTouchListener);
            return true;
        }

        return false;
    }
    
    protected View getChild() {
        return this.child;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollListener != null) {
            onScrollListener.onScrollChanged(t, oldt);
        }
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (child.getMeasuredHeight() - CONTENT_LOAD_THRESHOLD_HEIGHT <= 
                    getScrollY() + getHeight()) {
                onScrollListener.onBottomReached();
            } else if (getScrollY() == 0) {
                onScrollListener.onTopReached();
            } else {
                onScrollListener.onScrolling();
            }
        }
    };

    private void onTouchFinished() {
        if (onScrollListener == null) {
            Log.w(tag, "onScrollListener is null when on slide up.");
            return;
        }
        if (child != null && onScrollListener != null) {
            handler.sendMessageDelayed(handler.obtainMessage(),
                    SCROLLING_MESSAGE_DELAY);
        }
    }

}
