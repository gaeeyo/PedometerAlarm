package jp.syoboi.android.oauth;

import android.text.TextUtils;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

public class OAuth {

	static final Random RAND = new Random();
	static final StringBuilder NONCE_BUF = new StringBuilder();
	static final Charset UTF8 = Charset.forName("utf-8");


	public static synchronized String makeNonce() {
		final String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final int charsLen = chars.length();

		Random r = RAND;
		StringBuilder buf = NONCE_BUF;
		buf.delete(0, buf.length());
		for (int j=0; j<20; j++) {
			buf.append(chars.charAt(r.nextInt(charsLen)));
		}
		return buf.toString();
	}

	public static void testRequest() {
		String method = "POST";
		String api = "https://api.twitter.com/1/statuses/update.json";
		String oauthConsumerKey = "xvz1evFS4wEEPTGEFPHBog";
		String oauthConsumerSecret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
		String oauthToken = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
		String oauthTokenSecret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
		long timeInSec = 1318622958;

		RequestParams params = new RequestParams();
		params.add("include_entities", "true");
		params.add("status", "Hello Ladies + Gentlemen, a signed OAuth request!");

		String nonce = "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg";

		HttpPost post = (HttpPost) makeRequest(method, api, params,
				oauthConsumerKey,
				oauthConsumerSecret,
				oauthToken,
				oauthTokenSecret,
				nonce,
				timeInSec);
//		MyLog.d(post.toString());
	}

	public static HttpGet makeGetRequest(String api, RequestParams params,
			String oauthConsumerKey, String oauthConsumerSecret,
			String oauthToken, String oauthTokenSecret) {
		return (HttpGet) makeRequest("GET", api, params,
				oauthConsumerKey, oauthConsumerSecret,
				oauthToken, oauthTokenSecret,
				makeNonce(),
				System.currentTimeMillis() / 1000);
	}

	public static HttpPost makePostRequest(String api, RequestParams params,
			String oauthConsumerKey, String oauthConsumerSecret,
			String oauthToken, String oauthTokenSecret) {
		return (HttpPost) makeRequest("POST", api, params,
				oauthConsumerKey, oauthConsumerSecret,
				oauthToken, oauthTokenSecret,
				makeNonce(),
				System.currentTimeMillis() / 1000);
	}

	public static HttpRequest makeRequest(String method, String api, RequestParams params,
			String oauthConsumerKey, String oauthConsumerSecret,
			String oauthToken, String oauthTokenSecret) {
		return makeRequest(method, api, params,
				oauthConsumerKey, oauthConsumerSecret,
				oauthToken, oauthTokenSecret,
				makeNonce(),
				System.currentTimeMillis() / 1000);
	}


	/**
	 *
	 * @param method
	 * @param api
	 * @param orgParams  もしも "oauth_" のキーが含まれていたらAuthorizationヘッダに入れてorgParamsから取り除く
	 * @param oauthConsumerKey
	 * @param oauthConsumerSecret
	 * @param oauthToken
	 * @param oauthTokenSecret
	 * @param nonce
	 * @param timeInSec
	 * @return
	 */
	public static HttpRequest makeRequest(
			String method, String api, RequestParams orgParams,
			String oauthConsumerKey,
			String oauthConsumerSecret,
			String oauthToken,
			String oauthTokenSecret,
			String nonce,
			long timeInSec) {


		RequestParams oauthParams = new RequestParams();
		oauthParams.add("oauth_consumer_key", oauthConsumerKey);
		oauthParams.add("oauth_nonce", nonce);
		oauthParams.add("oauth_signature_method", "HMAC-SHA1");
		oauthParams.add("oauth_timestamp", Long.toString(timeInSec));
		if (!TextUtils.isEmpty(oauthToken)) {
			oauthParams.add("oauth_token", oauthToken);
		}
		oauthParams.add("oauth_version", "1.0");

		String realm = null;
		for (int j=orgParams.size()-1; j>=0; j--) {
			NameValuePair rp = orgParams.get(j);
			if (rp.getName().startsWith("oauth:")) {
				orgParams.remove(j);
				String newName = rp.getName().substring(6);
				if (newName.equals("realm")) {
					realm = rp.getValue();
				} else {
					oauthParams.add(newName, rp.getValue());
				}
			}
		}

		RequestParams allParams = new RequestParams(orgParams);
		allParams.addAll(oauthParams);
		Collections.sort(allParams, NAME_COMPARATOR);

		try {
			String encodedParams = urlEncode(allParams);

			String value = method
					+ "&" + MyURLEncoder.encode(api, UTF8)
					+ "&" + MyURLEncoder.encode(encodedParams, UTF8);

			String signingKey = makeSigningKey(oauthConsumerSecret, oauthTokenSecret);

			SecretKeySpec sks = new SecretKeySpec(
					signingKey.getBytes(),
					"HmacSHA1");
			try {

				Mac mac = Mac.getInstance("HmacSHA1");
				mac.init(sks);

				byte [] data = mac.doFinal(value.getBytes());
				String  oauthSignature = new String(Base64.encode(data, Base64.NO_WRAP));

				oauthParams.add("oauth_signature", oauthSignature);
				Collections.sort(oauthParams, NAME_COMPARATOR);

				Header authorizationHeader = makeAuthorizationHeader(realm, oauthParams);

				if (method.equalsIgnoreCase("GET")) {
					HttpGet request = new HttpGet(api + "?" + urlEncode(orgParams));
					request.addHeader(authorizationHeader);
					return request;
				}
				else {
					HttpPost request = new HttpPost(api);
					request.setEntity(new UrlEncodedFormEntity(orgParams));
					request.addHeader(authorizationHeader);
					return request;
				}

			} catch (InvalidKeyException e1) {
				e1.printStackTrace();
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Header makeAuthorizationHeader(String realm, RequestParams oauthParams) {
		StringBuilder sb = new StringBuilder();
		sb.append("OAuth ");
		int count = 0;
		if (realm != null) {
			sb.append("realm").append('=').append('"')
			.append(MyURLEncoder.encode(realm, UTF8))
			.append('"');
			count++;
		}

		for (NameValuePair param: oauthParams) {
			if (count++ > 0) {
				sb.append(',');
			}
			sb.append(MyURLEncoder.encode(param.getName(), UTF8))
			.append('=')
			.append('"')
			.append(MyURLEncoder.encode(param.getValue(), UTF8))
			.append('"');
		}
		return new BasicHeader("Authorization", sb.toString());
	}

	public static String urlEncode(RequestParams params) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		for (NameValuePair i: params) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(MyURLEncoder.encode(i.getName(), UTF8));
			sb.append('=');
			sb.append(MyURLEncoder.encode(i.getValue(), UTF8));
		}
		return sb.toString();
	}

	public static String makeSigningKey(String oauthConsumerSecret, String tokenSecret) {
		return MyURLEncoder.encode(oauthConsumerSecret,UTF8)
				+  "&"
				+ MyURLEncoder.encode(TextUtils.isEmpty(tokenSecret) ? "" : tokenSecret, UTF8);
	}


	static Comparator<BasicNameValuePair> NAME_COMPARATOR = new Comparator<BasicNameValuePair>() {

		@Override
		public int compare(BasicNameValuePair lhs, BasicNameValuePair rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}
	};

	public static class RequestParams extends ArrayList<BasicNameValuePair> {
		/**
		 *
		 */
		private static final long serialVersionUID = 5599749071087904408L;


		public RequestParams() {

		}

		public RequestParams(RequestParams params) {
			super(params);
		}

		public void add(String name, String value) {
			add(new BasicNameValuePair(name, value));
		}
	}
}
