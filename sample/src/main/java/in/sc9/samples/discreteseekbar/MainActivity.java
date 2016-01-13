package in.sc9.samples.discreteseekbar;

import android.app.Activity;
import android.os.Bundle;

import in.sc9.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class MainActivity extends Activity implements DiscreteSeekBar.OnProgressChangeListener {
    DiscreteSeekBar dsbTemp;
    String ARRAY[] = {"Cold", "20", "30", "40", "60", "95"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dsbTemp = (DiscreteSeekBar) findViewById(R.id.dsbTemp);
        dsbTemp.setOnProgressChangeListener(this);

        DiscreteSeekBar discreteSeekBar1 = (DiscreteSeekBar) findViewById(R.id.discrete1);
        discreteSeekBar1.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                return value * 100;
            }
        });

        dsbTemp.setTextValues(ARRAY);
        dsbTemp.setProgress(3);
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

    }
}
