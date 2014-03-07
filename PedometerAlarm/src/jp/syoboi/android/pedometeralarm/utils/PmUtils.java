package jp.syoboi.android.pedometeralarm.utils;

import android.content.Intent;

import jp.co.sharp.android.hardware.Pedometer;

public class PmUtils {
	
	public static final String [] VALUE_KEYS = {
		Pedometer.AEROBICS_STEPS,
		Pedometer.CALORIE,
		Pedometer.DISTANCE,
		Pedometer.EXERCISE,
		Pedometer.EXERCISE_DISTANCE,
		Pedometer.EXERCISE_STEPS,
		Pedometer.FAT_COMBUSTION,
		Pedometer.JOG_STEPS,
		Pedometer.JOG_TIME,
		//Pedometer.MEASURE_STATUS,
		Pedometer.METS,
		Pedometer.EXERCISE_TIME,
		Pedometer.STEPS,
		Pedometer.TIME,
		Pedometer.WALK_STATUS,
		Pedometer.WALK_STEPS,
		Pedometer.WALK_TIME
	};
	
	public static Intent createSettingIntent() {
		String className = "jp.co.sharp.android.settings.pedometersetting.PedometerSettingActivity";
		String packageName = "com.android.settings";
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setClassName(packageName, className);
		return i;
	}
}
