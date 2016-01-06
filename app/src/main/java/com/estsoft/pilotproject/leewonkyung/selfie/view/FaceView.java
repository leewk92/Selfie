package com.estsoft.pilotproject.leewonkyung.selfie.view;


import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.util.BitmapHelper;


/**
 * View which displays a bitmap containing a face along with overlay graphics that identify the
 * locations of detected facial landmarks.
 */
public class FaceView extends View {

  // this factor is obtained by trial and error method.
  static final private double ADJUST_COEFFICIENT_FOR_STICKERSIZE_X = 0.7;
  static final private double ADJUST_COEFFICIENT_FOR_STICKERSIZE_Y = 0.7;
  static final private double ADJUST_COEFFICIENT_FOR_STICKERLOCATION_X = 0.7 / 2;
  static final private double ADJUST_COEFFICIENT_FOR_STICKERLOCATION_Y = 0.8 / 2;

  private Bitmap mBitmap;
  private SparseArray<Face> mFaces;

  // Constructor
  public FaceView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Sets the bitmap background and the associated face detections.
   */
  public void setContent(Bitmap bitmap, SparseArray<Face> faces) {
    mBitmap = bitmap;
    mFaces = faces;
    invalidate();
  }

  /**
   * Draws the bitmap background and the associated face landmarks.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if ((mBitmap != null) && (mFaces != null)) {
      double scale = drawBitmap(canvas);
      // drawFaceAnnotations(canvas, scale);
      drawFaceDecoration(canvas, scale);
    }
  }

  /**
   * Draws the bitmap background, scaled to the device size.  Returns the scale for future use in
   * positioning the facial landmark graphics.
   */
  private double drawBitmap(Canvas canvas) {
    double viewWidth = canvas.getWidth();
    double viewHeight = canvas.getHeight();
    double imageWidth = mBitmap.getWidth();
    double imageHeight = mBitmap.getHeight();
    double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

    Rect destBounds = new Rect(0, 0, (int) (imageWidth * scale), (int) (imageHeight * scale));

    canvas.drawBitmap(mBitmap, null, destBounds, null);
    return scale;
  }

  /**
   * Draws a small circle for each detected landmark, centered at the detected landmark position.
   * Note that eye landmarks are defined to be the midpoint between the detected eye corner
   * positions, which tends to place the eye landmarks at the lower eyelid rather than at the pupil
   * position.
   */
  private void drawFaceAnnotations(Canvas canvas, double scale) {
    Paint paint = new Paint();
    paint.setColor(Color.GREEN);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(5);

    for (int i = 0; i < mFaces.size(); ++i) {
      Face face = mFaces.valueAt(i);

      for (Landmark landmark : face.getLandmarks()) {
        int cx = (int) (landmark.getPosition().x * scale);
        int cy = (int) (landmark.getPosition().y * scale);
        canvas.drawCircle(cx, cy, 10, paint);
        Log.d("face landmark type:", landmark.getType() + "(" + cx + "," + cy + ")");

      }
    }
  }

  /**
   * Draw a glasses sticker_activity on canvas.
   * Sticker size and location is adjusted by trial and error method.
   * @param canvas
   * @param scale
   */
  private void drawFaceDecoration(Canvas canvas, double scale) {
    Paint paint = new Paint();
    paint.setColor(Color.GREEN);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(5);

    Bitmap glasses = BitmapFactory.decodeResource(getResources(), R.drawable.shutter); //glasses
    Bitmap mutableBitmap_glasses = BitmapHelper.convertToMutable(glasses);

    for (int i = 0; i < mFaces.size(); ++i) {
      Face face = mFaces.valueAt(i);
      int noseX = 0;
      int noseY = 0;
      for (Landmark landmark : face.getLandmarks()) {

        int cx = (int) (landmark.getPosition().x * scale);
        int cy = (int) (landmark.getPosition().y * scale);
        Log.d("face ith landmark", landmark.getType() + "(" + cx + "," + cy + ")");
        if (landmark.getType() == Landmark.NOSE_BASE) {
          noseX = cx;
          noseY = cy;
        }
      }
      double faceWidth = face.getWidth();
      double faceHeight = face.getHeight();

      int adjustedStickerWidth =  (int) (faceWidth * ADJUST_COEFFICIENT_FOR_STICKERSIZE_X * faceHeight / faceWidth);
      int adjustedStickerHeight = (int) (faceWidth * ADJUST_COEFFICIENT_FOR_STICKERSIZE_Y);

      int adjustedStickerLocationX = (int) (noseX - face.getWidth() * ADJUST_COEFFICIENT_FOR_STICKERLOCATION_X);
      int adjustedStickerLocationY =(int) (noseY - face.getWidth() * ADJUST_COEFFICIENT_FOR_STICKERLOCATION_Y);

      Bitmap resizedBitmap_glasses = BitmapHelper.getResizedBitmap(mutableBitmap_glasses,adjustedStickerWidth, adjustedStickerHeight);

      // Rotate sticker_activity like face rotation.
      resizedBitmap_glasses = BitmapHelper.getRotatedBitmap(resizedBitmap_glasses, (int) face.getEulerZ());
      // Draw on canvas
      canvas.drawBitmap(resizedBitmap_glasses, adjustedStickerLocationX, adjustedStickerLocationY, null);

      if(mutableBitmap_glasses.isRecycled() == false){
        mutableBitmap_glasses.recycle();
      }
      if(glasses.isRecycled() == false){
        glasses.recycle();
      }
      if(resizedBitmap_glasses.isRecycled() == false){
        resizedBitmap_glasses.recycle();
      }
    }
  }


}