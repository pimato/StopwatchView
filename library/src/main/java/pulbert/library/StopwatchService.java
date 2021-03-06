package pulbert.library;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;


public class StopwatchService extends Service {
    // Member fields
    private long mElapsedTime;
    private long mStartTime;
    private int mYear,mMonth,mDay;
    private int sMinute,sHour;
    private boolean mLoadApp;
    private NotificationManager mNotificationManager;

    // Constants for intent information
    // Make this a large number to avoid the alarm ID's which seem to be 1, 2, ...
    // Must also be different than TimerReceiver.IN_USE_NOTIFICATION_ID
    private static final int NOTIFICATION_ID = Integer.MAX_VALUE - 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        sHour = 0;
        sMinute = 0;
        mYear = 0;
        mMonth = 0;
        mDay = 0;
        mElapsedTime = 0;
        mStartTime = 0;
        mLoadApp = false;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        // May not have the most recent values.
        readFromSharedPrefs();

        String actionType = intent.getAction();
        long actionTime = intent.getLongExtra(StopwatchView.MESSAGE_TIME, Utils.getTimeNow());
        boolean showNotif = intent.getBooleanExtra(StopwatchView.SHOW_NOTIF, true);
        boolean updateCircle = showNotif; // Don't save updates to the cirle if we're in the app.
        if (actionType.equals(StopwatchView.START_STOPWATCH)) {
            mStartTime = actionTime;
            writeSharedPrefsStarted(mYear,mMonth,mDay,sHour,sMinute,mStartTime, updateCircle);
            if (showNotif) {
                setNotification(mStartTime - mElapsedTime, true);
            } else {
                saveNotification(mStartTime - mElapsedTime, true);
            }
        }
        else if (actionType.equals(StopwatchView.STOP_STOPWATCH)) {
            mElapsedTime = mElapsedTime + (actionTime - mStartTime);
            writeSharedPrefsStopped(mYear,mMonth,mDay,sHour,sMinute,mElapsedTime, updateCircle);
            if (showNotif) {
                setNotification(actionTime - mElapsedTime, false);
            } else {
                saveNotification(mElapsedTime, false);
            }
        } else if (actionType.equals(StopwatchView.RESET_STOPWATCH)) {
            mLoadApp = false;
            writeSharedPrefsReset(updateCircle);
            clearSavedNotification();
            stopSelf();

        } else if (actionType.equals(StopwatchView.RESET_AND_LAUNCH_STOPWATCH)) {
            mLoadApp = true;
            writeSharedPrefsReset(updateCircle);
            clearSavedNotification();
            closeNotificationShade();
            stopSelf();

        }

        else if (actionType.equals(StopwatchView.SHOW_NOTIF)) {
            // SHOW_NOTIF sent from the DeskClock.onPause
            // If a notification is not displayed, this service's work is over
            if (!showSavedNotification()) {
                stopSelf();
            }
        } else if (actionType.equals(StopwatchView.KILL_NOTIF)) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        clearSavedNotification();
        sHour = 0;
        sMinute =0;
        mYear = 0;
        mMonth = 0;
        mDay = 0;
        mElapsedTime = 0;
        mStartTime = 0;
        if (mLoadApp) {
            Intent activityIntent = new Intent(getApplicationContext(), getLauncherActivity(getApplicationContext()));
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(activityIntent);
            mLoadApp = false;
        }
    }

    private void setNotification(long clockBaseTime, boolean clockRunning) {
        Context context = getApplicationContext();
  //      // Intent to load the app for a non-button click.
        Intent intent = new Intent(context, getLauncherActivity(getApplicationContext()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Set up remoteviews for the notification.
        RemoteViews remoteViewsCollapsed = new RemoteViews(getPackageName(), R.layout.stopwatch_notif_collapsed);

        remoteViewsCollapsed.setOnClickPendingIntent(R.id.swn_collapsed_hitspace, pendingIntent);

        //set Chronometers
        remoteViewsCollapsed.setChronometer(R.id.swn_collapsed_chronometer, clockBaseTime, null, clockRunning);

        remoteViewsCollapsed.setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);
        RemoteViews remoteViewsExpanded = new RemoteViews(getPackageName(), R.layout.stopwatch_notif_expanded);
        remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_expanded_hitspace, pendingIntent);

        remoteViewsExpanded.setChronometer(R.id.swn_expanded_chronometer, clockBaseTime, null, clockRunning);
        remoteViewsExpanded.
                setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);

        if (clockRunning) {

            remoteViewsExpanded.setTextViewText(
                    R.id.swn_left_button, getResources().getText(R.string.sw_pause_button));
            Intent leftButtonIntent = new Intent(context, StopwatchService.class);
            leftButtonIntent.setAction(StopwatchView.STOP_STOPWATCH);
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_left_button,
                    PendingIntent.getService(context, 0, leftButtonIntent, 0));
            remoteViewsExpanded.
                    setTextViewCompoundDrawablesRelative(R.id.swn_left_button,
                            R.drawable.ic_pause_black_24dp, 0, 0, 0);

            // Right button: stop clock
            remoteViewsExpanded.setTextViewText(
                    R.id.swn_right_button, getResources().getText(R.string.sw_stop_button));
            Intent rightButtonIntent = new Intent(context, StopwatchService.class);
            rightButtonIntent.setAction(StopwatchView.RESET_AND_LAUNCH_STOPWATCH);
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_right_button,
                    PendingIntent.getService(context, 0, rightButtonIntent, 0));
            remoteViewsExpanded.
                    setTextViewCompoundDrawablesRelative(R.id.swn_right_button,
                            R.drawable.ic_notify_stop, 0, 0, 0);
            remoteViewsCollapsed.setViewVisibility(R.id.swn_collapsed_laps, View.GONE);
            remoteViewsExpanded.setViewVisibility(R.id.swn_expanded_laps, View.GONE);

        } else {
            // Left button: start clock
            remoteViewsExpanded.setTextViewText(
                    R.id.swn_left_button, getResources().getText(R.string.sw_resume_button));
            Intent leftButtonIntent = new Intent(context, StopwatchService.class);
            leftButtonIntent.setAction(StopwatchView.START_STOPWATCH);
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_left_button, PendingIntent.getService(context, 0, leftButtonIntent, 0));
            remoteViewsExpanded.setTextViewCompoundDrawablesRelative(R.id.swn_left_button, R.drawable.ic_notify_start, 0, 0, 0);


            // Right button: reset Clock
            remoteViewsExpanded.setTextViewText(
                    R.id.swn_right_button, getResources().getText(R.string.sw_stop_button));
            Intent rightButtonIntent = new Intent(context, StopwatchService.class);
            rightButtonIntent.setAction(StopwatchView.RESET_AND_LAUNCH_STOPWATCH);
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_right_button,
                    PendingIntent.getService(context, 0, rightButtonIntent, 0));
            remoteViewsExpanded.
                    setTextViewCompoundDrawablesRelative(R.id.swn_right_button,
                            R.drawable.ic_notify_stop, 0, 0, 0);

            // Show stopped string.
            remoteViewsCollapsed.
                    setTextViewText(R.id.swn_collapsed_laps, getString(R.string.swn_paused));
            remoteViewsCollapsed.setViewVisibility(R.id.swn_collapsed_laps, View.VISIBLE);
            remoteViewsExpanded.
                    setTextViewText(R.id.swn_expanded_laps, getString(R.string.swn_paused));
            remoteViewsExpanded.setViewVisibility(R.id.swn_expanded_laps, View.VISIBLE);
        }

        Intent dismissIntent = new Intent(context, StopwatchService.class);
        dismissIntent.setAction(StopwatchView.RESET_STOPWATCH);

        Notification notification = new Notification.Builder(context)
                .setAutoCancel(!clockRunning)
                .setContent(remoteViewsCollapsed)
                .setOngoing(clockRunning)
                .setDeleteIntent(PendingIntent.getService(context, 0, dismissIntent, 0))
                .setSmallIcon(R.drawable.ic_tab_stopwatch_activated)
                .setPriority(Notification.PRIORITY_MAX).build();
        notification.bigContentView = remoteViewsExpanded;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    /** Save the notification to be shown when the app is closed. **/
    private void saveNotification(long clockTime, boolean clockRunning) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (clockRunning) {
            editor.putLong(StopwatchView.NOTIF_CLOCK_BASE, clockTime);
            editor.putLong(StopwatchView.NOTIF_CLOCK_ELAPSED, -1);
            editor.putBoolean(StopwatchView.NOTIF_CLOCK_RUNNING, true);
        } else {
            editor.putLong(StopwatchView.NOTIF_CLOCK_ELAPSED, clockTime);
            editor.putLong(StopwatchView.NOTIF_CLOCK_BASE, -1);
            editor.putBoolean(StopwatchView.NOTIF_CLOCK_RUNNING, false);
        }
        editor.putBoolean(StopwatchView.PREF_UPDATE_CIRCLE, false);
        editor.apply();
    }

    /** Show the most recently saved notification. **/
    private boolean showSavedNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        long clockBaseTime = prefs.getLong(StopwatchView.NOTIF_CLOCK_BASE, -1);
        long clockElapsedTime = prefs.getLong(StopwatchView.NOTIF_CLOCK_ELAPSED, -1);
        boolean clockRunning = prefs.getBoolean(StopwatchView.NOTIF_CLOCK_RUNNING, false);
        if (clockBaseTime == -1) {
            if (clockElapsedTime == -1) {
                return false;
            } else {
                // We don't have a clock base time, so the clock is stopped.
                // Use the elapsed time to figure out what time to show.
                mElapsedTime = clockElapsedTime;
                clockBaseTime = Utils.getTimeNow() - clockElapsedTime;
            }
        }
        setNotification(clockBaseTime, clockRunning);
        return true;
    }

    private void clearSavedNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(StopwatchView.NOTIF_CLOCK_BASE);
        editor.remove(StopwatchView.NOTIF_CLOCK_RUNNING);
        editor.remove(StopwatchView.NOTIF_CLOCK_ELAPSED);
        editor.apply();
    }

    private void closeNotificationShade() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(intent);
    }

    private void readFromSharedPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStartTime = prefs.getLong(StopwatchView.PREF_START_TIME, 0);
        mElapsedTime = prefs.getLong(StopwatchView.PREF_ACCUM_TIME, 0);
        mYear = prefs.getInt(StopwatchView.PREF_YEAR,0);
        mMonth = prefs.getInt(StopwatchView.PREF_MONTH,0);
        mDay = prefs.getInt(StopwatchView.PREF_DAY,0);

        sMinute = prefs.getInt(StopwatchView.PREF_START_MINUTE,0);
        sHour = prefs.getInt(StopwatchView.PREF_START_HOUR,0);
    }

    private void writeToSharedPrefs(Integer year,Integer month,Integer day,
                                    Integer hour,Integer minute,
                                    Long startTime, Long elapsedTime, Integer state, boolean updateCircle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        if(year != null){
            editor.putInt(StopwatchView.PREF_YEAR, year);
            editor.putInt(StopwatchView.PREF_MONTH,month);
            editor.putInt(StopwatchView.PREF_DAY,day);
            editor.putInt(StopwatchView.PREF_START_HOUR,hour);
            editor.putInt(StopwatchView.PREF_START_MINUTE,minute);

            mYear = year;
            mMonth = month;
            mDay = day;
            sHour = hour;
            sMinute = minute;

        }
        if (startTime != null) {
            editor.putLong(StopwatchView.PREF_START_TIME, startTime);
            mStartTime = startTime;
        }
        if (elapsedTime != null) {
            editor.putLong(StopwatchView.PREF_ACCUM_TIME, elapsedTime);
            mElapsedTime = elapsedTime;
        }
        if (state != null) {
            if (state == StopwatchView.STOPWATCH_RESET) {
                editor.putInt(StopwatchView.PREF_STATE, StopwatchView.STOPWATCH_RESET);
            } else if (state == StopwatchView.STOPWATCH_RUNNING) {
                editor.putInt(StopwatchView.PREF_STATE, StopwatchView.STOPWATCH_RUNNING);
            } else if (state == StopwatchView.STOPWATCH_STOPPED) {
                editor.putInt(StopwatchView.PREF_STATE, StopwatchView.STOPWATCH_STOPPED);
            }
        }
        editor.putBoolean(StopwatchView.PREF_UPDATE_CIRCLE, updateCircle);
        editor.apply();
    }

    private void writeSharedPrefsStarted(Integer year,Integer month,Integer day,
                                         Integer hour,Integer minute,
                                         long startTime, boolean updateCircle) {
        writeToSharedPrefs(year,month,day,hour,minute,startTime, null, StopwatchView.STOPWATCH_RUNNING, updateCircle);
        if (updateCircle) {
            long time = Utils.getTimeNow();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());
            long intervalStartTime = prefs.getLong(
                    StopwatchView.KEY + StopwatchView.PREF_CTV_INTERVAL_START, -1);
            if (intervalStartTime != -1) {
                intervalStartTime = time;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(StopwatchView.KEY + StopwatchView.PREF_CTV_INTERVAL_START,
                        intervalStartTime);
                editor.putBoolean(StopwatchView.KEY + StopwatchView.PREF_CTV_PAUSED, false);
                editor.apply();
            }
        }
    }


    private void writeSharedPrefsStopped(Integer year,Integer month,Integer day,
                                         Integer hour,Integer minute,
                                         long elapsedTime, boolean updateCircle) {
        writeToSharedPrefs(year,month,day,hour,minute,null, elapsedTime, StopwatchView.STOPWATCH_STOPPED, updateCircle);
        if (updateCircle) {
            long time = Utils.getTimeNow();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());
            long accumulatedTime = prefs.getLong(
                    StopwatchView.KEY + StopwatchView.PREF_CTV_ACCUM_TIME, 0);
            long intervalStartTime = prefs.getLong(
                    StopwatchView.KEY + StopwatchView.PREF_CTV_INTERVAL_START, -1);
            accumulatedTime += time - intervalStartTime;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(StopwatchView.KEY + StopwatchView.PREF_CTV_ACCUM_TIME, accumulatedTime);
            editor.putBoolean(StopwatchView.KEY + StopwatchView.PREF_CTV_PAUSED, true);
            editor.putLong(
                    StopwatchView.KEY + StopwatchView.PREF_CTV_CURRENT_INTERVAL, accumulatedTime);
            editor.apply();
        }
    }

    private void writeSharedPrefsReset(boolean updateCircle) {
        writeToSharedPrefs(null,null,null,null,null,null, null, StopwatchView.STOPWATCH_RESET, updateCircle);
    }

    public static Class<?> getLauncherActivity(Context context)
    {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}