package jp.syoboi.android.pedometeralarm.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.TextView;

import jp.co.sharp.android.hardware.Pedometer;
import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pedometeralarm.service.PollingService;
import jp.syoboi.android.pedometeralarm.utils.PmUtils;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
 
@EActivity(R.layout.main_activity)
@OptionsMenu(R.menu.main_activity_menu)
public class MainActivity extends Activity {

	static final String TAG = "MainActivity";
	
	static IntentFilter INTENT_FILTER_PEDOMETER = new IntentFilter();
	static {
		IntentFilter filter = INTENT_FILTER_PEDOMETER;
		filter.addAction(Pedometer.ACTION_START);
		filter.addAction(Pedometer.ACTION_STOP);
	} 
	 

	@ViewById(R.id.warningMessage)		TextView	mWarningMessage;
	@ViewById(R.id.pedometerSetting)	View		mOpenPedometerSettings;
	
	@Pref		GlobalPrefs_	mPrefs;
	
	Pedometer 	mPedometer;
	
	
	/**
	 * Pedometerからの通知を処理
	 */
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateStatus();
		}
	};
	
	@AfterInject
	void afterInject() {
		mPedometer = Pedometer.createInstance(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		registerReceiver(mReceiver, INTENT_FILTER_PEDOMETER);
		updateStatus();
		
		// Alarmを更新する
		if (mPrefs.isActive().get()) {
			PollingService.updateAlarm(this);
		}
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(mReceiver);
		super.onPause();
	}
	
	void updateStatus() {
		boolean showWarning = true;
		if (mPedometer == null) {
			mWarningMessage.setText(R.string.pedometerNotFound);
		}
		else {
			int measureStatus = mPedometer.getIntParameter(Pedometer.MEASURE_STATUS);
			switch (measureStatus) {
			case Pedometer.DEVICE_STOPED:
				mWarningMessage.setText(R.string.suggestPedometerSetting);
				break;
			default:
				showWarning = false;
				break;
			}
		}
		mOpenPedometerSettings.setEnabled(mPedometer != null);
		mWarningMessage.setVisibility(showWarning ? View.VISIBLE : View.GONE);
	}

	@Click(R.id.pedometerSetting)
	void openPedometerSettings() {
		startActivity(PmUtils.createSettingIntent());
	}
	
	@OptionsItem(R.id.settings)
	void onSettings() {
		SettingActivity_.intent(this).start();
	}
}
