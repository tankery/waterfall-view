/**
 * 
 */
package tankery.app.family.photos.widget;

import java.util.ArrayList;

import tankery.app.family.photos.data.PhotoStorage;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * @author tankery
 *
 */
public class WaterfallItemColumn extends LinearLayout {
    
    static final String tag = "WaterfallItemColumn";
    
    private ArrayList<Integer> photoIdList = new ArrayList<Integer>();
    
    private int columnWidth = 0;

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

            ImageView view = new ImageView(getContext());
            addView(view);
            view.setPadding(0, 2, 0, 2);

            Bitmap bmp = PhotoStorage.getInstance().getPhoto(id);
            if (bmp == null) {
                Log.e(tag, "Bitmap is null when addPhoto.");
                return;
            }
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int layoutHeight = (height * columnWidth) / width;
            LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, layoutHeight);
            view.setLayoutParams(itemParam);
            view.setImageBitmap(bmp);
        }
    };

    public void addPhoto(int id) {
        // get photo from photo table by id, pass it with msg.what,
        // then, create item view and add it from UI thread.
        photoIdList.add(id);
        handler.obtainMessage(id).sendToTarget();
    }

}
