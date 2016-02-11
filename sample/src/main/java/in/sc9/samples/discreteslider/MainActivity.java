package in.sc9.samples.discreteslider;

import android.app.Activity;
import android.os.Bundle;

import in.sc9.discreteslider.DiscreteSlider;

public class MainActivity extends Activity implements DiscreteSlider.OnProgressChangeListener {
    DiscreteSlider dsbTemp;
    String ARRAY[] = {"Cold", "20", "30", "40", "60", "95"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dsbTemp = (DiscreteSlider) findViewById(R.id.dsbTemp);
        dsbTemp.setOnProgressChangeListener(this);

        DiscreteSlider discreteSlider1 = (DiscreteSlider) findViewById(R.id.discrete1);
        discreteSlider1.setNumericTransformer(new DiscreteSlider.NumericTransformer() {
            @Override
            public int transform(int value) {
                return value * 100;
            }
        });

        dsbTemp.setTextValues(ARRAY);
        dsbTemp.setProgress(3);
    }

    @Override
    public void onProgressChanged(DiscreteSlider seekBar, int value, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(DiscreteSlider seekBar) {

    }

    @Override
    public void onStopTrackingTouch(DiscreteSlider seekBar) {

    }
}
