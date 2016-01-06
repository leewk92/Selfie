package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by LeeWonKyung on 2015-12-14.
 */
public class BitmapHelper {

  /**
   * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates more memory
   * that there is already allocated.
   *
   * @param bitmap - Source image. It will be released, and should not be used more
   * @return a copy of imgIn, but muttable.
   */
  public static Bitmap convertToMutable(Bitmap bitmap) {
    try {
      //this is the file going to use temporally to save the bytes.
      // This file will not be a image, it will store the raw image data.
      File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

      //Open an RandomAccessFile
      //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
      //into AndroidManifest.xml file
      RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

      // get the width and height of the source bitmap.
      final int width = bitmap.getWidth();
      final int height = bitmap.getHeight();
      final Bitmap.Config type = bitmap.getConfig();

      //Copy the byte to the file
      //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
      FileChannel channel = randomAccessFile.getChannel();
      MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, bitmap.getRowBytes() * height);
      bitmap.copyPixelsToBuffer(map);
      //recycle the source bitmap, this will be no longer used.
      bitmap.recycle();
      //Create a new bitmap to load the bitmap again. Probably the memory will be available.
      bitmap = Bitmap.createBitmap(width, height, type);
      map.position(0);
      //load it back from temporary
      bitmap.copyPixelsFromBuffer(map);
      //close the temporary file and channel , then delete that also
      channel.close();
      randomAccessFile.close();

      // delete the temp file
      file.delete();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return bitmap;
  }

  /**
   * Convert size of bitmap with scaling to target height and width.
   * @param bitmap
   * @param newHeight
   * @param newWidth
   * @return resized bitmap
   */
  static public Bitmap getResizedBitmap(Bitmap bitmap, int newHeight, int newWidth) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();
    final float scaleWidth = ((float) newWidth) / width;
    final float scaleHeight = ((float) newHeight) / height;
    // create a matrix for the manipulation
    Matrix matrix = new Matrix();
    // resize the bit map
    matrix.postScale(scaleWidth, scaleHeight);
    // recreate the new Bitmap
    final Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
    return resizedBitmap;
  }

  /**
   * rotate bitmap about given degrees
   * @param bitmap
   * @param degrees
   * @return rotated bitmap
   */
  static public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) {

    Matrix matrix = new Matrix();
    matrix.setRotate(360 - degrees);
    final Bitmap bmpRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    return bmpRotated;
  }


  static public File getResizedBitmapFile(Context mContext, int targetW, int targetH, String imagePath) {

    // Get the dimensions of the bitmap
    BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
    //inJustDecodeBounds = true <-- will not load the bitmap into memory
    bmpOptions.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imagePath, bmpOptions);
    final int photoW = bmpOptions.outWidth;
    final int photoH = bmpOptions.outHeight;

    // Determine how much to scale down the image
    final int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

    // Decode the image file into a Bitmap sized to fill the View
    bmpOptions.inJustDecodeBounds = false;
    bmpOptions.inSampleSize = scaleFactor;
    bmpOptions.inPurgeable = true;

    Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmpOptions);
    File imageFile = new File(mContext.getCacheDir() + File.separator + "uploadfile.jpg");
    FileOutputStream fileOutputStream = null;

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);        //quality : 0 to 100
    byte[] imageByteArray = stream.toByteArray();

    try {
      fileOutputStream = new FileOutputStream(imageFile);
      fileOutputStream.write(imageByteArray); // fixed : not flipped
      // output.write(bytes);           // original : flipped
      fileOutputStream.close();
      Log.i("Image Downscale", "success ! ");
    } catch (IOException e) {
      e.printStackTrace();
    }

    return imageFile;
  }


  static public Bitmap loadBitmapFromView(View v) {
    final int width = v.getWidth();
    final int height = v.getHeight();
    final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
    v.draw(canvas);
    return bitmap;
  }

  static public Uri getUriFromImageView(Activity activity, ImageView imageView) {

    OutputStream fileOutputStream = null;
    Uri outputFileUri = null;
    Bitmap tmpBitmap = BitmapHelper.loadBitmapFromView(imageView);
    try { // TODO: 'root' variable must be used on third line but it's not related to exception
          // TODO: it is okay to locate inside of 'try' statement?
      File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Pictures" + File.separator + "Decorated" + File.separator);
      root.mkdirs();
      File sdImageMainDirectory = File.createTempFile("photoview", ".jpg", root);
      outputFileUri = Uri.fromFile(sdImageMainDirectory);

      String outputFilePath = sdImageMainDirectory.getPath();
      fileOutputStream = new FileOutputStream(sdImageMainDirectory);

      // make visible on gallery app
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.DATA, outputFilePath);
      values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
      activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      tmpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);

    } catch (IOException e) {
      // this error occurred when createTempFile method 'or' FileOutputStream method has filepath not exist.
      // TODO: How to maintain Atomicity of Exception while two or more methods throw same exception?
      ToastHelper.showToast(activity, "GetUri error occured. Please try again later.");
      e.printStackTrace();
    } finally{
      if(null != fileOutputStream) {
        try {
          fileOutputStream.flush();
          fileOutputStream.close();
        } catch (IOException e){
          e.printStackTrace();
        }
      }
    }

    return outputFileUri;

  }

}