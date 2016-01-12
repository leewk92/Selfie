package com.estsoft.pilotproject.leewonkyung.selfie.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.util.BitmapHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ColorFilterGenerator;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ToastHelper;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class EditColorActivity extends Activity {


  public static EditColorActivity newInstance() {
    return new EditColorActivity();
  }

  static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d";
  static final String SCALE_TOAST_STRING = "Scaled to: %.2ff";

  private Activity mActivity;
  private Uri mUri;
  private String mOutputFilePath;
  private String mInputFilePath;
  private File mFile;
  private FrameLayout mLayoutMenu;
  private FrameLayout mLayoutColor;
  private ImageView mImageView = null;
  private Bitmap mBitmap = null;
  private int mCurrentHue = 0;
  private int mCurrentSaturation = 0;
  private int mCurrentBrightness = 0;
  private int mCurrentContrast = 0;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.edit_color_activity);
    mActivity = this;
    mImageView = (ImageView) findViewById(R.id.img_photo);
    mLayoutMenu = (FrameLayout) findViewById(R.id.layout_menu);
    mLayoutColor = (FrameLayout) findViewById(R.id.layout_color);

    mInputFilePath = getIntent().getStringExtra("image_filepath");
    try { // asyncTask로 바꿔야함
      mFile = new File(mInputFilePath);
      Log.d("inputFilePath", mInputFilePath);
      FileInputStream is = new FileInputStream(mInputFilePath);
      mBitmap = BitmapFactory.decodeStream(is);
      is.close();
    } catch (FileNotFoundException e) {
      Log.e("Invalid Filepath : ", mInputFilePath);
      e.printStackTrace();
    } catch (IOException e){
      e.printStackTrace();
    }

    mUri = Uri.fromFile(new File(mInputFilePath));

    final SeekBar hueSeekbar = (SeekBar) findViewById(R.id.seekbar_hue);
    final SeekBar saturationSeekbar = (SeekBar) findViewById(R.id.seekbar_saturation);;
    final SeekBar contrastSeekbar = (SeekBar) findViewById(R.id.seekbar_contrast);
    final SeekBar brightnessSeekbar = (SeekBar) findViewById(R.id.seekbar_brightness);
    final ImageButton btnColor =  (ImageButton) findViewById(R.id.btn_color);
    final ImageButton btnCrop = (ImageButton) findViewById(R.id.btn_crop);
    final ImageButton btnRotate =  (ImageButton) findViewById(R.id.btn_rotate);
    final ImageButton btnBack = (ImageButton) findViewById(R.id.btn_back);
    final ImageButton btnSave = (ImageButton) findViewById(R.id.btn_save);

    final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener()
    {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        switch (seekBar.getId()) {
          case R.id.seekbar_hue:
            mCurrentHue = (progress - 50) * 360 / 100; // range : [-180 , 180]
            break;
          case R.id.seekbar_contrast:
            mCurrentContrast = (progress - 50) * 2; // range : [-100 , 100]
            break;
          case R.id.seekbar_brightness:
            mCurrentBrightness = (progress - 50) * 2;  // range : [-100 , 100]
            break;
          case R.id.seekbar_saturation:
            mCurrentSaturation = (progress - 50) * 2;  // range : [-100, 100]
            break;
        }
        mImageView.getDrawable().setColorFilter(ColorFilterGenerator.adjustColor(mCurrentBrightness, mCurrentContrast, mCurrentSaturation, mCurrentHue));
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    };

    final ImageButton.OnClickListener onClickListener = new ImageButton.OnClickListener() {

      @Override
      public void onClick(View v) {
        mLayoutColor.setVisibility(View.INVISIBLE);

        switch (v.getId()) {
          case R.id.btn_back:
            clickBack();
            break;
          case R.id.btn_color:
            clickColor();
            break;
          case R.id.btn_crop:
            clickCrop();
            break;
          case R.id.btn_rotate:
            clickRotate();
            break;
          case R.id.btn_save:
            clickSave();
            break;
          default:
            break;
        }

      }
    };

    hueSeekbar.setOnSeekBarChangeListener(onSeekBarChangeListener);
    saturationSeekbar.setOnSeekBarChangeListener(onSeekBarChangeListener);
    contrastSeekbar.setOnSeekBarChangeListener(onSeekBarChangeListener);
    brightnessSeekbar.setOnSeekBarChangeListener(onSeekBarChangeListener);

    btnColor.setOnClickListener(onClickListener);
    btnCrop.setOnClickListener(onClickListener);
    btnRotate.setOnClickListener(onClickListener);
    btnBack.setOnClickListener(onClickListener);
    btnSave.setOnClickListener(onClickListener);
  }

  /**
   * Functions for click buttons
   */
  private void clickBack(){
    finish();
  }
  private void clickColor(){
    mLayoutColor.setVisibility(View.VISIBLE);
  }
  private void clickCrop(){
    Crop.of(mUri, mUri).start(mActivity);
  }
  private void clickRotate(){
    mBitmap = BitmapHelper.getRotatedBitmap(mBitmap, 90);
    mImageView.setImageBitmap(mBitmap);

  }
  private void clickSave(){
    mUri = BitmapHelper.getUriFromImageView(mActivity, mImageView);
    mOutputFilePath = mUri.getPath();
    // TODO: Recycling Bitmap files. but not yet!
    gotoHomeActivity();
  }

  private void gotoHomeActivity() {
    Intent intent = new Intent(mActivity, HomeActivity.class);
    intent.putExtra("image_filepath", mOutputFilePath);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent result) {
    if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {

    } else if (requestCode == Crop.REQUEST_CROP) {
      handleCrop(resultCode, result);
    }
  }

  private void handleCrop(int resultCode, Intent result) {
    if (resultCode == RESULT_OK) {
      // TODO: save and put it on image view.
    } else if (resultCode == Crop.RESULT_ERROR) {
      ToastHelper.showToast(this, Crop.getError(result).getMessage());
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
  }

}
