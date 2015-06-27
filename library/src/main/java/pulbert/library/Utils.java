package pulbert.library;

import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * Created by X on 20.06.15.
 */
public class Utils {

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }


    /**
     * Clears the persistent data of stopwatch (start time, state, laps, etc...).
     */
    public static void clearSwSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (StopwatchView.PREF_START_TIME);
        editor.remove (StopwatchView.PREF_ACCUM_TIME);
        editor.remove (StopwatchView.PREF_STATE);
        editor.apply();
    }
}
