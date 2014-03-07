package jp.syoboi.android.pedometeralarm.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import jp.syoboi.android.pedometeralarm.GlobalPrefs_;
import jp.syoboi.android.pedometeralarm.MyApp;
import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pm2hg.HatenaOAuth;
import jp.syoboi.android.pm2hg.HatenaOAuth.OAuthToken;

import org.androidannotations.annotations.AfterTextChange;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EActivity(R.layout.login_activity)
public class LoginActivity extends Activity {
	
	static final String TAG = "LoginActivity";
	
	@ViewById(android.R.id.progress)	View		mProgress;
	@ViewById(android.R.id.message)		TextView	mMessage;
	@ViewById(R.id.login)				View		mLogin;
	@ViewById(R.id.codeForm)			View		mCodeForm;
	@ViewById(R.id.code)				EditText	mCode;
	@ViewById(R.id.enterCode)			View		mEnterCode;

	@App		MyApp			mApp;
	@Pref		GlobalPrefs_	mPrefs;
	
	@InstanceState
	boolean	mLoginPrepared;
	
//	@InstanceState
	OAuthToken	mOAuthToken;
	
	@AfterViews
	void afterViews() {
		mProgress.setVisibility(View.GONE);
		mMessage.setText(R.string.loginPrompt);
		onAfterTextChangeCode();
		
		if (mLoginPrepared) {
			showCodeForm();
		} else {
			mCodeForm.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 認証コード
	 */
	void showCodeForm() {
		mCodeForm.setVisibility(View.VISIBLE);
		mMessage.setText(R.string.codePrompt);
	}

	/**
	 * ログイン実行
	 */
	@Click(R.id.login)
	void onClickLogin() {
		prepareLogin();
	}
	
	/**
	 * 認証コードを張り付け
	 */
	@Click(R.id.pasteCode)
	void onClickPasteCode() {
		mCode.onTextContextMenuItem(android.R.id.paste);
	}
	
	/**
	 * 認証コードのテキストが変更された
	 */
	@AfterTextChange(R.id.code)
	void onAfterTextChangeCode() {
		mEnterCode.setEnabled(mCode.length() > 0);
	}
	
	/**
	 * 認証コード確定
	 */
	@Click(R.id.enterCode)
	void onClickEnterCode() {
		mCodeForm.setVisibility(View.GONE);
		mMessage.setText(R.string.verifyingCode);
		mProgress.setVisibility(View.VISIBLE);
		verifyCode(mCode.getText().toString());
	}
	
	/**
	 * ログイン開始(リクエストークン取得スレッドを開始)
	 */
	void prepareLogin() {
		mLoginPrepared = false;
		mLogin.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);
		mMessage.setText(R.string.prepareLogin);
		prepareLoginInBackground();
	}
	
	/**
	 * リクエストトークンを取得
	 */
	@Background
	void prepareLoginInBackground() {
		try {
			onPrepareLoginResult(HatenaOAuth.getRequestToken());
		}
		catch (Exception e) {
			onPrepareLoginResult(e);
		}
	}
	
	/**
	 * リクエストトークン取得完了
	 * @param result
	 */
	@UiThread
	void onPrepareLoginResult(Object result) {
		mProgress.setVisibility(View.GONE);
		if (result instanceof OAuthToken) {
			// 認証用URLを生成してブラウザを起動
			mLoginPrepared = true;
			showCodeForm();
			
			mOAuthToken = (OAuthToken) result;
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(HatenaOAuth.getAuthorizationUrl(mOAuthToken)));
			startActivity(i);
		} 
		else if (result instanceof Throwable){
			// エラー表示
			mLogin.setVisibility(View.VISIBLE);
			Throwable t = (Throwable) result;
			mMessage.setText(t.getMessage());
		}
	}

	@Background
	void verifyCode(String code) {
		OAuthToken at;
		try {
			at = HatenaOAuth.getAccessToken(mOAuthToken, code);
			Log.v(TAG, "at:" + at);
			onVerifyCodeResult(at);
		} catch (Exception e) {
			e.printStackTrace();
			onVerifyCodeResult(e);
		}
	}
	
	@UiThread
	void onVerifyCodeResult(Object result) {
		mProgress.setVisibility(View.GONE);
		if (result instanceof OAuthToken) {
			OAuthToken oa = (OAuthToken) result;
			mPrefs.hatenaAuth().put(oa.toJson());
			finish();
		}
		else if (result instanceof Throwable) {
			// エラー表示
			mCodeForm.setVisibility(View.VISIBLE);
			Throwable t = (Throwable) result;
			mMessage.setText(t.getMessage());
		}
	}
}
