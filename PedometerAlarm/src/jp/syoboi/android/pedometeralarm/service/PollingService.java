package jp.syoboi.android.pedometeralarm.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
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
		
		speak(mPedometer.getIntParameter(Pedometer.STEPS));
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		HANDLER.removeCallbacks(mCheckRunnable);
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		HANDLER.removeCallbacks(mCheckRunnable);
		HANDLER.post(mCheckRunnable);
		startForeground(NID_APP, mNotification);
		
		return START_STICKY;
	}
	
	void initSteps() {
		mSteps = mPedometer.getIntParameter(Pedometer.STEPS);
		mNextAlarmSteps = computeNextSteps(mSteps);
		mPrevTime = System.currentTimeMillis();
	}
	
	int computeNextSteps(int steps) {
		return steps + (mAlarmSteps - (steps % mAlarmSteps));
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
				int newNextSteps = computeNextSteps(newSteps);
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
					abandonAudioFocus();
				}
			});
		}
		
		String speakText = String.valueOf("現在、" + steps + "歩です");
		speakNow(speakText);
	}
	
	void speakNow(String text) {
		if (mTextToSpeechState == TextToSpeech.SUCCESS) {

			HashMap<String,String> params = new HashMap<String,String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, 
					String.valueOf(AudioManager.STREAM_MUSIC));
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, 
					"1");  
			
			int audioFocus = mAudioManager.requestAudioFocus(
					mOnAudioFocusChangeListener,
					AudioManager.STREAM_MUSIC, 
					AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
			mHasAudioFocus = (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
			
			int speakResult = mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
			if (speakResult != TextToSpeech.SUCCESS) {
				abandonAudioFocus();
			}
			mSpeakText = null;
		} else {
			mSpeakText = text;
		}
	}
	
	boolean mHasAudioFocus;
	
	void abandonAudioFocus() {
		if (mHasAudioFocus) {
			mHasAudioFocus = false;
			mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
		}
	}
	
	OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
		}
	};
}
