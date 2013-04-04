package tankery.app.family.photos;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class HomeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FamilyPhotosView view = (FamilyPhotosView) findViewById(R.id.family_photos_view);
        view.init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }
}
