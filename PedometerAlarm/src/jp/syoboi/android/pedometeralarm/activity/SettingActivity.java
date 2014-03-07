package jp.syoboi.android.pedometeralarm.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.MyApp;
import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pm2hg.HatenaOAuth.OAuthToken;

import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity
@SuppressWarnings("deprecation")
public class SettingActivity extends PreferenceActivity {

	
	@App	MyApp			mApp;
	@Pref	GlobalPrefs_	mPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}
	
	@OptionsItem(android.R.id.home)
	void onHome() {
		MainActivity_.intent(this)
		.flags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		.start();
		finish();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		init();
	}
	
	void init() {
		// はてなにログイン
		Preference hatena = findPreference("loginHatena");
		if (hatena != null) {
			
			OAuthToken token = mApp.getHatenaAuth();
			String summary = null;
			if (token != null) {
				summary = getString(R.string.loginAsFmt, token.urlName, token.displayName);
			}
			hatena.setSummary(summary);
			
			hatena.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					LoginActivity_.intent(SettingActivity.this).start();
					return true;
				}
			});
		}
	}
}
