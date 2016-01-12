package com.estsoft.pilotproject.leewonkyung.selfie.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.util.BitmapHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.FileHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.HTTPRestfulHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.SafeFaceDetector;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ToastHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.flickrhelpers.FlickrjActivity;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * If photo is taken on Camera fragment, it transfer the filepath of image to this activity
 * by using intent.putExtra
 */
public class HomeActivity extends Activity  {

  private Activity mActivity;
  private File mFile;
  private PhotoViewAttacher mAttacher;
  private String mFilename;
  private FrameLayout mLayoutShare;
  private String mCategory = "";
  private Bitmap mBitmap;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.home_activity);
    mActivity = this;
    mLayoutShare = (FrameLayout) findViewById(R.id.layout_share);
    final ImageView imageView = (ImageView) findViewById(R.id.iv_photo);
    final ImageButton btnEdit = (ImageButton) findViewById(R.id.btn_edit);
    final ImageButton btnShare = (ImageButton) findViewById(R.id.btn_share);
    final ImageButton btnCamera = (ImageButton) findViewById(R.id.btn_camera);
    final ImageButton btnSticker = (ImageButton) findViewById(R.id.btn_sticker);
    final ImageButton btnSave = (ImageButton) findViewById(R.id.btn_save);
    final Button btnSendOut = (Button) findViewById(R.id.btn_sendout);
    final Button btnFlickr = (Button) findViewById(R.id.btn_flickr);

    mFilename = getIntent().getStringExtra("image_filepath");
    try {   // async task 로 바꿔야함
      mFile = new File(mFilename);
      FileInputStream is = new FileInputStream(mFilename);
      mBitmap = BitmapFactory.decodeStream(is);
      is.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    imageView.setImageBitmap(mBitmap);

    mAttacher = new PhotoViewAttacher(imageView);

    final View.OnClickListener onClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mLayoutShare.setVisibility(View.INVISIBLE);
        switch (view.getId()) {
          case R.id.btn_edit: {
            clickEdit();
            break;
          }
          case R.id.btn_share: {
            clickShare();
            break;
          }
          case R.id.btn_camera: {
            clickCamera();
            break;
          }
          case R.id.btn_sticker: {
            clickSticker();
            break;
          }
          case R.id.btn_save: {
            clickSave();
            break;
          }
          case R.id.btn_sendout: {
            clickSendOut();
            break;
          }
          case R.id.btn_flickr: {
            clickFlickr();
            break;
          }
        }
      }
    };

    btnEdit.setOnClickListener(onClickListener);
    btnShare.setOnClickListener(onClickListener);
    btnCamera.setOnClickListener(onClickListener);
    btnSticker.setOnClickListener(onClickListener);
    btnSave.setOnClickListener(onClickListener);
    btnSendOut.setOnClickListener(onClickListener);
    btnFlickr.setOnClickListener(onClickListener);

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mAttacher.cleanup();
  }

  public static final String PREFS_NAME = "prefsname";
  public static final String KEY_OAUTH_TOKEN = "token";
  public static final String KEY_TOKEN_SECRET = "secret";
  public static final String KEY_USER_NAME = "name";
  public static final String KEY_USER_ID = "id";

  /**
   * Button actions are following
   */
  private void clickEdit(){
    Intent intent = new Intent(mActivity, EditColorActivity.class);
    intent.putExtra("image_filepath", mFilename);
    startActivity(intent);
  }

  private void clickShare(){
    mLayoutShare.setVisibility(View.VISIBLE);
  }
  private void clickCamera(){
    mFile = null;
    mActivity.finish();
  }

  private void clickSticker(){
    try {
      Intent intent = new Intent(mActivity, StickerActivity.class);
      intent.putExtra("image_filepath", mFile.getAbsolutePath());
      startActivity(intent);
    } catch (Throwable t) {
      t.printStackTrace();
      ToastHelper.showToast(this, "Error occurred while transfer image to sticker_activity activity");
    }
  }

  private void clickSave(){
    if (SafeFaceDetector.hasFace(this,mBitmap)) {
      savePictureToSelfieFolder();
    } else {     // if the photo hasn't any faces.
      sendRequestToServerAndGetCategory();
    }
  }

  private void savePictureToSelfieFolder(){
    final String outputPath = Environment.getExternalStorageDirectory() + File.separator + "Pictures" + File.separator + "Selfie" + File.separator;
    FileHelper.moveFile(mFile.getPath(), outputPath);
    ToastHelper.showToast(this, "picture is saved to selfie folder!");
    makeVisibleOnGalleryApp(outputPath);
  }

  private void makeVisibleOnGalleryApp(String imagePath){
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.DATA, imagePath + mFile.getName());
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
  }

  private void sendRequestToServerAndGetCategory(){
    final String url = "http://119.81.176.246:8000";
    File resizedFile = BitmapHelper.getResizedBitmapFile(this, 400, 400, mFile.getPath());    // for uploading fast
    HTTPRestfulHelperExtender httpRest = new HTTPRestfulHelperExtender(this, url, "POST", new Bundle(), resizedFile.getPath());
    httpRest.doExecution();
  }


  private void clickSendOut(){
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("image/jpeg");
    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
    startActivity(share);
  }

  private void clickFlickr(){

    Dialog chooseDialog;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Flickr Upload");
    builder.setMessage("Do you want to upload this photo to Flickr ?");
    builder.setCancelable(true);

    builder.setPositiveButton("Upload",
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            gotoFlickrjActivity();
          }

          private void gotoFlickrjActivity(){
            Intent intent = new Intent(HomeActivity.this, FlickrjActivity.class);
            intent.putExtra("flickr_image_filepath", mFile.getAbsolutePath());
            intent.putExtra("flickr_image_title", "hello,flickr!");
            startActivity(intent);
          }
        }

    );

    builder.setNegativeButton("Cancel",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Nothing.
          }
        }
    );
    chooseDialog = builder.create();
    chooseDialog.show();
  }


  private class HTTPRestfulHelperExtender extends HTTPRestfulHelper {

    // Constructor for POST
    public HTTPRestfulHelperExtender(Context mContext, String url, String HTTPRestType, Bundle inputBundle, String photo) {
      setContext(mContext);
      setUrl(url);
      setHTTPRestType(HTTPRestType);
      setInputBundle(inputBundle);
      setPhoto(photo);
      setTask(new HttpAsyncTaskExtenders());
      Log.i("HTTP Constructor url", url);
    }

    @Override
    public void doExecution() {
      mTask.execute(getUrl(), getHTTPRestType());
    }

    class HttpAsyncTaskExtenders extends HTTPRestfulHelper.HttpAsyncTask {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        dialog = ProgressDialog.show(mContext, "Auto Categorization", "Please wait...", true);
        super.onPreExecute();
      }

      @Override
      protected String doInBackground(String... strings) {
        String url = strings[0];
        setOutputString(POST(url, getInputBundle()));
        return getOutputString();
      }

      @Override
      protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // obtain the response of server.
        JSONObject outputJsonObject = getOutputJsonObject();
        try {
          mCategory = outputJsonObject.getString("category");
        } catch (JSONException e) {
          e.printStackTrace();
        } finally {
          String outputPath = Environment.getExternalStorageDirectory() + File.separator + "Pictures" + File.separator + mCategory + File.separator;
          FileHelper.moveFile(mFile.getPath(), outputPath);
          ToastHelper.showToast(mActivity, "picture is moved to category : " + mCategory + "!");
          makeVisibleOnGalleryApp(outputPath);
          if (dialog.isShowing()) {
            dialog.dismiss();
          }
        }
      }
    }
  }

}
