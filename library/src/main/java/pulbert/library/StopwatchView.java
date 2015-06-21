package pulbert.library;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.DecimalFormat;




public class StopwatchView extends RelativeLayout implements SharedPreferences.OnSharedPreferenceChangeListener  {

    private static final String TAG = "CountingTimerView";

    private static final int STOPWATCH_REFRESH_INTERVAL_MILLIS = 25;

    private static final String TWO_DIGITS = "%02d";
    private static final String DD = "0,%02d";
    private static final String ONE_DIGIT = "%01d";
    private static final String NEG_TWO_DIGITS = "-%02d";
    private static final String NEG_ONE_DIGIT = "-%01d";
    private String mHours, mMinutes, mSeconds;
    private String mCompleteSalary;
    private TextView mTimerCounter;
    private TextView mSalaryCounter;
    private Button mButtonStopWatch;
    private Button mButtonStop;
    private GradientDrawable mShape,mShapeV;
    private int mButtonStartColor;
    private int mButtonStopColor;
    private int mButtonPauseColor;
    private int mResumeColor;
    int mState = StopwatchView.STOPWATCH_RESET;
    long mAccumulatedTime = 0;
    long mStartTime = 0;
    private PowerManager.WakeLock mWakeLock;
    private View divider;

    private long mIntervalTime = 0;
    private long mIntervalStartTime = -1;
    private long mMarkerTime = -1;
    private long mCurrentIntervalTime = 0;
    private long mAccumulatedTimeP = 0;
    private boolean mPaused = false;

    public static final String PREF_CTV_PAUSED  = "_ctv_paused";
    public static final String PREF_CTV_INTERVAL  = "_ctv_interval";
    public static final String PREF_CTV_INTERVAL_START = "_ctv_interval_start";
    public static final String PREF_CTV_CURRENT_INTERVAL = "_ctv_current_interval";
    public static final String PREF_CTV_ACCUM_TIME = "_ctv_accum_time";
    public static final String PREF_CTV_TIMER_MODE = "_ctv_timer_mode";
    public static final String PREF_CTV_MARKER_TIME = "_ctv_marker_time";

    public static final String START_STOPWATCH = "start_stopwatch";
    public static final String LAP_STOPWATCH = "lap_stopwatch";
    public static final String STOP_STOPWATCH = "stop_stopwatch";
    public static final String RESET_STOPWATCH = "reset_stopwatch";
    public static final String SHARE_STOPWATCH = "share_stopwatch";
    public static final String RESET_AND_LAUNCH_STOPWATCH = "reset_and_launch_stopwatch";
    public static final String MESSAGE_TIME = "message_time";
    public static final String SHOW_NOTIF = "show_notification";
    public static final String KILL_NOTIF = "kill_notification";
    public static final String PREF_START_TIME  = "sw_start_time";
    public static final String PREF_ACCUM_TIME = "sw_accum_time";
    public static final String PREF_STATE = "sw_state";
    public static final String PREF_LAP_NUM = "sw_lap_num";
    public static final String PREF_LAP_TIME = "sw_lap_time_";
    public static final String PREF_UPDATE_CIRCLE = "sw_update_circle";
    public static final String NOTIF_CLOCK_BASE = "notif_clock_base";
    public static final String NOTIF_CLOCK_ELAPSED = "notif_clock_elapsed";
    public static final String NOTIF_CLOCK_RUNNING = "notif_clock_running";
    public static final String KEY = "sw";

    public static final int STOPWATCH_RESET = 0;
    public static final int STOPWATCH_RUNNING = 1;
    public static final int STOPWATCH_STOPPED = 2;

    public static final int MAX_LAPS = 99;

    public static int refcount = 0;
    public float mSalary;

    private PrimaryButtonListener mCustomOnClickListener;

    private long startTime;


    @SuppressWarnings("unused")
    public StopwatchView(Context context) {
        this(context, null);
    }

    public StopwatchView(Context context, AttributeSet attrs) {
        this(context, attrs,0);

    }
    public StopwatchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle,0);
        mSalary = 15.0f;
        init();
    }

    public interface PrimaryButtonListener {
        void onClick(View v, boolean checked);
    }

    public void setPrimaryButtonListener(PrimaryButtonListener mCustomOnClickListener) {
        this.mCustomOnClickListener = mCustomOnClickListener;
    }

    public void setSalary(float mSalary) {
        this.mSalary = mSalary;
    }

    public float getSalary() {
        return mSalary;
    }

    public void init(){
        refcount++;
        inflate(getContext(), R.layout.stopwatch_item,this);
        Resources r = getContext().getResources();

        mTimerCounter = (TextView) this.findViewById(R.id.timer_counter_textview);
        mSalaryCounter = (TextView) this.findViewById(R.id.salary_counter_textview);
        divider = (View) this.findViewById(R.id.divider_id);


        mButtonStopWatch = (Button) this.findViewById(R.id.button_stopwatch);
        mButtonStop = (Button) this.findViewById(R.id.button_stop);


        //get the Drawable from STOP button
        LayerDrawable mStopDrawable = (LayerDrawable) mButtonStop.getBackground();
        mShapeV = (GradientDrawable)  mStopDrawable.findDrawableByLayerId(R.id.button_drawable_shape);
        mShapeV.setColor(r.getColor(R.color.button_pause));

        //get the Drawable from Start/pause button
        LayerDrawable mDrawableBG = (LayerDrawable) mButtonStopWatch.getBackground();
        mShape = (GradientDrawable)  mDrawableBG.findDrawableByLayerId(R.id.button_drawable_shape);

        mButtonStartColor = r.getColor(R.color.button_start);
        mButtonStopColor = r.getColor(R.color.button_stop);
        mButtonPauseColor = r.getColor(R.color.button_pause);
        mResumeColor = r.getColor(R.color.button_resume);
        mShape.setColor(mButtonStartColor);
        mButtonStopWatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonMainPressed(v);
            }
        });
        mButtonStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPause();
            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(PreferenceManager.getDefaultSharedPreferences(getContext()))) {
            if (! (key.equals(StopwatchView.PREF_LAP_NUM) ||
                    key.startsWith(StopwatchView.PREF_LAP_TIME))) {
                this.readFromSharedPref(prefs);
                if (prefs.getBoolean(StopwatchView.PREF_UPDATE_CIRCLE, true)) {
                    this.readFromSharedPref(prefs, "sw");
                }
            }
        }
    }

    public void onResume(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        this.readFromSharedPref(prefs);
        this.readFromSharedPref(prefs, "sw");
        this.postInvalidate();
        this.reDraw();

        if (mState == StopwatchView.STOPWATCH_RUNNING) {
            this.setRunningState();
            Log.e("Stopwatch", "setRunningState()");
        } else if (mState == StopwatchView.STOPWATCH_STOPPED && mAccumulatedTime != 0) {
            this.setResumeState();
            Log.e("Stopwatch", "setResumeState()");
        } else {
            this.setStartState();
            Log.e("Stopwatch", "setStartState()");
        }

    }

    public void onPause(){
        if (mState == StopwatchView.STOPWATCH_RUNNING) {
            this.stopUpdateThread();

            // This is called because the lock screen was activated, the window stay
            // active under it and when we unlock the screen, we see the old time for
            // a fraction of a second.

        }
        // The stopwatch must keep running even if the user closes the app so save stopwatch state
        // in shared prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        writeToSharedPref(prefs);
        writeToSharedPref(prefs, "sw");
        releaseWakeLock();
    }

    public void buttonPause(){
        long time = Utils.getTimeNow();
        Context context = getContext().getApplicationContext();
        Intent intent = new Intent(context, StopwatchService.class);
        intent.putExtra(StopwatchView.MESSAGE_TIME, time);
        intent.putExtra(StopwatchView.SHOW_NOTIF, false);

        switch(mState){
            //stop Time
            case StopwatchView.STOPWATCH_RUNNING:
                mShapeV.setColor(mResumeColor);
                mButtonStop.setText(getContext().getResources().getString(R.string.stopwatch_resume));
                //save actual Color
                startTime = Utils.getTimeNow();
                mAccumulatedTime += (startTime - mStartTime);
                doStop();
                intent.setAction(StopwatchView.STOP_STOPWATCH);
                context.startService(intent);
                releaseWakeLock();
                break;

            //pause Time
            case StopwatchView.STOPWATCH_STOPPED:
                mShapeV.setColor(mButtonPauseColor);
                mButtonStop.setText(getContext().getResources().getString(R.string.stopwatch_pause));
                doStart(time);
                intent.setAction(StopwatchView.START_STOPWATCH);
                context.startService(intent);
                acquireWakeLock();
                break;

        }
    }

    public void buttonMainPressed(View v){
        long time = Utils.getTimeNow();
        Context context = getContext().getApplicationContext();
        Intent intent = new Intent(context, StopwatchService.class);
        intent.putExtra(StopwatchView.MESSAGE_TIME, time);
        intent.putExtra(StopwatchView.SHOW_NOTIF, false);

        switch (mState){
            case StopwatchView.STOPWATCH_RESET:
                // do start
                doStart(time);
                intent.setAction(StopwatchView.START_STOPWATCH);
                context.startService(intent);
                acquireWakeLock();
                mButtonStopWatch.setText(context.getResources().getString(R.string.stopwatch_stop));
                mShape.setColor(mButtonStopColor);
                setViewsVisible(true);
                if(mCustomOnClickListener != null) {
                    mCustomOnClickListener.onClick(v,true);
                }
                // Invoke the other added onclick listener
                break;
            default:
                doResetAndStop();
                setViewsVisible(false);
                mTimerCounter.setText(getContext().getResources().getString(R.string.default_textview_timer_content));
                mShapeV.setColor(mButtonPauseColor);
                mButtonStop.setText(getContext().getResources().getString(R.string.stopwatch_pause));
                mButtonStopWatch.setText(context.getResources().getString(R.string.stopwatch_start));
                mShape.setColor(mButtonStartColor);
                if(mCustomOnClickListener != null) {
                    mCustomOnClickListener.onClick(v,false);
                }
                releaseWakeLock();
                break;


        }


    }

    private void setViewsVisible(boolean visible){
        if(visible) {
            divider.setVisibility(VISIBLE);
            mTimerCounter.setVisibility(VISIBLE);
            mButtonStop.setVisibility(VISIBLE);
            mSalaryCounter.setVisibility(VISIBLE);
        }else{
            divider.setVisibility(INVISIBLE);
            mSalaryCounter.setVisibility(INVISIBLE);
            mTimerCounter.setVisibility(VISIBLE);
            mButtonStop.setVisibility(INVISIBLE);
        }
    }


    private void doReset() {
        //   if (DEBUG) LogUtils.v("StopwatchFragment.doReset");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        Utils.clearSwSharedPref(prefs);
        clearSharedPref(prefs, "sw");
        mAccumulatedTime = 0;
        updateTextViews(mAccumulatedTime, false, true);
        mState = StopwatchView.STOPWATCH_RESET;

    }

    public void writeToSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong (StopwatchView.PREF_START_TIME, mStartTime);
        editor.putLong (StopwatchView.PREF_ACCUM_TIME, mAccumulatedTime);
        editor.putInt (StopwatchView.PREF_STATE, mState);

        if (mState == StopwatchView.STOPWATCH_RUNNING) {
            editor.putLong(StopwatchView.NOTIF_CLOCK_BASE, mStartTime-mAccumulatedTime);
            editor.putLong(StopwatchView.NOTIF_CLOCK_ELAPSED, -1);
            editor.putBoolean(StopwatchView.NOTIF_CLOCK_RUNNING, true);
        } else if (mState == StopwatchView.STOPWATCH_STOPPED) {
            editor.putLong(StopwatchView.NOTIF_CLOCK_ELAPSED, mAccumulatedTime);
            editor.putLong(StopwatchView.NOTIF_CLOCK_BASE, -1);
            editor.putBoolean(StopwatchView.NOTIF_CLOCK_RUNNING, false);
        } else if (mState == StopwatchView.STOPWATCH_RESET) {
            editor.remove(StopwatchView.NOTIF_CLOCK_BASE);
            editor.remove(StopwatchView.NOTIF_CLOCK_RUNNING);
            editor.remove(StopwatchView.NOTIF_CLOCK_ELAPSED);
        }
        editor.putBoolean(StopwatchView.PREF_UPDATE_CIRCLE, false);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs) {
        mStartTime = prefs.getLong(StopwatchView.PREF_START_TIME, 0);
        mAccumulatedTime = prefs.getLong(StopwatchView.PREF_ACCUM_TIME, 0);
        mState = prefs.getInt(StopwatchView.PREF_STATE, StopwatchView.STOPWATCH_RESET);

        //     if (prefs.getBoolean(StopwatchView.PREF_UPDATE_CIRCLE, true)) {
        if (mState == StopwatchView.STOPWATCH_STOPPED) {
            doStop();
        } else if (mState == StopwatchView.STOPWATCH_RUNNING) {
            doStart(mStartTime);
        } else if (mState == StopwatchView.STOPWATCH_RESET) {
            doReset();
        }
        //   }
    }

    public void reDraw(){
        this.postInvalidate();
        updateTextViews(mAccumulatedTime, false, true);
    }

    public void setRunningState(){
        acquireWakeLock();
        startUpdateThread();
        mShapeV.setColor(mButtonPauseColor);
        mButtonStop.setText(getContext().getResources().getString(R.string.stopwatch_pause));
        mShape.setColor(mButtonStopColor);
        mButtonStopWatch.setText(getContext().getResources().getString(R.string.stopwatch_stop));
        mButtonStop.setVisibility(VISIBLE);
        mSalaryCounter.setVisibility(VISIBLE);
    }
    public void setResumeState(){
        mShape.setColor(mButtonStopColor);
        mShapeV.setColor(mResumeColor);
        mButtonStop.setText(getContext().getResources().getString(R.string.stopwatch_resume));
        mButtonStop.setVisibility(VISIBLE);
        mSalaryCounter.setVisibility(VISIBLE);
        mButtonStopWatch.setText(getContext().getResources().getString(R.string.stopwatch_stop));

    }
    public void setStartState(){
        mTimerCounter.setText(getContext().getResources().getString(R.string.default_textview_timer_content));
        mShape.setColor(mButtonStartColor);

    }




    public long getAccumulatedTime() {
        return mAccumulatedTime;
    }

    public void doResetAndStop(){
        stopUpdateThread();
        //   if (DEBUG) LogUtils.v("StopwatchFragment.doReset");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        Utils.clearSwSharedPref(prefs);
        clearSharedPref(prefs, "sw");
        mAccumulatedTime = 0;
        mTimerCounter.setText(getContext().getResources().getString(R.string.default_textview_timer_content));
        mState = StopwatchView.STOPWATCH_RESET;

    }



    private void doStop() {
        stopUpdateThread();
        updateTextViews(mAccumulatedTime, false, true);
        mState = StopwatchView.STOPWATCH_STOPPED;
    }

    private void doStart(long time) {
        mStartTime = time;
        startUpdateThread();
        mState = StopwatchView.STOPWATCH_RUNNING;
    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            long curTime = Utils.getTimeNow();
            long totalTime = mAccumulatedTime + (curTime - mStartTime);
            updateTextViews(totalTime, false, true);

            StopwatchView.this.postDelayed(mTimeUpdateThread, STOPWATCH_REFRESH_INTERVAL_MILLIS);
        }
    };

    public void startUpdateThread() { this.post(mTimeUpdateThread);}

    public void stopUpdateThread() { this.removeCallbacks(mTimeUpdateThread);}


    // Used to keeps screen on when stopwatch is running.

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            final PowerManager pm =
                    (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }



    /**
     * Update the time to display. Separates that time into the hours, minutes, seconds and
     * hundredths. If update is true, the view is invalidated so that it will draw again.
     *
     * @param time new time to display - in milliseconds
     * @param showHundredths flag to show hundredths resolution
     * @param update to invalidate the view - otherwise the time is examined to see if it is within
     *               100 milliseconds of zero seconds and when so, invalidate the view.
     */
    // TODO:showHundredths S/B attribute or setter - i.e. unchanging over object life
    public void updateTextViews(long time, boolean showHundredths, boolean update) {

        boolean neg = false, showNeg = false;
        String format;
        if (time < 0) {
            time = -time;
            neg = showNeg = true;
        }
        long hundreds, seconds, minutes, hours;
        seconds = time / 1000;
        hundreds = (time - seconds * 1000) / 10;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }

        float hourInSeconds = (hours * 60)*60;
        float minutesInSeconds = (minutes * 60);
        float allSeconds = hourInSeconds + minutesInSeconds +seconds;
        float salaryPerMinute = mSalary /60;
        float salaryPerSeconds = salaryPerMinute /60;
        float mSalary = allSeconds * salaryPerSeconds;

        //save complete Salary in String
        mCompleteSalary = new DecimalFormat("0.00").format(mSalary);
        mCompleteSalary += " €";


        // The time  can be between 0 and -1 seconds, but the "truncated" equivalent time of hours
        // and minutes and seconds could be zero, so since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        // TODO:does it matter that we do not look at showHundredths?
        if (hours == 0 && minutes == 0 && seconds == 0) {
            showNeg = false;
        }

        // Normalize and check if it is 'time' to invalidate
        if (!showHundredths) {
            if (!neg && hundreds != 0) {
                seconds++;
                if (seconds == 60) {
                    seconds = 0;
                    minutes++;
                    if (minutes == 60) {
                        minutes = 0;
                        hours++;
                    }
                }
            }
            if (hundreds < 10 || hundreds > 90) {
                update = true;
            }
        }

        // Hours may be empty
        if (hours >= 10) {
            format = showNeg ? NEG_TWO_DIGITS : TWO_DIGITS;
            mHours = String.format(format, hours);
        } else if (hours > 0) {
            format = showNeg ? NEG_ONE_DIGIT : ONE_DIGIT;
            mHours = String.format(format, hours);
        } else {
            mHours = null;
        }

        // Minutes are never empty and when hours are non-empty, must be two digits
        if (minutes >= 10 || hours > 0) {
            format = (showNeg && hours == 0) ? NEG_TWO_DIGITS : TWO_DIGITS;
            mMinutes = String.format(format, minutes);
        } else {
            format = (showNeg && hours == 0) ? NEG_ONE_DIGIT : ONE_DIGIT;
            mMinutes = String.format(format, minutes);
        }

        // Seconds are always two digits
        mSeconds = String.format(TWO_DIGITS, seconds);

        if (update) {
            setContentDescription(getTimeStringForAccessibility((int) hours, (int) minutes,
                    (int) seconds, showNeg, getResources()));
            invalidate();
        }

        initTextViews();
    }

    public void initTextViews(){
        mTimerCounter.setText(getTimeString());
        mSalaryCounter.setText(mCompleteSalary);

    }

    public String getTimeString() {
        if (mHours == null) {
            return String.format("%s:%s", mMinutes, mSeconds);
        }else {
            return String.format("%s:%s:%s", mHours, mMinutes, mSeconds);
        }
    }

    private static String getTimeStringForAccessibility(int hours, int minutes, int seconds,
                                                        boolean showNeg, Resources r) {
        StringBuilder s = new StringBuilder();
        if (showNeg) {
            // This must be followed by a non-zero number or it will be audible as "hyphen"
            // instead of "minus".
            s.append("-");
        }
        if (showNeg && hours == 0 && minutes == 0) {
            // Non-negative time will always have minutes, eg. "0 minutes 7 seconds", but negative
            // time must start with non-zero digit, eg. -0m7s will be audible as just "-7 seconds"
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        } else if (hours == 0) {
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(),
                    minutes));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        } else {
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nhours_description, hours).toString(),
                    hours));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(),
                    minutes));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        }
        return s.toString();
    }


    public int getStopWatchState() {
        return mState;
    }


    // Since this view is used in multiple places, use the key to save different instances
    public void writeToSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean (key + PREF_CTV_PAUSED, mPaused);
        editor.putLong (key + PREF_CTV_INTERVAL, mIntervalTime);
        editor.putLong (key + PREF_CTV_INTERVAL_START, mIntervalStartTime);
        editor.putLong (key + PREF_CTV_CURRENT_INTERVAL, mCurrentIntervalTime);
        editor.putLong (key + PREF_CTV_ACCUM_TIME, mAccumulatedTimeP);
        editor.putLong (key + PREF_CTV_MARKER_TIME, mMarkerTime);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs, String key) {
        mPaused = prefs.getBoolean(key + PREF_CTV_PAUSED, false);
        mIntervalTime = prefs.getLong(key + PREF_CTV_INTERVAL, 0);
        mIntervalStartTime = prefs.getLong(key + PREF_CTV_INTERVAL_START, -1);
        mCurrentIntervalTime = prefs.getLong(key + PREF_CTV_CURRENT_INTERVAL, 0);
        mAccumulatedTimeP = prefs.getLong(key + PREF_CTV_ACCUM_TIME, 0);
        mMarkerTime = prefs.getLong(key + PREF_CTV_MARKER_TIME, -1);
    }

    public void clearSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (StopwatchView.PREF_START_TIME);
        editor.remove (StopwatchView.PREF_ACCUM_TIME);
        editor.remove (StopwatchView.PREF_STATE);
        editor.remove (key + PREF_CTV_PAUSED);
        editor.remove (key + PREF_CTV_INTERVAL);
        editor.remove (key + PREF_CTV_INTERVAL_START);
        editor.remove (key + PREF_CTV_CURRENT_INTERVAL);
        editor.remove (key + PREF_CTV_ACCUM_TIME);
        editor.remove (key + PREF_CTV_MARKER_TIME);
        editor.remove (key + PREF_CTV_TIMER_MODE);
        editor.apply();
    }


    public long getStartTime() {
        return startTime;
    }
}

