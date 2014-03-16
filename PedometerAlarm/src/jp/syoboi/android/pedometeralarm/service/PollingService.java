package jp.syoboi.android.pedometeralarm.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.format.DateUtils;

import java.util.HashMap;

import jp.co.sharp.android.hardware.Pedometer;
import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pedometeralarm.activity.MainActivity_;
import jp.syoboi.android.pedometeralarm.utils.MyLog;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EService
public class PollingService extends Service {
 
	static final String TAG = "PollingService";
	
	static final int NID_APP = 1;
	
	/**
	 * 1秒あたりの歩数(1秒に3.33歩歩く可能性がある)
	 *　最大 50歩/15秒 とする
	 */
	static final float MAX_STEP_PER_SECOND = 50f / 15f;
	
	static final float MIN_TIME_PER_STEP = MAX_STEP_PER_SECOND * DateUtils.SECOND_IN_MILLIS;
	
	
	static final long INTERVAL_POLLING = 5 * DateUtils.MINUTE_IN_MILLIS; 

	@Pref GlobalPrefs_	mPrefs;
	
	@SystemService AudioManager			mAudioManager;
	@SystemService NotificationManager	mNotificationManager;
	
	static Handler HANDLER = new Handler();
	
	Pedometer	mPedometer;
	Notification mNotification;
	int			mPlugState = -1;
	int			mAlarmSteps = 500;

	int			mSteps = -1;
	int			mNextAlarmSteps;
	long		mPrevTime;
	
	
	BroadcastReceiver	mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
				int state = intent.getIntExtra("state", 0);
				if (mPlugState != state) {
					HANDLER.removeCallbacks(mCheckRunnable);
					mPlugState = state;
					if (state != 0) {
						mSteps = 0;
						mNextAlarmSteps = 0;
						HANDLER.post(mCheckRunnable);
					}
				}
			}
		}
	};
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		PendingIntent pi = PendingIntent.getActivity(this, 0, 
				MainActivity_.intent(this).get(), 
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		mNotification = new Notification.Builder(this)
			.setOngoing(true)
			.setContentIntent(pi)
			.setContentTitle(getString(R.string.app_name))
			.setSmallIcon(R.drawable.stat_notify_app)
			.getNotification();
		
		mPedometer = Pedometer.createInstance(this);
		registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		initSteps();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		mNotificationManager.cancel(NID_APP);
		HANDLER.removeCallbacks(mCheckRunnable);
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		HANDLER.removeCallbacks(mCheckRunnable);
		HANDLER.post(mCheckRunnable);
		mNotificationManager.notify(NID_APP, mNotification);
		startForeground(NID_APP, mNotification);
		
		return START_STICKY;
	}
	
	void initSteps() {
		mSteps = mPedometer.getIntParameter(Pedometer.STEPS);
		mPrevTime = System.currentTimeMillis();
	}
	
	Runnable	mCheckRunnable = new Runnable() {
		
		@Override
		public void run() {
			int newSteps = mPedometer.getIntParameter(Pedometer.STEPS);
			boolean changed = (newSteps != mSteps);
			
			MyLog.v(TAG, "mCheckRunnable 歩数: " + newSteps + " changed:" + changed);
			long nextCheck = 30 * DateUtils.SECOND_IN_MILLIS;
			if (changed) {
				long now = System.currentTimeMillis();
				long ellapsed = (now - mPrevTime);
				
				// 歩数が変わっていた
				int newNextSteps = newSteps + (mAlarmSteps - (newSteps % mAlarmSteps));
				float timePerStep = MIN_TIME_PER_STEP;
				if (ellapsed > 0 && newSteps > mSteps) {
					timePerStep = (newSteps - mSteps) / ellapsed; 
				}
				
				long minNextTime = (long) ((newNextSteps - newSteps) * timePerStep); 

				mSteps = newSteps;
				mPrevTime = now;
				
				if (mNextAlarmSteps != newNextSteps) {
					mNextAlarmSteps = newNextSteps;
					speak(newSteps);
				} 
				
				nextCheck = Math.min(minNextTime, nextCheck);
				nextCheck = Math.max(nextCheck, 5 * DateUtils.SECOND_IN_MILLIS);
				 
				MyLog.v(TAG, "次のアラート newNextSteps:" + newNextSteps
						+ " newNextStepまでの時間(秒):" + (minNextTime / DateUtils.SECOND_IN_MILLIS)
						+ " nextCheck(秒):" + (nextCheck / DateUtils.SECOND_IN_MILLIS) 
						); 
			}
			HANDLER.postDelayed(mCheckRunnable, nextCheck);
		}
	};
	
	TextToSpeech	mTextToSpeech;
	int				mTextToSpeechState = -1;
	String			mSpeakText;
	
	@SuppressWarnings("deprecation")
	public void speak(int steps) {
		
		if (mTextToSpeech == null) {
			mTextToSpeech = new TextToSpeech(this, new OnInitListener() {
				@Override
				public void onInit(int status) {
					mTextToSpeechState = status;
					HANDLER.post(new Runnable() {
						@Override
						public void run() {
							if (mSpeakText != null) {
								speakNow(mSpeakText);
							}
						}
					});
				}
			});
			mTextToSpeech.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
				@Override
				public void onUtteranceCompleted(String utteranceId) {
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mMusicVolume, 0);
//					mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mSpeakVolume, 0);
				}
			});
		}
		
		String speakText = String.valueOf("げんざい、" + steps + " ほです");
		speakNow(speakText);
	}
	
	int mMusicVolume;
	int mSpeakVolume;
	
	void speakNow(String text) {
		if (mTextToSpeechState == TextToSpeech.SUCCESS) {

			mMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			mSpeakVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

			HashMap<String,String> params = new HashMap<String,String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, 
					String.valueOf(AudioManager.STREAM_NOTIFICATION));
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, 
					"1");  
//			params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME,
//					String.valueOf((int)(mMusicVolume * 0.8)));
			 
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(mMusicVolume * 0.7), 0);
//			mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)(mMusicVolume * 0.8), 0);

			mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
			mSpeakText = null;
		} else {
			mSpeakText = text;
		}
	}
	
	/*
	public int onStartCommand_old(Intent intent, int flags, int startId) {
	
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
	*/

	/*
	public static void updateAlarm(Context context) {
		updateAlarm(context, SystemClock.elapsedRealtime(), INTERVAL_POLLING);
	}
	*/

	
	/**
	 * Alarmをアップデート
	 * @param context
	 */
	/*
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
	*/
	
	
	/*
	public static PendingIntent createPendingIntent(Context context) {
		Intent i = new Intent(context, PollingService_.class);
		return PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	*/

	
}
