package jp.syoboi.android.pedometeralarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.service.PollingService;
import jp.syoboi.android.pedometeralarm.service.PollingService_;

import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EReceiver
public class MyReceiver extends BroadcastReceiver {

	static final String TAG = "MyReceiver";
	
	@Pref
	GlobalPrefs_	mPrefs;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive intent:" + intent);
		String action = (intent != null ? intent.getAction() : null);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			PollingService.updateAlarm(context);
		}
		
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
			AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			
			Intent i = PollingService_.intent(context).get();
			if (am.isSpeakerphoneOn()) {
				i.setAction(PollingService.ACTION_STOP);
			} else {
				i.setAction(PollingService.ACTION_START);
			}
			context.startService(i);
		}
	}

	
}
