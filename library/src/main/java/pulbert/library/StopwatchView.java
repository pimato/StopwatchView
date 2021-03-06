package pulbert.library;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import pulbert.library.playdrawable.PlayPauseView;


public class StopwatchView extends FrameLayout implements SharedPreferences.OnSharedPreferenceChangeListener  {

    private static final String TAG = "CountingTimerView";

    private static final int STOPWATCH_REFRESH_INTERVAL_MILLIS = 25;

    private static final String TWO_DIGITS = "%02d";
    private static final String DD = "0,%02d";
    private static final String ONE_DIGIT = "%01d";
    private static final String NEG_TWO_DIGITS = "-%02d";
    private static final String NEG_ONE_DIGIT = "-%01d";
    int mState = StopwatchView.STOPWATCH_RESET;
    long mAccumulatedTime = 0;
    long mStartTime = 0;
    private PowerManager.WakeLock mWakeLock;

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
    public static final String STOP_STOPWATCH = "stop_stopwatch";
    public static final String RESET_STOPWATCH = "reset_stopwatch";
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

    public static final String PREF_YEAR = "sw_year";
    public static final String PREF_MONTH = "sw_month";
    public static final String PREF_DAY = "sw_day";

    public static final String PREF_START_MINUTE = "sw_start_minute";
    public static final String PREF_START_HOUR = "sw_start_hour";

    private int mYear,mMonth,mDay;
    private int sHour,sMinute;
    private int fHour,fMinute;


    public static int refcount = 0;
    public float mWage;


    private RelativeLayout mIdleLayout;

    private String mHours;
    private String mMinutes;
    private String mSeconds;
    private String mCompleteSalary;
    private TextView mRunningTextView;
    private TextView mRunningSalaryTextView;
    private TextView mDefaultTextView;

    private PlayPauseView mPlayPauseButton;
    private ImageButton mStopButton;

    private RelativeLayout textViewsLayout;

    public StopwatchView(Context context) {
        this(context, null);
    }
    public StopwatchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }
    public StopwatchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle,0);
        mWage = 15.0f;
        init();
    }

    public void init(){
        refcount++;
        inflate(getContext(), R.layout.stopwatch_item_main, this);

        mIdleLayout = (RelativeLayout) this.findViewById(R.id.stopwatch_mainlayout);
        if(mIdleLayout != null){
            setupIdleLayout();
        }
    }

    public void setupIdleLayout(){

        mRunningTextView = (TextView) findViewById(R.id.stopwatch_running_textview);
        mDefaultTextView = (TextView) findViewById(R.id.stopwatch_default_textview);

        mRunningSalaryTextView = (TextView) findViewById(R.id.stopwatch_running_salary_textview);
        textViewsLayout = (RelativeLayout) findViewById(R.id.textviews_layout);

        mPlayPauseButton = (PlayPauseView) findViewById(R.id.play_pause_view);
        mStopButton = (ImageButton) findViewById(R.id.stopwatch_stop_button);
        mStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFinishTime();
                doResetAndStop();
                releaseWakeLock();
                mPlayPauseButton.finishAnimation(mStopButton);
                if (mPlayPauseButton.getPlayPauseDrawable().isPlay()) {
                    mPlayPauseButton.togglePlayPauseAnimation();
                }
                textViewsLayout.setVisibility(View.GONE);
                mDefaultTextView.setVisibility(VISIBLE);

            }
        });

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStopButton.getVisibility() == INVISIBLE) {
                    mPlayPauseButton.toggleStopButtonAnimation(mStopButton);
                }
                mPlayPauseButton.togglePlayPauseAnimation();
                long time = Utils.getTimeNow();
                Context context = getContext().getApplicationContext();
                Intent intent = new Intent(context, StopwatchService.class);
                intent.putExtra(StopwatchView.MESSAGE_TIME, time);
                intent.putExtra(StopwatchView.SHOW_NOTIF, false);
                if (mPlayPauseButton.getPlayPauseDrawable().isPlay()) {
                    long curTime = Utils.getTimeNow();
                    mAccumulatedTime += (curTime - mStartTime);
                    doStop();
                    intent.setAction(StopwatchView.STOP_STOPWATCH);
                    context.startService(intent);
                    releaseWakeLock();
                } else {
                    doStart(time);
                    intent.setAction(StopwatchView.START_STOPWATCH);
                    context.startService(intent);
                    acquireWakeLock();
                }
                mDefaultTextView.setVisibility(View.GONE);
                textViewsLayout.setVisibility(VISIBLE);

            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    }

    public void onResume(Context context){

        // We only want to show notifications for stopwatch/timer when the app is closed so
        // that we don't have to worry about keeping the notifications in perfect sync with
        // the app.
        Intent stopwatchIntent = new Intent(context, StopwatchService.class);
        stopwatchIntent.setAction(StopwatchView.KILL_NOTIF);
        context.startService(stopwatchIntent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        this.readFromSharedPref(prefs);
        this.readFromSharedPref(prefs, "sw");
        this.postInvalidate();
        this.reDraw();

        if (mState == StopwatchView.STOPWATCH_RUNNING) {
            acquireWakeLock();
            startUpdateThread();
            mDefaultTextView.setVisibility(View.GONE);
            textViewsLayout.setVisibility(VISIBLE);
            mStopButton.setVisibility(VISIBLE);
            Log.e("Stopwatch", "recoverRunningState()");
        } else if (mState == StopwatchView.STOPWATCH_STOPPED && mAccumulatedTime != 0) {
            mDefaultTextView.setVisibility(View.GONE);
            textViewsLayout.setVisibility(VISIBLE);
            mStopButton.setVisibility(VISIBLE);

            if(mPlayPauseButton.getPlayPauseDrawable().isPlay()) {
                AnimatorSet set = new AnimatorSet();
                Animator anim = mPlayPauseButton.getPlayPauseDrawable().getPausePlayAnimator();
                anim.setDuration(1);
                set.play(anim);
                set.start();
            }
            Log.e("Stopwatch", "recoverResumeState()");
        } else {
            if(mPlayPauseButton.getPlayPauseDrawable().isPlay()) {
                Log.e("Stopwatch", "setStartState()");
                AnimatorSet set = new AnimatorSet();
                Animator anim = mPlayPauseButton.getPlayPauseDrawable().getPausePlayAnimator();
                anim.setDuration(1);
                set.play(anim);
                set.start();
            }
        }

    }


    public void onPause(Context context){

        Intent intent = new Intent(context, StopwatchService.class);
        intent.setAction(StopwatchView.SHOW_NOTIF);
        context.startService(intent);

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



    public void saveFinishTime(){
        Calendar c= Calendar.getInstance();
        fHour = c.get(Calendar.HOUR_OF_DAY);
        fMinute = c.get(Calendar.MINUTE);
    }

    public void saveDateTimeInformation(){
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        sMinute = c.get(Calendar.MINUTE);
        sHour = c.get(Calendar.HOUR_OF_DAY);
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

        editor.putInt(StopwatchView.PREF_START_HOUR,sHour);
        editor.putInt(StopwatchView.PREF_START_MINUTE,sMinute);

        editor.putInt(StopwatchView.PREF_YEAR,mYear);
        editor.putInt(StopwatchView.PREF_MONTH,mMonth);
        editor.putInt(StopwatchView.PREF_DAY,mDay);

        editor.putLong(StopwatchView.PREF_START_TIME, mStartTime);
        editor.putLong(StopwatchView.PREF_ACCUM_TIME, mAccumulatedTime);
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

    public void  readFromSharedPref(SharedPreferences prefs) {

        sHour = prefs.getInt(StopwatchView.PREF_START_HOUR,0);
        sMinute = prefs.getInt(StopwatchView.PREF_START_MINUTE,0);

        mYear = prefs.getInt(StopwatchView.PREF_YEAR,0);
        mMonth = prefs.getInt(StopwatchView.PREF_MONTH,0);
        mDay = prefs.getInt(StopwatchView.PREF_DAY,0);

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

    public int getYear(){return mYear;}
    public int getMonth(){return mMonth;}
    public int getDay(){return mDay;}

    public void reDraw(){
        this.postInvalidate();
        updateTextViews(mAccumulatedTime, false, true);
    }


    public void doResetAndStop(){
        stopUpdateThread();
        //   if (DEBUG) LogUtils.v("StopwatchFragment.doReset");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        Utils.clearSwSharedPref(prefs);
        clearSharedPref(prefs, "sw");
        mAccumulatedTime = 0;
        mRunningTextView.setText("0:00");
        mRunningSalaryTextView.setText("0,00 €");
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
            Log.i("StopwatchView", "Time" + totalTime);
            StopwatchView.this.postDelayed(mTimeUpdateThread, STOPWATCH_REFRESH_INTERVAL_MILLIS);
        }
    };

    public void startUpdateThread() { this.post(mTimeUpdateThread);}

    public void stopUpdateThread() { this.removeCallbacks(mTimeUpdateThread);}


    /**
     * Update the time to display. Separates that time into the hours, minutes, seconds and
     * hundredths. If update is true, the mPlayPauseButton is invalidated so that it will draw again.
     *
     * @param time new time to display - in milliseconds
     * @param showHundredths flag to show hundredths resolution
     * @param update to invalidate the mPlayPauseButton - otherwise the time is examined to see if it is within
     *               100 milliseconds of zero seconds and when so, invalidate the mPlayPauseButton.
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
        float salaryPerMinute = mWage /60;
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

    public String getDateString(){
        Calendar c = Calendar.getInstance();
        c.set(mYear,mMonth,mDay);
        return new SimpleDateFormat("dd. MMM yyyy").format(c.getTime());
    }

    public void initTextViews(){
          mRunningTextView.setText(getTimeString());
          mRunningSalaryTextView.setText(mCompleteSalary);

    }

    public String getTimeString() {
        if (mHours == null) {
            return String.format("%s:%s", mMinutes, mSeconds);
        }else {
            return String.format("%s:%s:%s", mHours, mMinutes, mSeconds);
        }
    }

    private static String getTimeStringForAccessibility(int hours, int minutes, int seconds, boolean showNeg, Resources r) {
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

    // Since this mPlayPauseButton is used in multiple places, use the key to save different instances
    public void writeToSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean (key + PREF_CTV_PAUSED, mPaused);
        editor.putLong (key + PREF_CTV_INTERVAL, mIntervalTime);
        editor.putLong (key + PREF_CTV_INTERVAL_START, mIntervalStartTime);
        editor.putLong (key + PREF_CTV_CURRENT_INTERVAL, mCurrentIntervalTime);
        editor.putLong (key + PREF_CTV_ACCUM_TIME, mAccumulatedTimeP);
        editor.putLong(key + PREF_CTV_MARKER_TIME, mMarkerTime);
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

    public int getStartMinute(){
        return sMinute;
    }
    public int getStartHour(){
        return sHour;
    }
    public int getFinishMinute(){
        return fMinute;
    }
    public int getFinishHour(){
        return fHour;
    }

    public void setWage(float mSalary) {
        this.mWage = mSalary;
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
}

