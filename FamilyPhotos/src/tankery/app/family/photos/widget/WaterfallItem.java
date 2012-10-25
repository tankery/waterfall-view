package tankery.app.family.photos.widget;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import tankery.app.family.photos.data.PhotoStorage;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class WaterfallItem extends ImageView {
    
    static final String tag = "WaterfallItem";

    private int photoId = 0;

    @Override
    public String toString() {
        return String.valueOf(photoId);
    }

    public WaterfallItem(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public WaterfallItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public WaterfallItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public boolean setImageBitmap(int id) {
        photoId = id;
        // get photo from photo table by id.
        Bitmap bmp = PhotoStorage.getInstance().getPhoto(id);
        if (bmp == null) {
            Log.e(tag, "Bitmap [" + id + "] is null when addPhoto.");
            return false;
        }
        setImageBitmap(bmp);

        return true;
    }

    public void reload() {
        if (photoId == 0)
            return;

        Bitmap bmp = PhotoStorage.getInstance().getPhoto(photoId);
        if (bmp == null || bmp.isRecycled()) {
            String name = String.valueOf(photoId);
            try {
                FileInputStream fis =
                        getContext().getApplicationContext()
                                    .openFileInput(name);
                final Bitmap newBmp = BitmapFactory.decodeStream(fis);
                if (newBmp == null || newBmp.getWidth() <= 0 ||
                    newBmp.getHeight() <= 0) {
                    Log.e(tag, "Bitmap is null decode from " + name);
                    return;
                }
                PhotoStorage.getInstance().setPhoto(photoId, newBmp);

                WaterfallItemColumn column = (WaterfallItemColumn) getParent();
                if (column != null)
                    column.setPhoto(this, photoId);

                // if parent is null, means this view is remove from the parent,
                // do nothing.
            } catch (FileNotFoundException e) {
                Log.e(tag, e.getMessage());
                return;
            }
        }
    }

    public void recycle() {
        // set the bitmap in PhotoStorage will cause the old one
        // recycle.
        PhotoStorage.getInstance().setPhoto(photoId, null);

        ((WaterfallItemColumn) getParent()).setPhoto(this, photoId);
    }

}
