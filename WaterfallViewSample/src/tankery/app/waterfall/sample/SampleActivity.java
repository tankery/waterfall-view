package tankery.app.waterfall.sample;

import android.app.Activity;
import android.os.Bundle;

public class SampleActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        SampleView view = (SampleView) findViewById(R.id.sample_view);
        view.init(getAssets());
    }

}
