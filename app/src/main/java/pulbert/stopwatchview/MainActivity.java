package pulbert.stopwatchview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
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
                    Toast.makeText(MainActivity.this, "start", Toast.LENGTH_SHORT).show();
                    toastStart();
                } else {
                    Toast.makeText(MainActivity.this, "stop", Toast.LENGTH_SHORT).show();
                    toastEnd();
                }

        }
    });
        mStopwatch.setSalary(32.0f);

    }

    public void toastStart(){
        Toast.makeText(this,mStopwatch.getStartHour() + ":"+ mStopwatch.getStartMinute(),Toast.LENGTH_SHORT).show();
    }
    public void toastEnd(){
        Toast.makeText(this,mStopwatch.getStartHour() + ":"+ mStopwatch.getStartMinute(),Toast.LENGTH_SHORT).show();
    }

    public String getFullHourString(){

        DateTime testStart = new DateTime().withDayOfMonth(1).withTime((int)mStopwatch.getStartHour(),(int) mStopwatch.getStartMinute(), 0, 0);
        DateTime testEnd = new DateTime().withDayOfMonth(2).withTime((int)mStopwatch.getFinishHour(), (int) mStopwatch.getFinishMinute(), 0, 0);
        Period test = new Period(testStart , testEnd);
        int h = test.getHours();
        int m = test.getMinutes();
        String f = String.format("%%0%dd", 2);
        String temp =DateTimeFormat.forPattern("HH:mm").print(testStart)+ " - "+
                DateTimeFormat.forPattern("HH:mm").print(testEnd)+ " | "+
                Long.toString(h)+":"+String.format(f, m);
        return temp;
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
