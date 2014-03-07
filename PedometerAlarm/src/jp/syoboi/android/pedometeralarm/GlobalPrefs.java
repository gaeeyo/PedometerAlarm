package jp.syoboi.android.pedometeralarm;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.androidannotations.annotations.sharedpreferences.SharedPref.Scope;

@SharedPref(value=Scope.APPLICATION_DEFAULT)
public interface GlobalPrefs {
	@DefaultBoolean(true)
	public boolean isActive();
	
	public String hatenaAuth();
	public int steps();
	public long pollingInterval();
	public long lastTime();
	public int nextAlarmSteps();
}
