package com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.tasks.GetOAuthTokenTask;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.tasks.OAuthTask;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.tasks.UploadPhotoTask;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

public class FlickrjActivity extends Activity {
	public static final String CALLBACK_SCHEME = "estsoft-pilotproject-leewonkyung-selfie";
	public static final String PREFS_NAME = "prefsnames";
	public static final String KEY_OAUTH_TOKEN = "token";
	public static final String KEY_TOKEN_SECRET = "secret";
	public static final String KEY_USER_NAME = "name";
	public static final String KEY_USER_ID = "id";

 	public 	String path, title;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getExtras() != null) {

			if (getIntent().getExtras().containsKey("flickImagePath")) {


				path = getIntent().getStringExtra("flickImagePath");
				title = getIntent().getStringExtra("flickrImageName");

				Log.d("flickr", "path = " + path);
				Log.d("flickr", "title = " + title);
			}

		}


	}

	Handler h = new Handler();
	Runnable init = new Runnable() {

		@Override
		public void run() {


			Log.d("Flickr", "init - run attached");
			OAuth oauth = getOAuthToken();



			if (oauth == null || oauth.getUser() == null) {
				Log.d("Flickr","don't have oauth and user");
				OAuthTask task = new OAuthTask(getContext());
				task.execute();
			} else {
				Log.d("flickr","i have oauth");
				load(oauth);
			}
		}
	};

	private void load(OAuth oauth) {

		Log.d("Flickr", "load attached");

		if(oauth == null)
			Log.d("Flickr", "oauth == null");
		if(path == null)
			Log.d("Flickr", "path == null");
		if(title == null)
			Log.d("Flickr", "title == null");



		if (oauth != null && path != null && title != null) {

			Log.d("Flickr", "load- in attached");
			UploadPhotoTask taskUpload = new UploadPhotoTask(this, new File(path), title);
			taskUpload.setOnUploadDone(new UploadPhotoTask.onUploadDone() {

				@Override
				public void onComplete() {
					finish();
				}
			});

			taskUpload.execute(oauth);
		} else {
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getIntent();
		String scheme = intent.getScheme();
		OAuth savedToken = getOAuthToken();

		if (CALLBACK_SCHEME.equals(scheme)	&& (savedToken == null || savedToken.getUser() == null)) {
			Uri uri = intent.getData();
			String query = uri.getQuery();
			String[] data = query.split("&");
			if (data != null && data.length == 2) {
				String oauthToken = data[0].substring(data[0].indexOf("=") + 1);
				String oauthVerifier = data[1]
						.substring(data[1].indexOf("=") + 1);

				OAuth oauth = getOAuthToken();
				if (oauth != null && oauth.getToken() != null && oauth.getToken().getOauthTokenSecret() != null) {

					Log.d("Flickr","resume oauth");
					GetOAuthTokenTask task = new GetOAuthTokenTask(this);
					task.execute(oauthToken, oauth.getToken().getOauthTokenSecret(), oauthVerifier);
				}
			}
		}

		new Thread() {
			@Override
			public void run() {
				h.post(init);
			}
		}.start();

	}

	// checks if auth is failed or succeeded
	public void onOAuthDone(OAuth result) {

		Log.d("Flickr", "onOAuthDone attached");

		if (result == null) {
			Toast.makeText(this, "Authorization failed", //$NON-NLS-1$
					Toast.LENGTH_LONG).show();
		} else {
			User user = result.getUser();
			OAuthToken token = result.getToken();
			if (user == null || user.getId() == null || token == null
					|| token.getOauthToken() == null
					|| token.getOauthTokenSecret() == null) {
				Toast.makeText(this, "Authorization failed", //$NON-NLS-1$
						Toast.LENGTH_LONG).show();
				return;
			}
			String message = String.format(Locale.KOREA, "Authorization Succeed: user=%s, userId=%s, oauthToken=%s, tokenSecret=%s", //$NON-NLS-1$
					user.getUsername(), user.getId(), token.getOauthToken(), token.getOauthTokenSecret());
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			saveOAuthToken(user.getUsername(), user.getId(),
					token.getOauthToken(), token.getOauthTokenSecret());
			load(result);
		}
	}

	// Restore preferences
	public OAuth getOAuthToken() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String oauthTokenString = settings.getString(KEY_OAUTH_TOKEN, null);
		String tokenSecret = settings.getString(KEY_TOKEN_SECRET, null);
		if (oauthTokenString == null && tokenSecret == null) {
			return null;
		}
		OAuth oauth = new OAuth();
		String userName = settings.getString(KEY_USER_NAME, null);
		String userId = settings.getString(KEY_USER_ID, null);
		if (userId != null) {
			User user = new User();
			user.setUsername(userName);
			user.setId(userId);
			oauth.setUser(user);
		}
		OAuthToken oauthToken = new OAuthToken();
		oauth.setToken(oauthToken);
		oauthToken.setOauthToken(oauthTokenString);
		oauthToken.setOauthTokenSecret(tokenSecret);


		Log.d("flickr", "PREFS oauthTokenString : " + oauthTokenString);
		Log.d("flickr", "PREFS tokenSecret : " + tokenSecret);
		Log.d("flickr", "PREFS userName : " + userName);
		Log.d("flickr", "PREFS userId : " + userId);

		return oauth;
	}

	// saves oAuthtoken to shared preferences
	public void saveOAuthToken(String userName, String userId, String token,
			String tokenSecret) {
		SharedPreferences sp = getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();

		if(token != null)
			editor.putString(KEY_OAUTH_TOKEN, token);
		if(tokenSecret != null)
			editor.putString(KEY_TOKEN_SECRET, tokenSecret);
		if(userName !=  null)
			editor.putString(KEY_USER_NAME, userName);

		if(userId != null)
			editor.putString(KEY_USER_ID, userId);
		editor.commit();
	}

	private Context getContext() {
		return this;

	}
}