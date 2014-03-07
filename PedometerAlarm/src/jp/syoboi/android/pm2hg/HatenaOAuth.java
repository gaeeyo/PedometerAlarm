package jp.syoboi.android.pm2hg;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import jp.syoboi.android.oauth.OAuth;
import jp.syoboi.android.oauth.OAuth.RequestParams;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HatenaOAuth {
	
	public static final String REQUEST_TOKEN_URL = "https://www.hatena.com/oauth/initiate";
	public static final String ACCESS_TOKEN_URL = "https://www.hatena.com/oauth/token"; 
	
	public static String OAUTH_CONSUMER_KEY = "VSZEitd3y9JCsw==";
	public static String OAUTH_CONSUMER_SECRET = "t5YYmK7XqzCrVz3WPTUWGVSUHjU=";
	public static String OAUTH_REALM = "";

	public static OAuthToken getRequestToken() throws OAuthException, ClientProtocolException, IOException {


		if (true) {
			List<NameValuePair> kv = URLEncodedUtils.parse(URI.create("foo=bar"), "utf-8");
			for (NameValuePair value: kv) {
				Log.v("", value.getName() + " = " + value.getValue());
			}
		}

		RequestParams params = new RequestParams();
		params.add("scope", "read_public,read_private");
		params.add("oauth:oauth_callback", "oob");
		params.add("oauth:realm", OAUTH_REALM);

		HttpPost req = OAuth.makePostRequest(
				REQUEST_TOKEN_URL,
				params,
				OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET,
				null, null);

		DefaultHttpClient client = new DefaultHttpClient();

		try {
			HttpResponse res = client.execute(req);
			String body = EntityUtils.toString(res.getEntity());


			List<NameValuePair> values = URLEncodedUtils.parse(URI.create("?" + body), "utf-8");

			OAuthToken oauth = new OAuthToken();
			for (NameValuePair value: values) {
				String name = value.getName();
				if (name.equals("oauth_token")) {
					oauth.token = value.getValue();
				}
				if (name.equals("oauth_token_secret")) {
					oauth.tokenSecret = value.getValue();
				}
			}
			if (oauth.token != null && oauth.tokenSecret != null) {
				return oauth;
			}
			else {
				throw new OAuthException("サーバからリクエストトークンを取得できませんでした\n" + body);
			}

		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static OAuthToken getAccessToken(OAuthToken token, String verifier) throws OAuthException, ClientProtocolException, IOException {
		RequestParams params = new RequestParams();
		params.add("oauth:oauth_verifier", verifier);
		params.add("oauth:realm", OAUTH_REALM);

		HttpPost req = OAuth.makePostRequest(
				ACCESS_TOKEN_URL,
				params,
				OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET,
				token.token, token.tokenSecret);
		
		DefaultHttpClient client = new DefaultHttpClient();

		try {
			HttpResponse res = client.execute(req);
			String body = EntityUtils.toString(res.getEntity());


			List<NameValuePair> values = URLEncodedUtils.parse(URI.create("?" + body), "utf-8");

			OAuthToken oauth = new OAuthToken();
			for (NameValuePair value: values) {
				String name = value.getName();
				if (name.equals("oauth_token")) {
					oauth.token = value.getValue();
				}
				else if (name.equals("oauth_token_secret")) {
					oauth.tokenSecret = value.getValue();
				}
				else if (name.equals("url_name")) {
					oauth.urlName = value.getValue();
				}
				else if (name.equals("display_name")) {
					oauth.displayName = value.getValue();
				}
			}
			if (oauth.token != null && oauth.tokenSecret != null) {
				return oauth;
			}
			else {
				throw new OAuthException("アクセストークンを取得できませんでした\n" + body);
			}

		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static String getAuthorizationUrl(OAuthToken token) {
		try {
			return "https://www.hatena.ne.jp/touch/oauth/authorize?oauth_token="
					+ URLEncoder.encode(token.token, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static class OAuthToken {
		public String token;
		public String tokenSecret;
		public String urlName;
		public String displayName;

		public static OAuthToken fromJson(String s) {
			OAuthToken t = new OAuthToken();
			JSONObject j;
			try {
				j = new JSONObject(s);
				t.token = j.getString("t");
				t.tokenSecret = j.getString("ts");
				t.urlName = j.getString("un");
				t.displayName = j.getString("dn");
				return t;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public String toJson() {
			JSONObject jo = new JSONObject();
			try {
				jo.put("t", token);
				jo.put("ts", tokenSecret);
				jo.put("un", urlName);
				jo.put("dn", displayName);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo.toString();
		}
	}

	public static class OAuthException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = -1993950898071320661L;
		public OAuthException(String msg) {
			super(msg);
		}
	}
}
