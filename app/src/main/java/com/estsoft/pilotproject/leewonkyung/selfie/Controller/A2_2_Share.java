package com.estsoft.pilotproject.leewonkyung.selfie.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.flickrhelpers.FlickrjActivity;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

import java.io.File;


public class A2_2_Share extends Activity {

    Context mContext = null;
    Activity mActivity = null;



    private static final String API_KEY = "274e1d3e1b7f14ab86b219d152a63755"; //$NON-NLS-1$
    public static final String API_SEC = "658cd23cf393b53d"; //$NON-NLS-1$

    static final int CHOOSE_GALLERY_REQUEST = 102;
    static final int CAMERA_PIC_REQUEST = 103;
    Uri mCapturedImageURI;

    File fileUri;
    private EditText et_ImageName;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = A2_2_Share.this;
        mActivity = this;
        setContentView(R.layout.a2_2_share);
        Button btnFlickr = (Button) findViewById(R.id.button_upload);
        btnFlickr.setOnClickListener(mFlickrClickListener);

        Button btnPick = (Button) findViewById(R.id.button_pick);
        btnPick.setOnClickListener(mPickClickListener);

        et_ImageName = (EditText) findViewById(R.id.editText_imageName);
    }



    View.OnClickListener mFlickrClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (fileUri == null) {
                Toast.makeText(getApplicationContext(),
                        "Please pick photo", Toast.LENGTH_SHORT).show();

                return;
            }

            Intent intent = new Intent(getApplicationContext(),
                    FlickrjActivity.class);
            intent.putExtra("flickImagePath", fileUri.getAbsolutePath());
            intent.putExtra("flickrImageName", et_ImageName.getText()
                    .toString());
            startActivity(intent);
        }
    };


    View.OnClickListener mPickClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Choose photo source");
            builder.setMessage("Choose your photo from?");
            builder.setCancelable(true);

            builder.setPositiveButton("From Gallery",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivityForResult(intent,
                                    CHOOSE_GALLERY_REQUEST);
                        }
                    });

            builder.setNegativeButton("From Camera",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.MediaColumns.TITLE, "ImageTemp");
                            mCapturedImageURI =getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            Intent intentPicture = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                            intentPicture.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                            startActivityForResult(intentPicture, CAMERA_PIC_REQUEST);
                        }
                    });

            Dialog chooseDialog = builder.create();
            chooseDialog.show();
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_GALLERY_REQUEST
                || requestCode == CAMERA_PIC_REQUEST) {

            if (resultCode == Activity.RESULT_OK && data != null) {

                Uri tmp_fileUri = data.getData();

                if (tmp_fileUri == null) {
                    tmp_fileUri = mCapturedImageURI;
                }

                System.out.println("-----uri is----" + tmp_fileUri);

                ((ImageView) findViewById(R.id.imageView1))
                        .setImageURI(tmp_fileUri);

                String selectedImagePath = getPath(tmp_fileUri);
                fileUri = new File(selectedImagePath);
                Log.e("", "fileUri : " + fileUri.getAbsolutePath());
            }

            else if (resultCode == Activity.RESULT_OK && data == null) {

                Uri tmp_fileUri = mCapturedImageURI;
                System.out.println("-----uri is----" + tmp_fileUri);

                ((ImageView) findViewById(R.id.imageView1))
                        .setImageURI(tmp_fileUri);

                String selectedImagePath = getPath(tmp_fileUri);
                fileUri = new File(selectedImagePath);
                Log.e("", "fileUri : " + fileUri.getAbsolutePath());
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.MediaColumns.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = this.managedQuery(uri, projection, null, null,
                null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




}