package jp.syoboi.android.pedometeralarm.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import java.util.Locale;

import jp.co.sharp.android.hardware.Pedometer;
import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pedometeralarm.service.PollingService_;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;
 
@EActivity
public class MainActivity extends PreferenceActivity {

	static final String TAG = "MainActivity";
	
	static IntentFilter INTENT_FILTER_PEDOMETER = new IntentFilter();
	static {
		IntentFilter filter = INTENT_FILTER_PEDOMETER;
		filter.addAction(Pedometer.ACTION_START);
		filter.addAction(Pedometer.ACTION_STOP);
	} 
	 

	@Pref		GlobalPrefs_	mPrefs;
	
	
	Pedometer 		mPedometer;
	TextToSpeech	mTts;
	boolean			mTtsInitialized;
	
	Preference		mPedometerPref;
	Preference		mTtsPref;
//	Preference		mTestTts;
	
	/**
	 * Pedometerからの通知を処理
	 */
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateStatus();
		}
	};
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		mPedometer = Pedometer.createInstance(this);
		mPedometerPref = findPreference("openPedometerSettings");
		mTtsPref = findPreference("tts");
//		mTestTts = findPreference("ttsTest");
		
//		mTestTts.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				ttsTest();
//				return true;
//			}
//		});
//		
		// 有効/無効のスイッチが切り替わったらサービスの状態を更新する
		findPreference(mPrefs.isActive().key()).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Context context = MainActivity.this;
				if (mPrefs.isActive().get()) {
					PollingService_.intent(context).start();
				} else {
					PollingService_.intent(context).stop();
				}
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		registerReceiver(mReceiver, INTENT_FILTER_PEDOMETER);
		
		mTtsInitialized = false;
		mTts = new TextToSpeech(this, new OnInitListener() {
			@Override
			public void onInit(int status) {
				mTtsInitialized = (status == TextToSpeech.SUCCESS);
				updateStatus();
			}
		});
		
		updateStatus();
		
		// Alarmを更新する
		if (mPrefs.isActive().get()) {
//			PollingService.updateAlarm(this);
			PollingService_.intent(this).start();
		}
	}
	
	
	
	@Override
	protected void onPause() {
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
		unregisterReceiver(mReceiver);
		super.onPause();
	}
	
	void updateStatus() {
		if (mPedometer == null) {
			mPedometerPref.setSummary(R.string.pedometerNotFound);
		}
		else {
			int measureStatus = mPedometer.getIntParameter(Pedometer.MEASURE_STATUS);
			switch (measureStatus) {
			case Pedometer.DEVICE_STOPED:
				mPedometerPref.setSummary(R.string.suggestPedometerSetting);
				break;
			default:
				mPedometerPref.setSummary(null);
				break;
			}
		}
		mPedometerPref.setEnabled(mPedometer != null);

		if (mTts != null) {
			Locale locale = mTts.getLanguage();
			if (locale == null) {
				mTtsPref.setSummary(R.string.ttsNotFound);
			}
			else {
				mTtsPref.setSummary(R.string.ttsInstalled);
			}
//			mTestTts.setEnabled(mTts != null);
		}
	}
	
//	void ttsTest() {
//		if (mTts != null) {
//			HashMap<String, String> params = new HashMap<String, String>();
//			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, 
//					String.valueOf(AudioManager.STREAM_MUSIC));
//			
//			int steps = 1260;
//			if (mPedometer != null) {
//				steps = mPedometer.getIntParameter(Pedometer.STEPS);
//			}
//			mTts.speak("現在、" + steps + "歩です", TextToSpeech.QUEUE_FLUSH, params);
//		}
//	}
}
