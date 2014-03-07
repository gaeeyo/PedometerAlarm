package jp.syoboi.android.pedometeralarm;

import android.app.Application;

import java.io.File;

import jp.syoboi.android.pedometeralarm.utils.MyLog;
import jp.syoboi.android.pm2hg.HatenaOAuth.OAuthToken;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EApplication
public class MyApp extends Application {
	
	@Pref	GlobalPrefs_	mPrefs;
	
	@AfterInject
	void afterInject() {
		MyLog.setFile(new File(getCacheDir(), "pedometerAlarm.log"));
	}
	
	
	public OAuthToken getHatenaAuth() {
		OAuthToken token = OAuthToken.fromJson(mPrefs.hatenaAuth().get());
		return token;
	}
}
