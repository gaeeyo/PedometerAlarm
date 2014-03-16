package jp.syoboi.android.pedometeralarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import jp.syoboi.android.pedometeralarm.service.PollingService_;

import org.androidannotations.annotations.EReceiver;

@EReceiver
public class MyReceiver extends BroadcastReceiver {

	static final String TAG = "MyReceiver";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		MyLog.v(TAG, "onReceive intent:" + intent);
		String action = (intent != null ? intent.getAction() : null);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
//			PollingService.updateAlarm(context);
			
			PollingService_.intent(context).start();
		}
		
//		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
//			AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//			
//			Intent i = PollingService_.intent(context).get();
//			if (am.isSpeakerphoneOn()) {
//				i.setAction(PollingService.ACTION_STOP);
//			} else {
//				i.setAction(PollingService.ACTION_START);
//			}
//			context.startService(i);
//		}
	}
}
