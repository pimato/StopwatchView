package pulbert.stopwatchview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;

import pulbert.library.StopwatchService;
import pulbert.library.StopwatchView;

public class MainActivity extends AppCompatActivity {

    private StopwatchView mStopwatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);
        mStopwatch =(StopwatchView) findViewById(R.id.stopwatch_view);
        mStopwatch.setPrimaryButtonListener(new StopwatchView.PrimaryButtonListener() {
            @Override
            public void onClick(View v, boolean checked) {
                if (checked) {
                    toastStart();
                } else {
                    toastEnd();
                }

        }
    });
        mStopwatch.setWage(32.0f);

    }

    public void toastStart(){
        Toast.makeText(this,"start",Toast.LENGTH_SHORT).show();
    }
    public void toastEnd(){
        Toast.makeText(this,"Time"+mStopwatch.getSeconds(), Toast.LENGTH_SHORT).show();

      //  Toast.makeText(this,"Jahr :"+mStopwatch.getYear()+"Monat :"+mStopwatch.getMonth()+"Tag :"+mStopwatch.getDay(),Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent intent = new Intent(this, StopwatchService.class);
        intent.setAction(StopwatchView.SHOW_NOTIF);
        startService(intent);
        mStopwatch.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We only want to show notifications for stopwatch/timer when the app is closed so
        // that we don't have to worry about keeping the notifications in perfect sync with
        // the app.
        Intent stopwatchIntent = new Intent(this, StopwatchService.class);
        stopwatchIntent.setAction(StopwatchView.KILL_NOTIF);
        startService(stopwatchIntent);
        mStopwatch.onResume();

    }

}
