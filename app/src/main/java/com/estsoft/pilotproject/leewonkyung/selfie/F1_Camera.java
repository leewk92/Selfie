/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.estsoft.pilotproject.leewonkyung.selfie;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.estsoft.pilotproject.leewonkyung.selfie.Util.CompareSizesByArea;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.ErrorDialog;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.ImageSaver;
import com.estsoft.pilotproject.leewonkyung.selfie.Util.OnSwipeTouchListener;

public class F1_Camera extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback, SensorEventListener {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private static final String TAG = "F1_Camera";
    private static final int STATE_PREVIEW = 0; //Showing camera preview.
    private static final int STATE_WAITING_LOCK = 1; // Waiting for the focus to be locked.
    private static final int STATE_WAITING_PRECAPTURE = 2; // Waiting for the exposure to be precapture state.
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;  //  Waiting for the exposure state to be something other than precapture.
    private static final int STATE_PICTURE_TAKEN = 4;       //taken
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private int STATE_FACING = CameraCharacteristics.LENS_FACING_FRONT;

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int FROM_RADS_TO_DEGS = -57;
    private int mCurrentOrientation = 0;
    private int mPreOrientation = 0;
    private int mCurrentEffect = 0; // 0; off.
    private int surfaceWidth=0;
    private int surfaceHeight=0;
    private String mEffectArray[]; // save names of effects
    ImageButton btn_takePicture;
    ImageButton btn_info;
    Animation anim_rotateLeft;
    Animation anim_rotateRight;


    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
            surfaceWidth = width;
            surfaceHeight = height;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private String mCameraId; //ID of the currentCameraDevice
    private AutoFitTextureView mTextureView; //for camera preview.
    private CameraCaptureSession mCaptureSession;  // CameraCaptureSession for camera preview.
    private CameraDevice mCameraDevice; //  A reference to the opened CameraDevice
    private Size mPreviewSize; // size of camera preview.
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Activity mActivity;
    private HandlerThread mBackgroundThread; //An additional thread for running tasks that shouldn't block the UI.
    private Handler mBackgroundHandler; //for running tasks in the background.
    private ImageReader mImageReader; // handles still image capture.
    private File mFile; //This is the output file for our picture.
    private ImageSaver mImageSaver;
    private Bitmap outputBitmap;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mImageSaver = new ImageSaver(reader.acquireNextImage(), mFile, mCurrentOrientation);
            mBackgroundHandler.post(mImageSaver);
            outputBitmap = mImageSaver.getOutputBitmap();
        }

    };

    private CaptureRequest.Builder mStillCaptureRequestBuilder;
    private CaptureRequest mStillCaptureRequest;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCharacteristics mCharacteristics;
    private int mState = STATE_PREVIEW; //The current state of camera state for taking pictures.
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);  //to prevent the app from exiting before closing the camera.

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.

                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE:{
                    gotoPreviewState();
                    break;
                }
            }
        }
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            if(mState == STATE_WAITING_PRECAPTURE)
                mState = STATE_PREVIEW;
            else
                process(result);
        }

    };


    public static F1_Camera newInstance() {
        return new F1_Camera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.f1_camera, container, false);

    }

    int clamp(int tmp, int min, int max){
        if(tmp < min)
            tmp = min;
        else if (tmp > max){
            tmp = max;
        }
        return tmp;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        mActivity = this.getActivity();
        btn_takePicture = (ImageButton)view.findViewById(R.id.picture);
        btn_info = (ImageButton) view.findViewById(R.id.info);
        btn_takePicture.setOnClickListener(this);
        btn_info.setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);


        mTextureView.setOnTouchListener(new OnSwipeTouchListener(this.getActivity().getApplicationContext()){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return super.onTouch(v, event);
            }

            @Override
            public void onSwipeRight() {
                 nextEffect();
                gotoPreviewState();
                showToast(mEffectArray[mCurrentEffect]);

            }

            @Override
            public void onSwipeLeft() {
                prevEffect();
                gotoPreviewState();
                showToast(mEffectArray[mCurrentEffect]);
            }

            @Override
            public void onActionDown(MotionEvent event) {
                super.onActionDown(event);
                Rect rect=mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Log.i("onAreaTouchEvent","SENSOR_INFO_ACTIVE_ARRAY_SIZE,,,,,,,,rect.left--->" + rect.left + ",,,rect.top--->"+ rect.top+ ",,,,rect.right--->"+ rect.right+ ",,,,rect.bottom---->"+ rect.bottom);
                Size size=mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                Log.i("onAreaTouchEvent","mCameraCharacteristics,,,,size.getWidth()--->" + size.getWidth() + ",,,size.getHeight()--->"+ size.getHeight());
                int areaSize=200;
                int right=rect.right;
                int bottom=rect.bottom;
                int viewWidth=mTextureView.getWidth();
                int viewHeight=mTextureView.getHeight();
                int ll, rr;
                Rect newRect;
                int centerX=(int)event.getX();
                int centerY=(int)event.getY();
                ll=((centerX * right) - areaSize) / viewWidth;
                rr=((centerY * bottom) - areaSize) / viewHeight;
                int focusLeft=clamp(ll,0,right);
                int focusBottom=clamp(rr,0,bottom);
                Log.i("focus_position","focusLeft--->" + focusLeft + ",,,focusTop--->"+ focusBottom+ ",,,focusRight--->"+ (focusLeft + areaSize)+ ",,,focusBottom--->"+ (focusBottom + areaSize));
                newRect=new Rect(focusLeft,focusBottom,focusLeft + areaSize,focusBottom + areaSize);
                MeteringRectangle meteringRectangle=new MeteringRectangle(newRect,500);
                MeteringRectangle[] meteringRectangleArr={meteringRectangle};

                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,meteringRectangleArr);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,meteringRectangleArr);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                gotoPreviewState();

            }
        });




        try {
            mSensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
        } catch (Exception e) {
            Log.d(TAG, "Hardware compatibility issue");
        }
//        anim_rotateLeft = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_left);
//        anim_rotateRight = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_right);
//        anim_rotateLeft = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_left);
//        anim_rotateRight = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_right);
        anim_rotateLeft = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_left);
        anim_rotateLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
//                switch (mCurrentOrientation){
//                    case 0:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_0);
//                        Log.d(TAG,"shutter 0 ");
//                        break;
//                    case 90:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_90);
//                        Log.d(TAG, "shutter 90 ");
//                        break;
//                    case 180:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_180);
//                        Log.d(TAG,"shutter 180 ");
//                        break;
//                    case 270:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_270);
//                        Log.d(TAG,"shutter 270 ");
//                        break;
//
//                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim_rotateRight = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_right);
        anim_rotateRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
//                switch (mCurrentOrientation){
//                    case 0:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_0);
//                        break;
//                    case 90:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_90);
//                        break;
//                    case 180:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_180);
//                        break;
//                    case 270:
//                        btn_takePicture.setImageResource(R.drawable.icon_shutter_270);
//                        break;
//
//                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null) , "pic.jpg");

    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }
    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

//    private void requestCameraPermission() {
//        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
//        } else {
//            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
//                    REQUEST_CAMERA_PERMISSION);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);
                mCharacteristics = characteristics;
                // We don't use a back facing camera in this app.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing !=STATE_FACING) {

                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // added by wk. confirm that applying effects on this camera hardware is able
                Log.d("effect list : ", characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS).length + "");
                mEffectArray = new String[characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS).length];
                mEffectArray[0] = "OFF";
                mEffectArray[1] = "MONO";
                mEffectArray[2] = "NEGATIVE";
                mEffectArray[3] = "SOLARIZE";
                mEffectArray[4] = "SEPIA";
                mEffectArray[5] = "POSTERIZE";
                mEffectArray[6] = "WHITEBOARD";
                mEffectArray[7] = "BLACKBOARD";
                mEffectArray[8] = "AQUA";

                int modes[] = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                for (int i = 0; i < modes.length; i++)
                    Log.d("modes", i + " : " + modes[i]);

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener( mOnImageAvailableListener, mBackgroundHandler);

                ///
                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                boolean swappedDimensions = true;
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                ///


 //               mPreviewSize = largest;

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }


    private void openCamera(int width, int height) {
        /*if (getActivity().checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }*/
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Log.d(TAG, "CameraId : " +  mCameraId);
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
       // int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
       // Log.d(TAG, "sensorOrientation = " + sensorOrientation);

//        if (sensorOrientation == 90 || sensorOrientation == 270) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / mPreviewSize.getHeight(),
//                    (float) viewWidth / mPreviewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (1), centerX, centerY);
//        } else if (sensorOrientation == 0 ) {
//            matrix.postRotate(180, centerX, centerY);
//        }else if (sensorOrientation == 180 ) {
//            matrix.postRotate(0, centerX, centerY);
//        }
        mTextureView.setTransform(matrix);
    }


    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            // added by wk, filtering
//                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CameraMetadata.CONTROL_EFFECT_MODE_AQUA);
                            // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY);
                            // mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);

                            // Auto focus should be continuous for camera preview.
/*                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // Flash is automatically enabled when necessary.
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

*/
                            // Finally, we start displaying the camera preview.
                            gotoPreviewState();

                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG,"Configure Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                 takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }

                break;
            }
        }
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        //lockFocus();
        captureStillPicture();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
//    private void lockFocus() {
//        try {
//            // This is how to tell the camera to lock focus.
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CameraMetadata.CONTROL_AF_TRIGGER_START);
//            // Tell #mCaptureCallback to wait for the lock.
//            mState = STATE_WAITING_LOCK;
//            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
//                    mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }



//
//    private void captureStillPicture() {
//        try {
//            final Activity activity = getActivity();
//            if (null == activity || null == mCameraDevice) {
//                return;
//            }
//            // This is the CaptureRequest.Builder that we use to take a picture.
//            final CaptureRequest.Builder captureBuilder =
//                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(mImageReader.getSurface());
//
//            // Use the same AE and AF modes as the preview.
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//
//            captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
//            // Orientation
////            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
////            int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//            Log.d(TAG, "mCurrentOrientation : " + mCurrentOrientation);
//          //  captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ); // not working.
//
//            CameraCaptureSession.CaptureCallback CaptureCallback
//                    = new CameraCaptureSession.CaptureCallback() {
//
//                @Override
//                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
//                                               @NonNull CaptureRequest request,
//                                               @NonNull TotalCaptureResult result) {
//                    showToast("Saved: " + mFile);
//                    Log.d(TAG, mFile.toString());
//
//                    Intent intent = new Intent(mActivity , A2_EditPhoto.class);
//                    intent.putExtra("image_filepath", mImageSaver.getOutputFilepath());
//                    startActivity(intent);
//                    // gotoPreviewState();
//
//                }
//            };
//            mCaptureSession.stopRepeating();
//            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

// for test, wk

    private void preCaptureStillPicture() throws CameraAccessException {
        mStillCaptureRequestBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        mStillCaptureRequestBuilder.addTarget(mImageReader.getSurface());

        // Use the same AE and AF modes as the preview.
        mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

    }


    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.

            preCaptureStillPicture();

            mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
            // Orientation
//            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//            int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.d(TAG, "mCurrentOrientation : " + mCurrentOrientation);
            //  captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ); // not working.

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        mCaptureSession.stopRepeating();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());

                    Intent intent = new Intent(mActivity , A2_EditPhoto.class);
                    intent.putExtra("image_filepath", mImageSaver.getOutputFilepath());
                    startActivity(intent);
                    // gotoPreviewState();

                }
            };

            mCaptureSession.capture(mStillCaptureRequestBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void gotoPreviewState() {
        try {
            // Reset the auto-focus trigger
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
            mPreviewRequest = mPreviewRequestBuilder.build();
          //  mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.stopRepeating();
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // for obtaining orientation although orientation of app is locked to landscape.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
        float roll = orientation[2] * FROM_RADS_TO_DEGS;
//        Log.d(TAG, "Pitch : " + pitch);
//        Log.d(TAG, "Roll : " + roll);
        mPreOrientation = mCurrentOrientation;   // temp saved value
        if( -45 <= roll && roll < 45 ) {
            mCurrentOrientation = 0;

        }else if(45 <= roll && roll < 135 ){
            mCurrentOrientation = 90;

        }else if (-135 <= roll && roll <-45  ){
            mCurrentOrientation = 270;

        }else{
            mCurrentOrientation = 180;
        }
        // button rotation.
        if((mPreOrientation==0 && mCurrentOrientation==90) ||
                (mPreOrientation==90 && mCurrentOrientation==180) ||
                (mPreOrientation==180 && mCurrentOrientation==270) ||
                (mPreOrientation==270 && mCurrentOrientation==0)){
            btn_takePicture.startAnimation(anim_rotateRight);

        }else if((mPreOrientation==90 && mCurrentOrientation==0) ||
                (mPreOrientation==180 && mCurrentOrientation==90) ||
                (mPreOrientation==270 && mCurrentOrientation==180) ||
                (mPreOrientation==0 && mCurrentOrientation==270)){
            btn_takePicture.startAnimation(anim_rotateLeft);
        }
    }










    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void nextEffect(){
        this.mCurrentEffect = (mCurrentEffect + 1)%9;
    }
    public void prevEffect(){
        this.mCurrentEffect  =  mCurrentEffect - 1;
        if(mCurrentEffect == -1)
            mCurrentEffect = 8;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }



}
