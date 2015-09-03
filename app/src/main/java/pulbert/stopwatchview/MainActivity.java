package pulbert.stopwatchview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import pulbert.library.ButtonListener;
import pulbert.library.StopwatchView;

public class MainActivity extends AppCompatActivity {

    private StopwatchView mStopwatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStopwatch =(StopwatchView) findViewById(R.id.stopwatch_view);
        mStopwatch.setSecondaryButtonListener(new ButtonListener() {
            @Override
            public void onClick(View v, boolean checked) {
                Toast.makeText(MainActivity.this, mStopwatch.getDateString(), Toast.LENGTH_SHORT).show();
            }
        });
        mStopwatch.setPrimaryButtonListener(new ButtonListener() {
            @Override
            public void onClick(View v, boolean checked) {
                if (checked) {
                    Toast.makeText(MainActivity.this, "start", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,"GetMinute :"+mStopwatch.getFinishHour(),Toast.LENGTH_SHORT).show();
                }

            }
        });
        mStopwatch.setWage(32.0f);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mStopwatch.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStopwatch.onResume(this);

    }

}
