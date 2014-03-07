package jp.syoboi.android.pedometeralarm.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateUtils;

import jp.co.sharp.android.hardware.Pedometer;
import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.utils.MyLog;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EService
public class PollingService extends Service {
 
	static final String TAG = "PollingService";
	
	/**
	 * 1秒あたりの歩数(1秒に3.33歩歩く可能性がある)
	 *　最大 50歩/15秒 とする
	 */
	static final float MAX_STEP_PER_SECOND = 50f / 15f;
	
	static final float MIN_TIME_PER_STEP = MAX_STEP_PER_SECOND * DateUtils.SECOND_IN_MILLIS;
	
	
	static final long INTERVAL_POLLING = 2 * DateUtils.MINUTE_IN_MILLIS; 

	@Pref GlobalPrefs_	mPrefs;
	
	Pedometer	mPedometer;

	@Override
	public void onCreate() {
		super.onCreate();
		mPedometer = Pedometer.createInstance(this);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	
		int newSteps = mPedometer.getIntParameter(Pedometer.STEPS);
		int oldSteps = mPrefs.steps().get();
		boolean changed = (newSteps != oldSteps);
		
		MyLog.v(TAG, "onStartCommand 歩数: " + newSteps + " changed:" + changed);
		if (changed) {
			// 歩数が変わっていた
			int newNextSteps = newSteps + (1000 - (newSteps % 1000));
			long minNextTime = (long) ((newNextSteps - newSteps) * MIN_TIME_PER_STEP); 
			MyLog.v(TAG, "次のアラート newNextSteps:" + newNextSteps
					+ " newNextStepまでの時間(分):" + (minNextTime / DateUtils.MINUTE_IN_MILLIS) 
					);

			// 設定で有効化されている
			long interval = Math.max(minNextTime, INTERVAL_POLLING);
			updateAlarm(this, minNextTime, interval);
			
			mPrefs.edit()
			.steps().put(newSteps)
			.pollingInterval().put(newSteps)
			.lastTime().put(System.currentTimeMillis())
			.nextAlarmSteps().put(newNextSteps)
			.apply();
			
		}
		else {
		}
		
		
		return super.onStartCommand(intent, flags, startId);
	}

	public static void updateAlarm(Context context) {
		updateAlarm(context, SystemClock.elapsedRealtime(), INTERVAL_POLLING);
	}

	
	/**
	 * Alarmをアップデート
	 * @param context
	 */
	public static void updateAlarm(Context context, long next, long interval) {
		GlobalPrefs_ prefs = new GlobalPrefs_(context);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (prefs.isActive().get()) {
			MyLog.v(TAG, "アラーム設定 interval:" + interval + "(" + (interval / DateUtils.MINUTE_IN_MILLIS) + "分)");
			
			// 設定で有効化されている
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 
					next,
					interval, createPendingIntent(context));
		}
		else {
			MyLog.v(TAG, "アラーム無効");
			// 設定で無効化されている
			am.cancel(createPendingIntent(context));
		}
	}
	
	
	
	public static PendingIntent createPendingIntent(Context context) {
		Intent i = new Intent(context, PollingService_.class);
		return PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	
}
