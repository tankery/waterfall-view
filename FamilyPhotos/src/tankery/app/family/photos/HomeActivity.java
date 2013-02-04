package tankery.app.family.photos;

import tankery.app.family.photos.widget.WaterfallView;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class HomeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WaterfallView view = (WaterfallView) findViewById(R.id.home_waterfall_view);
        view.init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }
}
