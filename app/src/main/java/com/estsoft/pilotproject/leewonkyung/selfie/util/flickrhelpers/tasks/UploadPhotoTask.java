/**
 *
 */
package com.estsoft.pilotproject.leewonkyung.selfie.util.flickrhelpers.tasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.util.flickrhelpers.FlickrHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.flickrhelpers.FlickrjActivity;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.uploader.UploadMetaData;

import java.io.File;
import java.io.FileInputStream;

public class UploadPhotoTask extends AsyncTask<OAuth, Void, String> {
  /**
   *
   */
  private final FlickrjActivity flickrjAndroidSampleActivity;
  private File file;
  private String title;

  // private final Logger logger = LoggerFactory
  // .getLogger(UploadPhotoTask.class);

  public UploadPhotoTask(FlickrjActivity flickrjAndroidSampleActivity,
                         File file, String title) {
    this.flickrjAndroidSampleActivity = flickrjAndroidSampleActivity;
    this.file = file;
    this.title = title;
  }

  private ProgressDialog mProgressDialog;

  @Override
  protected void onPreExecute() {
    super.onPreExecute();

    Log.d("Flickr", "Upload PreExecute attached");

    mProgressDialog = ProgressDialog.show(flickrjAndroidSampleActivity, "",
        "Uploading...");
    mProgressDialog.setCanceledOnTouchOutside(true);
    mProgressDialog.setCancelable(false);
    mProgressDialog.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dlg) {
        UploadPhotoTask.this.cancel(true);
      }
    });
  }

  @Override
  protected String doInBackground(OAuth... params) {
    OAuth oauth = params[0];
    OAuthToken token = oauth.getToken();

    try {
      Log.d("Flickr", "upload doInBackground attached");
      Flickr f = FlickrHelper.getInstance().getFlickrAuthed(
          token.getOauthToken(), token.getOauthTokenSecret());

      UploadMetaData uploadMetaData = new UploadMetaData();
      uploadMetaData.setTitle(title);

      Log.d("Flickr", "upload doInBackground attached2");

      return f.getUploader().upload(file.getName(),
          new FileInputStream(file), uploadMetaData);
    } catch (Exception e) {
      Log.e("boom!!", "" + e.toString());
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected void onPostExecute(String response) {

    Log.d("Flickr", "uploadPostExecute attached");

    if (mProgressDialog != null) {
      if (mProgressDialog.isShowing())
        mProgressDialog.dismiss();
    }

    if (response != null) {
      Log.e("", "" + response);
    } else {

    }

    if (monUploadDone != null) {
      monUploadDone.onComplete();
    }

    Toast.makeText(
        flickrjAndroidSampleActivity.getApplicationContext(),
        "Image successfully uploaded to flickr and the response code is:"
            + response, Toast.LENGTH_SHORT).show();

  }

  onUploadDone monUploadDone;

  public void setOnUploadDone(onUploadDone monUploadDone) {
    this.monUploadDone = monUploadDone;
  }

  public interface onUploadDone {
    void onComplete();
  }

}