package com.estsoft.pilotproject.leewonkyung.selfie.View;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.BitmapHelper;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;


/**
 * View which displays a bitmap containing a face along with overlay graphics that identify the
 * locations of detected facial landmarks.
 */
public class FaceView extends View {
    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;

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
            drawFaceDecoration(canvas,scale);
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

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));

        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
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

    private void drawFaceDecoration(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        Bitmap glasses = BitmapFactory.decodeResource(getResources(), R.drawable.shutter);//glasses
        Bitmap mutableBitmap_glasses = BitmapHelper.convertToMutable(glasses);

//        Resources res = getResources();
//        BitmapDrawable bd = (BitmapDrawable)res.getDrawable(R.drawable.glasses);
//        Bitmap bit = bd.getBitmap();




        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);


            int nose_x=0;
            int nose_y=0;

            for (Landmark landmark : face.getLandmarks()) {

                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);

                Log.d("face ith landmark", landmark.getType() + "(" + cx + "," + cy+")");

                if(landmark.getType() == Landmark.NOSE_BASE){
                    nose_x = cx;
                    nose_y = cy;
                }
                // canvas.drawCircle(cx, cy, 10, paint);
            }

//            mutableBitmap_glasses.setWidth( (int)face.getWidth() );
//            mutableBitmap_glasses.setHeight( (int) ( face.getWidth() * glasses.getHeight() / glasses.getWidth()) );


            Bitmap resizedBitmap_glasses = BitmapHelper.getResizedBitmap(mutableBitmap_glasses,(int) ( face.getWidth()*0.7 * glasses.getHeight() / glasses.getWidth()), (int)(face.getWidth()*0.7));
            resizedBitmap_glasses =  BitmapHelper.getRotatedBitmap(resizedBitmap_glasses, (int) face.getEulerZ());
            Log.d("face width and height : ", face.getWidth() + ", " + face.getHeight());
            Log.d("glasses width and height : ", resizedBitmap_glasses.getWidth() + ", " + resizedBitmap_glasses.getHeight());


            canvas.drawBitmap(resizedBitmap_glasses, (int)( nose_x- face.getWidth()*0.7/2 ) , (int)(nose_y-face.getWidth()*0.8/2 ), null);

        }
    }

}