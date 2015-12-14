package com.estsoft.pilotproject.leewonkyung.selfie.Controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.FileHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.HTTPRestfulUtilizer;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.SafeFaceDetector;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.FlickrjActivity;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

/**
 * Created by LeeWonKyung on 2015-12-07.
 */
public class A2_EditPhoto extends Activity implements View.OnClickListener {


    public static A2_EditPhoto newInstance() {
        return new A2_EditPhoto();
    }

    static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d";
    static final String SCALE_TOAST_STRING = "Scaled to: %.2ff";
    private File mFile;
    private TextView mCurrMatrixTv;
    private PhotoViewAttacher mAttacher;
    private Toast mCurrentToast;
    private Matrix mCurrentDisplayMatrix = null;
    private String mFilename;
    private FrameLayout mLayoutShare;
    private String mCategory = "";
    private Bitmap mBitmap;
    private ImageButton edit;
    private ImageButton share;
    private ImageButton camera;
    private ImageButton sticker;
    private ImageButton save;

    private Button sendOut;
    private Button flickr;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a2_editphoto);

        ImageView mImageView = (ImageView) findViewById(R.id.iv_photo);
        mCurrMatrixTv = (TextView) findViewById(R.id.tv_current_matrix);
        mLayoutShare = (FrameLayout) findViewById(R.id.layout_share);

//        Drawable bitmap = getResources().getDrawable(R.drawable.shutter);
//        mImageView.setImageDrawable(bitmap);
        mBitmap = null;
        mFilename = getIntent().getStringExtra("image_filepath");
        try {
            mFile = new File(mFilename);
            FileInputStream is = new FileInputStream(mFilename);
            mBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(mBitmap);

        // The MAGIC happens here!
        mAttacher = new PhotoViewAttacher(mImageView);

        // Lets attach some listeners, not required though!
        mAttacher.setOnMatrixChangeListener(new MatrixChangeListener());
        mAttacher.setOnPhotoTapListener(new PhotoTapListener());

        edit = (ImageButton) findViewById(R.id.edit);
        share = (ImageButton) findViewById(R.id.share);
        camera = (ImageButton) findViewById(R.id.camera);
        sticker = (ImageButton) findViewById(R.id.sticker);
        save = (ImageButton) findViewById(R.id.save);
        sendOut = (Button) findViewById(R.id.btn_sendout);
        flickr = (Button) findViewById(R.id.btn_flickr);


        edit.setOnClickListener(this);
        share.setOnClickListener(this);
        camera.setOnClickListener(this);
        sticker.setOnClickListener(this);
        save.setOnClickListener(this);
        sendOut.setOnClickListener(this);
        flickr.setOnClickListener(this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Need to call clean-up
        mAttacher.cleanup();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        return super.onPrepareOptionsMenu(menu);
    }


    private class PhotoTapListener implements OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            float xPercentage = x * 100f;
            float yPercentage = y * 100f;

            showToast(String.format(PHOTO_TAP_TOAST_STRING, xPercentage, yPercentage, view == null ? 0 : view.getId()));
        }
    }

    private void showToast(CharSequence text) {
        if (null != mCurrentToast) {
            mCurrentToast.cancel();
        }

        mCurrentToast = Toast.makeText(A2_EditPhoto.this, text, Toast.LENGTH_SHORT);
        mCurrentToast.show();
    }

    private class MatrixChangeListener implements OnMatrixChangedListener {

        @Override
        public void onMatrixChanged(RectF rect) {
            mCurrMatrixTv.setText(rect.toString());
        }
    }


    public static final String CALLBACK_SCHEME = "estsoft-pilotproject-leewonkyung-selfie";
    public static final String PREFS_NAME = "prefsname";
    public static final String KEY_OAUTH_TOKEN = "token";
    public static final String KEY_TOKEN_SECRET = "secret";
    public static final String KEY_USER_NAME = "name";
    public static final String KEY_USER_ID = "id";

    public void showPrefs() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String oauthTokenString = settings.getString(KEY_OAUTH_TOKEN, null);
        String tokenSecret = settings.getString(KEY_TOKEN_SECRET, null);
        if (oauthTokenString == null && tokenSecret == null) {
            Log.d("nothing", "");
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


        Log.d("flickr Edit", "PREFS oauthTokenString : " + oauthTokenString);
        Log.d("flickr Edit", "PREFS tokenSecret : " + tokenSecret);
        Log.d("flickr Edit", "PREFS userName : " + userName);
        Log.d("flickr Edit", "PREFS userId : " + userId);

    }

    @Override
    public void onClick(View view) {

        mLayoutShare.setVisibility(View.INVISIBLE);

        switch (view.getId()) {

            case R.id.edit: {

                try {
                    showPrefs();
//
//                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
//                    mFile = File.createTempFile("photoview", ".jpeg",
//                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//                    FileOutputStream out = new FileOutputStream(mFile);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                    out.close();

                    Intent intent = new Intent(this, A2_1_EditColor.class);
                    intent.putExtra("image_filepath", mFilename);
                    startActivity(intent);

                    //Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while transfer image to sticker activity", Toast.LENGTH_SHORT).show();
                }


                break;
            }

            case R.id.share: {
                mLayoutShare.setVisibility(View.VISIBLE);


                break;
            }

            case R.id.camera: {

                mFile = null;
                this.finish();

                break;
            }
            case R.id.sticker: {

                try {
//                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
//                    mFile = File.createTempFile("photoview", ".jpeg",
//                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//                    FileOutputStream out = new FileOutputStream(mFile);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                    out.close();

                    Intent intent = new Intent(this, A2_4_Sticker.class);
                    intent.putExtra("image_filepath", mFile.getAbsolutePath());
                    startActivity(intent);

                    //Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while transfer image to sticker activity", Toast.LENGTH_SHORT).show();
                }


                break;
            }
            case R.id.save: {

                if(hasFace()){
                    String outputPath = Environment.getExternalStorageDirectory() + File.separator + "Pictures" + File.separator + "Selfie" + File.separator;
                    FileHelper.moveFile(mFile.getPath(), outputPath );
                    Toast.makeText(this, "picture is saved to selfie folder!", Toast.LENGTH_SHORT).show();
                    // make visible on gallery app
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA , outputPath + mFile.getName());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }else {     // if the photo hasn't any faces.
                    //saveBitmapToFile();
                    String url = "http://119.81.176.246:8000";
                    HTTPRestfulUtilizerExtender a = new HTTPRestfulUtilizerExtender(this, url, "POST", new Bundle(), mFile.getPath());
                    a.doExecution();
                }



//                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                Uri contentUri = Uri.fromFile(mFile);
//                mediaScanIntent.setData(contentUri);
//                this.sendBroadcast(mediaScanIntent);


                break;
            }


            case R.id.btn_sendout: {

                //saveBitmapToFile();
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpeg");
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
                startActivity(share);
//
//                try {
//                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
//                    mFile = File.createTempFile("photoview", ".jpeg",
//                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//                    FileOutputStream out = new FileOutputStream(mFile);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                    out.close();
//
//                    Intent share = new Intent(Intent.ACTION_SEND);
//                    share.setType("image/jpeg");
//                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
//                    startActivity(share);
//
//                    Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                    Toast.makeText(this, "Error occured while sharing image", Toast.LENGTH_SHORT).show();
//                }

                break;
            }

            case R.id.btn_flickr: {

                //saveBitmapToFile();
                Dialog chooseDialog;
//                Intent intent = new Intent(this , A2_2_Share.class);
//               // intent.putExtra("image_filepath", mFile.getAbsolutePath());
//                startActivity(intent);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Flickr Upload");
                builder.setMessage("Do you want to upload this photo to Flickr ?");
                builder.setCancelable(true);

                builder.setPositiveButton("Upload",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(A2_EditPhoto.this,
                                        FlickrjActivity.class);


                                intent.putExtra("flickImagePath", mFile.getAbsolutePath());
                                intent.putExtra("flickrImageName", "hi");
                                startActivity(intent);

                            }
                        });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub

                            }
                        });

                chooseDialog = builder.create();
                chooseDialog.show();


                break;
            }


        }
    }


//    void saveBitmapToFile() {
//
//        try {
//            Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
//            mFile = File.createTempFile("photoview", ".jpeg",
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//            FileOutputStream out = new FileOutputStream(mFile);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            out.close();
//
//
//            Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
//        } catch (Throwable t) {
//            t.printStackTrace();
//            Toast.makeText(this, "Error occured while extracting bitmap", Toast.LENGTH_SHORT).show();
//        }
//
//    }




    // for 일정 올리기
    class HTTPRestfulUtilizerExtender extends HTTPRestfulUtilizer {


        // Constructor for POST
        public HTTPRestfulUtilizerExtender(Context mContext, String url, String HTTPRestType, Bundle inputBundle, String photo) {
            setmContext(mContext);
            setUrl(url);
            setHTTPRestType(HTTPRestType);
            setInputBundle(inputBundle);
            setPhoto(photo);
            task = new HttpAsyncTaskExtenders();
            Log.d("HTTP Constructor url", url);

            // new HttpAsyncTask().execute(url,HTTPRestType);
        }

        @Override
        public void doExecution() {
            task.execute(getUrl(), getHTTPRestType());
        }

        class HttpAsyncTaskExtenders extends HTTPRestfulUtilizer.HttpAsyncTask {

            @Override
            protected void onPreExecute() {
                Toast.makeText(getmContext(), "업로드합니다! ", Toast.LENGTH_SHORT).show();
                super.onPreExecute();

            }

            @Override
            protected String doInBackground(String... strings) {
                String url = strings[0];
                String sHTTPRestType = strings[1];


                setOutputString(POST(url, getInputBundle()));

                return getOutputString();

            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                JSONObject outputJsonObject = getOutputJsonObject();
                try {
                    mCategory = outputJsonObject.getString("category");
                    Log.d("category :", mCategory);
                    String outputPath = Environment.getExternalStorageDirectory()+ File.separator + "Pictures" + File.separator + mCategory + File.separator;
                    FileHelper.moveFile(mFile.getPath(), outputPath );
                    Toast.makeText(getmContext(), "picture is moved to category : " + mCategory + "!", Toast.LENGTH_SHORT).show();

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA , outputPath + mFile.getName());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    boolean hasFace(){

        boolean result = true;

        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        Detector<Face> safeDetector = new SafeFaceDetector(detector);

        // Create a frame from the bitmap and run face detection on the frame.
        Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
        SparseArray<Face> faces = safeDetector.detect(frame);

        if(faces.size() == 0)
            result = false;
        return result;
    }



}
