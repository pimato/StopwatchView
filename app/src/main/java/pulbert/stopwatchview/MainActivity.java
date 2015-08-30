package pulbert.stopwatchview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import pulbert.library.ButtonListener;
import pulbert.library.StopwatchService;
import pulbert.library.StopwatchView;

public class MainActivity extends AppCompatActivity {

    private StopwatchView mStopwatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStopwatch =(StopwatchView) findViewById(R.id.stopwatch_view);
        mStopwatch.setWage(32.0f);

    }

    public void changePlayPauseButton(View view){
        mStopwatch.changeButtonWithoutAnimation();
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
