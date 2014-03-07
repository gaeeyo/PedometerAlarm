package jp.syoboi.android.pm2hg;


//public class MainActivity extends Activity {
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//
//		Pedometer p = Pedometer.createInstance(this);
//		Log.v("", "歩数:" + p.getIntParameter(Pedometer.STEPS));
//		Log.v("", "ジョギング歩数:" + p.getIntParameter(Pedometer.JOG_STEPS));
//		Log.v("", "有酸素運動の歩数:" + p.getIntParameter(Pedometer.AEROBICS_STEPS));
//		Log.v("", "歩行時間:" + p.getIntParameter(Pedometer.TIME));
//
////		test();
//	}
//
//	@Background
//	void test() {
//		try {
//			OAuthToken token = HatenaOAuth.getRequestToken();
//			onOAuthToken(token);
//
//		} catch (ClientProtocolException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (OAuthException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@UiThread
//	void onOAuthToken(OAuthToken token) {
//		Intent i = new Intent(Intent.ACTION_VIEW);
//		i.setData(Uri.parse(HatenaOAuth.getAuthorizationUrl(token)));
//		startActivity(i);
//	}
//
//}
