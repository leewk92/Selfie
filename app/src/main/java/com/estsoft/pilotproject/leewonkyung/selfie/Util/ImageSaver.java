package com.estsoft.pilotproject.leewonkyung.selfie.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
public class ImageSaver implements Runnable {

    /**
     * The JPEG image
     */
    private final Image mImage;
    /**
     * The file we save the image into.
     */
    private final File mFile;

    private Bitmap outputBitmap;

    int mCurrentOrientation ;
    public ImageSaver(Image image, File file, int currentOrientation) {
        mImage = image;
        mFile = file;
        mCurrentOrientation = currentOrientation;
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // flip horizontally.
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap flipped =  flipAndRotate(bmp);

        outputBitmap = flipped.copy( flipped.getConfig(), true);

        FileOutputStream output = null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        flipped.compress(Bitmap.CompressFormat.JPEG, 100, stream);        //quality : 0 to 100
        byte[] flippedImageByteArray = stream.toByteArray();

        try {
            output = new FileOutputStream(mFile);
            output.write(flippedImageByteArray); // fixed : not flipped
            // output.write(bytes);           // original : flipped
            Log.d("ImageSaver", "writeImage ! ");
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

    Bitmap flipAndRotate(Bitmap d)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);

        switch(mCurrentOrientation){
            case 0:
                m.postRotate(90);
                break;
            case 90:
                m.postRotate(0);
                break;
            case 270:
                m.postRotate(180);
                break;
            case 180:
                m.postRotate(270);

        }


        Bitmap src = d;
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return  dst;
    }

    public Bitmap getOutputBitmap() {
        return outputBitmap;
    }
    public String getOutputFilepath(){

        Log.d("fileAbsolutePath : ", mFile.getAbsolutePath());
        Log.d("filepath : ", mFile.getPath());
        Log.d("filename : ", mFile.getName());
        return mFile.getPath();
    }
}