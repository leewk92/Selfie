package com.estsoft.pilotproject.leewonkyung.selfie.Controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.estsoft.pilotproject.leewonkyung.selfie.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    private ImageButton edit;
    private ImageButton share;
    private ImageButton camera;
    private ImageButton sticker;
    private ImageButton save;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a2_editphoto);

        ImageView mImageView = (ImageView) findViewById(R.id.iv_photo);
        mCurrMatrixTv = (TextView) findViewById(R.id.tv_current_matrix);

//        Drawable bitmap = getResources().getDrawable(R.drawable.shutter);
//        mImageView.setImageDrawable(bitmap);
        Bitmap bmp = null;
        String filename = getIntent().getStringExtra("image_filepath");
        try {

            FileInputStream is = new FileInputStream(filename);
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(bmp);

        // The MAGIC happens here!
        mAttacher = new PhotoViewAttacher(mImageView);

        // Lets attach some listeners, not required though!
        mAttacher.setOnMatrixChangeListener(new MatrixChangeListener());
        mAttacher.setOnPhotoTapListener(new PhotoTapListener());

        edit = (ImageButton)findViewById(R.id.edit);
        share = (ImageButton)findViewById(R.id.share);
        camera = (ImageButton)findViewById(R.id.camera);
        sticker = (ImageButton)findViewById(R.id.sticker);
        save = (ImageButton)findViewById(R.id.save);

        edit.setOnClickListener(this);
        share.setOnClickListener(this);
        camera.setOnClickListener(this);
        sticker.setOnClickListener(this);
        save.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_a2_editphoto, menu);
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
        MenuItem zoomToggle = menu.findItem(R.id.menu_zoom_toggle);
        assert null != zoomToggle;
        zoomToggle.setTitle(mAttacher.canZoom() ? R.string.menu_zoom_disable : R.string.menu_zoom_enable);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_zoom_toggle:
                mAttacher.setZoomable(!mAttacher.canZoom());
                return true;

            case R.id.menu_scale_fit_center:
                mAttacher.setScaleType(ImageView.ScaleType.FIT_CENTER);
                return true;

            case R.id.menu_scale_fit_start:
                mAttacher.setScaleType(ImageView.ScaleType.FIT_START);
                return true;

            case R.id.menu_scale_fit_end:
                mAttacher.setScaleType(ImageView.ScaleType.FIT_END);
                return true;

            case R.id.menu_scale_fit_xy:
                mAttacher.setScaleType(ImageView.ScaleType.FIT_XY);
                return true;

            case R.id.menu_scale_scale_center:
                mAttacher.setScaleType(ImageView.ScaleType.CENTER);
                return true;

            case R.id.menu_scale_scale_center_crop:
                mAttacher.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return true;

            case R.id.menu_scale_scale_center_inside:
                mAttacher.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                return true;

            case R.id.menu_scale_random_animate:
            case R.id.menu_scale_random:
                Random r = new Random();

                float minScale = mAttacher.getMinimumScale();
                float maxScale = mAttacher.getMaximumScale();
                float randomScale = minScale + (r.nextFloat() * (maxScale - minScale));
                mAttacher.setScale(randomScale, item.getItemId() == R.id.menu_scale_random_animate);

                showToast(String.format(SCALE_TOAST_STRING, randomScale));

                return true;
            case R.id.menu_matrix_restore:
                if (mCurrentDisplayMatrix == null)
                    showToast("You need to capture display matrix first");
                else
                    mAttacher.setDisplayMatrix(mCurrentDisplayMatrix);
                return true;
            case R.id.menu_matrix_capture:
                mCurrentDisplayMatrix = mAttacher.getDisplayMatrix();
                return true;
            case R.id.extract_visible_bitmap:
                try {
                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
                    File tmpFile = File.createTempFile("photoview", ".jpeg",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
                    FileOutputStream out = new FileOutputStream(tmpFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/jpeg");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
                    startActivity(share);
                    Toast.makeText(this, String.format("Extracted into: %s", tmpFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while extracting bitmap", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
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


    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.edit: {
                break;
            }

            case R.id.share: {

                try {
                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
                    mFile = File.createTempFile("photoview", ".jpeg",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
                    FileOutputStream out = new FileOutputStream(mFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/jpeg");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
                    startActivity(share);

                    Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while sharing image", Toast.LENGTH_SHORT).show();
                }

                break;
            }

            case R.id.camera: {

                this.finish();

                break;
            }
            case R.id.sticker: {

                try {
                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
                    mFile = File.createTempFile("photoview", ".jpeg",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
                    FileOutputStream out = new FileOutputStream(mFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();

                    Intent intent = new Intent(this , A2_4_Sticker.class);
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

                try {
                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
                    mFile = File.createTempFile("photoview", ".jpeg",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
                    FileOutputStream out = new FileOutputStream(mFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();

                    Toast.makeText(this, String.format("Extracted into: %s", mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while extracting bitmap", Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }
    }

}
