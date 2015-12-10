package com.estsoft.pilotproject.leewonkyung.selfie.Controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.ColorFilterGenerator;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;


/**
 * Created by LeeWonKyung on 2015-12-10.
 */
public class A2_1_EditColor extends Activity {



    public static A2_1_EditColor newInstance() {
        return new A2_1_EditColor();
    }

    static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d";
    static final String SCALE_TOAST_STRING = "Scaled to: %.2ff";
    private Activity mActivity;
    private File mFile;
    private Uri mUri;
    private String mOutputFilePath;
    private String mInputFilePath;
    private Toast mCurrentToast;
    private Matrix mCurrentDisplayMatrix = null;

    private FrameLayout layout_menu;
    private FrameLayout layout_color;


    private SeekBar hueSeekbar;
    private SeekBar saturationSeekbar;
    private SeekBar contrastSeekbar;
    private SeekBar brightnessSeekbar;


    private Bitmap bmp = null;
    ImageView mImageView=null;
    ImageButton btnColor = null;
    ImageButton btnCrop = null;
    ImageButton btnRotate = null;
    ImageButton btnBack = null;
    ImageButton btnSave = null;

    private int currentHue = 0;
    private int currentSaturation = 0;
    private int currentBrightness = 0;
    private int currentContrast = 0;






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a2_1_editcolor);
        mActivity = this;
        mImageView = (ImageView) findViewById(R.id.editcolor_photo);
        layout_menu = (FrameLayout) findViewById(R.id.layout_menu);
        layout_color = (FrameLayout) findViewById(R.id.layout_color);       // invisible

        mInputFilePath = getIntent().getStringExtra("image_filepath");
        try {

            FileInputStream is = new FileInputStream(mInputFilePath);
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
       // mImageView.setImageBitmap(bmp);

        mUri = Uri.fromFile(new File(mInputFilePath));


        hueSeekbar = (SeekBar)findViewById(R.id.hue_seekbar);
        saturationSeekbar = (SeekBar)findViewById(R.id.saturation_seekbar);
        contrastSeekbar = (SeekBar)findViewById(R.id.contrast_seekbar);
        brightnessSeekbar = (SeekBar)findViewById(R.id.brightness_seekbar);
        btnColor = (ImageButton)findViewById(R.id.btn_color);
        btnCrop = (ImageButton)findViewById(R.id.btn_crop);
        btnRotate = (ImageButton)findViewById(R.id.btn_rotate);
        btnBack = (ImageButton)findViewById(R.id.btn_back);
        btnSave = (ImageButton)findViewById(R.id.btn_save);

        SeekBar_Listener sl = new SeekBar_Listener();
        hueSeekbar.setOnSeekBarChangeListener(sl);
        saturationSeekbar.setOnSeekBarChangeListener(sl);
        contrastSeekbar.setOnSeekBarChangeListener(sl);
        brightnessSeekbar.setOnSeekBarChangeListener(sl);
        btnColor.setOnClickListener(new ImageButton_Listener());
        btnCrop.setOnClickListener(new ImageButton_Listener());
        btnRotate.setOnClickListener(new ImageButton_Listener());
        btnBack.setOnClickListener(new ImageButton_Listener());
        btnSave.setOnClickListener(new ImageButton_Listener());


    }


    private class ImageButton_Listener implements ImageButton.OnClickListener {

        @Override
        public void onClick(View v) {

            switch(v.getId()){

                case R.id.btn_back:
                    try{
                        new File(mOutputFilePath).delete();
                    }catch(Exception e){

                    }
                    finish();

                    break;


                case R.id.btn_color:
                    layout_color.setVisibility(View.VISIBLE);

                    break;
                case R.id.btn_crop :

                    layout_color.setVisibility(View.INVISIBLE);
                    //Crop.pickImage(mActivity);

                    Crop.of(mUri, mUri).start(mActivity);

                    break;
                case R.id.btn_rotate :
                    layout_color.setVisibility(View.INVISIBLE);

                    break;
                case R.id.btn_save:

                    getUriFromTemporaryBitmap();
                    Intent intent = new Intent(mActivity , A2_EditPhoto.class);
                    intent.putExtra("image_filepath", mOutputFilePath);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP );

                    startActivity(intent);
                    finish();
                    break;
            }

        }
    }

///

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {

        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }

    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Log.d("yeah","true");
           // mUri = Crop.getOutput(result);
           // mImageView.setImageURI(Crop.getOutput(result));


        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mImageView.setImageURI(mUri);
        mImageView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageView.setVisibility(View.INVISIBLE);
        mImageView.setImageResource(R.drawable.ic_flare_black_24dp);
    }

    ///


    private class SeekBar_Listener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            switch(seekBar.getId()) {
//                case R.id.hue_seekbar:
//                    mImageView.setColorFilter(ColorFilterGenerator.adjustHue(progress));
//                    break;
//                case R.id.contrast_seekbar:
//                    mImageView.setColorFilter(ColorFilterGenerator.adjustContrast(progress));
//                    break;
//                case R.id.brightness_seekbar:
//                    mImageView.setColorFilter(ColorFilterGenerator.adjustBrightness(progress));
//                    break;
//                case R.id.saturation_seekbar:
//                    mImageView.setColorFilter(ColorFilterGenerator.adjustSaturation(progress));
//                    break;
//            }

            switch(seekBar.getId()) {
                case R.id.hue_seekbar:
                    currentHue = (progress-50)*380/100 ;
                    break;
                case R.id.contrast_seekbar:
                    currentContrast = (progress-50)*2;
                    break;
                case R.id.brightness_seekbar:
                    currentBrightness =  (progress-50)*2;
                    break;
                case R.id.saturation_seekbar:
                    currentSaturation = (progress-50)*2;
                    break;
            }

            mImageView.getDrawable().setColorFilter(ColorFilterGenerator.adjustColor(currentBrightness, currentContrast, currentSaturation, currentHue));
            Log.d("ColorFilter", "B : " + currentBrightness + " C : " + currentContrast + " S : " + currentSaturation + " H : " + currentHue);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }




    private Uri getUriFromTemporaryBitmap(){

        OutputStream fOut = null;
        Uri outputFileUri = null;
        Bitmap tmpBitmap = loadBitmapFromView(mImageView);
        try {
            File root = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "Selfie" + File.separator);
            root.mkdirs();
            File sdImageMainDirectory = new File(root, "tmp.png");
            outputFileUri = Uri.fromFile(sdImageMainDirectory);

            mOutputFilePath = sdImageMainDirectory.getPath();
            fOut = new FileOutputStream(sdImageMainDirectory);
        } catch (Exception e) {
            Toast.makeText(this, "Error occured. Please try again later.",
                    Toast.LENGTH_SHORT).show();
        }

        try {
            tmpBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);


            fOut.flush();
            fOut.close();
        } catch (Exception e) {
        }
        return outputFileUri;

    }

    private Bitmap loadBitmapFromView(View v) {
        final int w = v.getWidth();
        final int h = v.getHeight();
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas c = new  Canvas(b);
        //v.layout(0, 0, w, h);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_a2_editphoto, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

}
