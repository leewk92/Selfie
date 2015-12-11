package com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.tasks;

import java.net.URL;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.FlickrHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.FlickrjActivity;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.auth.Permission;
import com.googlecode.flickrjandroid.oauth.OAuthToken;

//THe OAUTH process
public class OAuthTask extends AsyncTask<Void, Integer, String> {

	// private static final Logger logger = LoggerFactory
	// .getLogger(OAuthTask.class);
	private static final Uri OAUTH_CALLBACK_URI = Uri
			.parse(FlickrjActivity.CALLBACK_SCHEME + "://oauth"); //$NON-NLS-1$

	private Context mContext;

	// progress dialog before opening browser
	private ProgressDialog mProgressDialog;

	public OAuthTask(Context context) {
		super();
		this.mContext = context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
//		mProgressDialog = ProgressDialog.show(mContext,
//				"", "Generating the authorization request..."); //$NON-NLS-1$ //$NON-NLS-2$
//		mProgressDialog.setCanceledOnTouchOutside(true);
//		mProgressDialog.setCancelable(false);
//		mProgressDialog.setOnCancelListener(new OnCancelListener() {
//			@Override
//			public void onCancel(DialogInterface dlg) {
//				OAuthTask.this.cancel(true);
//			}
//		});
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			Flickr f = FlickrHelper.getInstance().getFlickr();
			OAuthToken oauthToken = f.getOAuthInterface().getRequestToken(
					OAUTH_CALLBACK_URI.toString());
			saveTokenSecret(oauthToken.getOauthTokenSecret());
			URL oauthUrl = f.getOAuthInterface().buildAuthenticationUrl(
					Permission.WRITE, oauthToken);

			System.out.println("--------oAuthURL is--------"
					+ oauthUrl.toString());
			return oauthUrl.toString();
		} catch (Exception e) {
			//			logger.error("Error to oauth", e); //$NON-NLS-1$
			System.out
					.println("-----error in oAuth----catch block of doinback-----");
			return "error:" + e.getMessage(); //$NON-NLS-1$
		}
	}

	private void saveTokenSecret(String tokenSecret) {
		Log.d("flickr", "request token: " + tokenSecret); //$NON-NLS-1$
		//FlickrjActivity act = (FlickrjActivity) mContext;

		SharedPreferences settings = mContext.getSharedPreferences(FlickrjActivity.PREFS_NAME, FlickrjActivity.MODE_PRIVATE);
		String oauthTokenString = settings.getString(FlickrjActivity.KEY_TOKEN_SECRET, null);
		if(oauthTokenString == null)
			((FlickrjActivity)mContext).saveOAuthToken(null, null, null, tokenSecret);
			Log.d("oauth token secret svd",tokenSecret.toString()); //$NON-NLS-1$

	}

	@Override
	protected void onPostExecute(String result) {

		System.out.println("----reached onPostExecute-----");
//		if (mProgressDialog != null) {
//			if(mProgressDialog.isShowing())
//				mProgressDialog.dismiss();
//		}
		if (result != null && !result.startsWith("error")) { //$NON-NLS-1$

			Log.d("flickr", "post Execute - Uri : " + Uri.parse(result));
			Log.d("flickr", "mContext has data? "+ ((FlickrjActivity)mContext).path +  ((FlickrjActivity)mContext).title  );

			mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result)));

		} else {
			System.out.println("---------oauthtask result-----------"+result);

			Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();

		}
	}

}
