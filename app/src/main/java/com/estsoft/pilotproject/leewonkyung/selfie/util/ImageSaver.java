package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 * TODO: Recycling Bitmap File is not working!!
 */
public class ImageSaver implements Runnable {

  static private int ORIENTATION_BASED_ROTATION_DEGREE_0 = 90;
  static private int ORIENTATION_BASED_ROTATION_DEGREE_90 = 0;
  static private int ORIENTATION_BASED_ROTATION_DEGREE_180 = 270;
  static private int ORIENTATION_BASED_ROTATION_DEGREE_270 = 180;

  private final Image mImage; //jpeg image
  private final File mFile; // The file we save the image into.

  private Bitmap mOutputBitmap;
  private int mFacing; //front camera or back camera
  int mCurrentOrientation;

  // Constructor
  public ImageSaver(Image image, File file, int currentOrientation, int facing) {
    mImage = image;
    mFile = file;
    mCurrentOrientation = currentOrientation;
    mFacing = facing;
  }

  // TODO: bitmap recycle managemnet is required.
  @Override
  public void run() {
    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);

    // flip horizontally.
    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    Bitmap flipped;
    if (mFacing == CameraCharacteristics.LENS_FACING_FRONT) {
      flipped = flipAndRotate_front(bitmap);
    }
    else {
      flipped = flipAndRotate_back(bitmap);
    }
    if(!bitmap.isRecycled()) {
      bitmap.recycle();
    }

    mOutputBitmap = flipped.copy(flipped.getConfig(), true);

    FileOutputStream output = null;

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    flipped.compress(Bitmap.CompressFormat.JPEG, 100, stream);        //quality : 0 to 100
    byte[] flippedImageByteArray = stream.toByteArray();

    if(flipped.isRecycled()) {
      flipped.recycle();
    }

    try {
      output = new FileOutputStream(mFile);
      output.write(flippedImageByteArray); // fixed : flipped

    } catch (IOException e) {
      e.printStackTrace();

    } finally {
      mImage.close();
      if (null != output) {
        try {
          output.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  // TODO: How to recycle bitmaps ?

  /**
   * For back facing camera, we need to horizontal filp rotate image for saving in right orientation.
   * @param inputBitmap
   * @return
   */
  Bitmap flipAndRotate_back(Bitmap inputBitmap) {
    Matrix m = new Matrix();
    m.preScale(-1, 1);
    // Degrees are just camera hardware characteristic
    switch (mCurrentOrientation) {
      case 0:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_0);
        break;
      case 90:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_90);
        break;
      case 180:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_180);
        break;
      case 270:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_270);
        break;
    }
    Bitmap srcBitmap = inputBitmap;
    Bitmap dstBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), m, false);
    dstBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

    if(!inputBitmap.isRecycled()) {
      inputBitmap.recycle();
    }
    if(!srcBitmap.isRecycled()) {
      srcBitmap.recycle();
    }

    return dstBitmap;
  }

  /**
   * For front facing camera, we need to just rotate image for saving in right orientation.
   * @param inputBitmap
   * @return
   */
  Bitmap flipAndRotate_front(Bitmap inputBitmap) {
    Matrix m = new Matrix();
    // Degrees are just camera hardware characteristic
    switch (mCurrentOrientation) {
      case 0:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_0);
        break;
      case 90:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_90);
        break;
      case 180:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_180);
        break;
      case 270:
        m.postRotate(ORIENTATION_BASED_ROTATION_DEGREE_270);
        break;
    }
    Bitmap srcBitmap = inputBitmap;
    Bitmap dstBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), m, false);
    dstBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

    if(!inputBitmap.isRecycled()) {
      inputBitmap.recycle();
    }
    if(!srcBitmap.isRecycled()) {
      srcBitmap.recycle();
    }
    return dstBitmap;
  }

  public Bitmap getOutputBitmap() {
    return mOutputBitmap;
  }

  public String getOutputFilepath() {
    return mFile.getPath();
  }


}