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

package com.estsoft.pilotproject.leewonkyung.selfie.controller;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
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

import com.estsoft.pilotproject.leewonkyung.selfie.R;
import com.estsoft.pilotproject.leewonkyung.selfie.util.CompareSizesByArea;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ErrorDialog;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ImageSaver;
import com.estsoft.pilotproject.leewonkyung.selfie.util.OnSwipeTouchListener;
import com.estsoft.pilotproject.leewonkyung.selfie.util.ToastHelper;
import com.estsoft.pilotproject.leewonkyung.selfie.view.AutoFitTextureView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;



/**
 * Implement and control camera. take a picture, change facing of camera
 * and transfer the filename of taken picture to next activity : HomeActivity
 */
public class Camera extends Fragment implements SensorEventListener {

  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  private static final int REQUEST_CAMERA_PERMISSION = 1;
  private static final String FRAGMENT_DIALOG = "dialog";
  private static final int ROTATION_DEGREE_0 = 90;
  private static final int ROTATION_DEGREE_90 = 0;
  private static final int ROTATION_DEGREE_180 = 270;
  private static final int ROTATION_DEGREE_270 = 180;

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, ROTATION_DEGREE_0);
    ORIENTATIONS.append(Surface.ROTATION_90, ROTATION_DEGREE_90);
    ORIENTATIONS.append(Surface.ROTATION_180, ROTATION_DEGREE_180);
    ORIENTATIONS.append(Surface.ROTATION_270, ROTATION_DEGREE_270);
  }

  private static final String TAG = "Camera";
  private static final int STATE_PREVIEW = 0; //Showing camera preview.
  private static final int STATE_WAITING_PRECAPTURE = 2; // Waiting for the exposure to be precapture state.
  private static final int MAX_PREVIEW_WIDTH = 1920;  // TODO: we have to know that this value is adjust to any hardwares
  private static final int MAX_PREVIEW_HEIGHT = 1080;
  private static final int SENSOR_DELAY = 500 * 1000; // 500ms
  private static final int FROM_RADS_TO_DEGS = -57;
  private static final int CAMERA_EFFECT_OFF = 0;

  private SensorManager mSensorManager;
  private Sensor mRotationSensor;
  private int mStateFacing = CameraCharacteristics.LENS_FACING_FRONT;
  private int mCurrentOrientation = 0;
  private int mPreOrientation = 0;
  private int mCurrentEffect = CAMERA_EFFECT_OFF;
  private String mEffectArray[]; // save names of effects
  private ImageButton mBtnTakePicture;
  private ImageButton mBtnSwitch;
  private Animation mAnimRotateLeft;  // for rotating buttons
  private Animation mAnimRotateRight;
  private Activity mActivity; // TODO: We need this member variable? final or not, is it reusable?
  private String mCameraId; //ID of the currentCameraDevice
  private AutoFitTextureView mTextureView; //for camera preview.
  private CameraCaptureSession mCaptureSession;  // CameraCaptureSession for camera preview.
  private CameraDevice mCameraDevice; //  A reference to the opened CameraDevice
  private Size mPreviewSize; // size of camera preview.
  private Semaphore mCameraOpenCloseLock = new Semaphore(1);  //to prevent the app from exiting before closing the camera.

  /**
   * {@link CameraDevice.StateCallback} handles several state events on a {@link CameraDevice}.
   */
  private final CameraDevice.StateCallback mStateCallback =
      new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
          mCameraOpenCloseLock.release();   // resource 해제
          mCameraDevice = cameraDevice;
          createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
          releaseAndCloseCamera(cameraDevice);
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
          releaseAndCloseCamera(cameraDevice);
          finishActivity();
        }

        private void finishActivity(){
          if (mActivity != null) {
            mActivity.finish();
          }
        }

        private void releaseAndCloseCamera(CameraDevice cameraDevice){
          mCameraOpenCloseLock.release();
          cameraDevice.close();
          mCameraDevice = null;
        }
      };

  private HandlerThread mBackgroundThread; //An additional thread for running tasks that shouldn't block the UI.
  private Handler mBackgroundHandler; //for running tasks in the background.
  private ImageReader mImageReader; // handles still image capture.
  private File mOutputPictureFile; //This is the output file for our picture.
  private ImageSaver mImageSaver;

  /**
   * {@link ImageReader.OnImageAvailableListener} tells {@link ImageSaver} image is savable
   * if image is available, post ImageSaver instance to mBackgroundHandler.
   */
  private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
      = new ImageReader.OnImageAvailableListener() {

    @Override
    public void onImageAvailable(ImageReader reader) {
      mImageSaver = new ImageSaver(reader.acquireNextImage(), mOutputPictureFile, mCurrentOrientation, mStateFacing);
      mBackgroundHandler.post(mImageSaver);
    }

  };

  private CaptureRequest.Builder mStillCaptureRequestBuilder;
  private CaptureRequest.Builder mPreviewRequestBuilder;
  private CaptureRequest mPreviewRequest;
  private CameraCharacteristics mCharacteristics;
  private int mState = STATE_PREVIEW; //The current state of camera state for taking pictures.

  /**
   * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
   */
  private CameraCaptureSession.CaptureCallback mCaptureCallback
      = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureProgressed(CameraCaptureSession session,
                                    CaptureRequest request,
                                    CaptureResult partialResult) {
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session,
                                   CaptureRequest request,
                                   TotalCaptureResult result) {
      if (STATE_WAITING_PRECAPTURE  == mState) {
        mState = STATE_PREVIEW;
      }
    }
  };

  /**
   * Public Static Factory Method
   * return this fragment instance with member variable facing which determine the facing of camera
   * @param facing
   * @return returnFragment : Camera() class instance with member parameter mStagefacing
   */
  public static Camera newInstance(int facing) {
    Camera returnFragment = new Camera();
    returnFragment.mStateFacing = facing;
    return returnFragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.camera, container, false);
  }

  @Override
  public void onViewCreated(final View view, Bundle savedInstanceState) {

    mActivity = getActivity();
    mBtnTakePicture = (ImageButton) view.findViewById(R.id.btn_take_picture);
    mBtnSwitch = (ImageButton) view.findViewById(R.id.btn_switch_camera);
    mTextureView = (AutoFitTextureView) view.findViewById(R.id.view_texture);
    mSensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
    mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);

    /**
     * {@link android.view.View.OnClickListener} handles events of clicking buttons
     * if mBtnTakePicture is clicked, takePicture() method is called
     * else if mBtnSwitch is clicked, switch the facing of camera ( backward or frontward )
     */
    final View.OnClickListener onClickListener =
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {

            switch (view.getId()) {
              case R.id.btn_take_picture: {
                try {
                  takePicture();
                }catch(CameraAccessException e){
                  e.printStackTrace();
                }
                break;
              }
              case R.id.btn_switch_camera: {
                switchCameraFacing();
                break;
              }
              default:{
                break;
              }
            }
          }

          private void switchCameraFacing(){
            if (CameraCharacteristics.LENS_FACING_FRONT == mStateFacing) {
              mStateFacing = CameraCharacteristics.LENS_FACING_BACK;
            } else {
              mStateFacing = CameraCharacteristics.LENS_FACING_FRONT;
            }
            getFragmentManager().beginTransaction().replace(R.id.container, Camera.newInstance(mStateFacing)).commit();
          }
        };

    /**
     * {@link OnSwipeTouchListener} handles swipe event to change filter to apply.
     * swipe to left : applying previous filter
     * swipe to right : applying next filter
     */
    final OnSwipeTouchListener swipeTouchListener =
        new OnSwipeTouchListener(getActivity().getApplicationContext()){

          @Override
          public boolean onTouch(View v, MotionEvent event) {
            return super.onTouch(v, event);
          }
          @Override
          public void onSwipeRight() {
            adjustNextEffect();
            gotoPreviewState();
            ToastHelper.showToast(mActivity, mEffectArray[mCurrentEffect]);
          }
          @Override
          public void onSwipeLeft() {
            adjustPrevEffect();
            gotoPreviewState();
            ToastHelper.showToast(mActivity,mEffectArray[mCurrentEffect]);
          }
          @Override
          public void onActionDown(MotionEvent event) {
            super.onActionDown(event);
          }
        };

    /**
     * {@link android.view.animation.Animation.AnimationListener} handle animation state
     * but we control nothing with this Listener
     */
    final Animation.AnimationListener animationListener =
        new Animation.AnimationListener() {
          @Override
          public void onAnimationStart(Animation animation) {
          }
          @Override
          public void onAnimationEnd(Animation animation) {
          }
          @Override
          public void onAnimationRepeat(Animation animation) {
          }
        };

    mBtnTakePicture.setOnClickListener(onClickListener);
    mBtnSwitch.setOnClickListener(onClickListener);
    mTextureView.setOnTouchListener(swipeTouchListener);

    mAnimRotateLeft = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_left);
    mAnimRotateRight = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_right);
    mAnimRotateRight.setAnimationListener(animationListener);
    mAnimRotateLeft.setAnimationListener(animationListener);

  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  /**
   * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
   * TextureView}.
   */
  final TextureView.SurfaceTextureListener mSurfaceTextureListener
      = new TextureView.SurfaceTextureListener() {

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
      openCameraForPreview(width, height);
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
      return true;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
    }

    private void openCameraForPreview(int width, int height){
      try {
        openCamera(width, height);
      } catch (CameraAccessException e) {
        // Camera is not available
        e.printStackTrace();
      } catch (RuntimeException e){
        // Couldn't open camera in 2.5sec
        e.printStackTrace();
      }
    }

  };

  @Override
  public void onResume() {
    super.onResume();

    String outputFileName = getActivity().getCacheDir() + File.separator + "pictureview" + System.currentTimeMillis() + ".jpg";
    mOutputPictureFile = new File(outputFileName);
    startBackgroundThread();

    // This comment is provided by com.example.android.camera2basic.Camera2BasicFragment.java.
    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).
    if (mTextureView.isAvailable()) {
      try {
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    } else {
      mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    closeCamera();
    stopBackgroundThread();
    mTexture.release();
    mSurface.release();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    if (REQUEST_CAMERA_PERMISSION == requestCode) {
      if (grantResults.length != 1 ||  grantResults[0] != PackageManager.PERMISSION_GRANTED ) {
        ErrorDialog.newInstance(getString(R.string.request_permission)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
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
   * @Throws if manager.getCameraIdList is failed, throws CameraAccessException.
   */
  private void setUpCameraOutputs(int width, int height) throws CameraAccessException {

    CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
    for (String cameraId : manager.getCameraIdList()) {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      mCharacteristics = characteristics;
      Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
      if (facing != null && facing != mStateFacing) {
        // we do not use the other facing camera.
        continue;
      }

      StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      if (map == null) {
        // it means that this camera is not available
        continue;
      }
      Log.i("effect list : ", mCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS).length + "");
      mEffectArray = new String[mCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS).length];
      mEffectArray[0] = "OFF";
      mEffectArray[1] = "MONO";
      mEffectArray[2] = "NEGATIVE";
      mEffectArray[3] = "SOLARIZE";
      mEffectArray[4] = "SEPIA";
      mEffectArray[5] = "POSTERIZE";
      mEffectArray[6] = "WHITEBOARD";
      mEffectArray[7] = "BLACKBOARD";
      mEffectArray[8] = "AQUA";

      // For still image captures, we use the largest available size.
      final Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

      // Check available camera resolutions
      for (int i = 0; i < map.getOutputSizes(ImageFormat.JPEG).length; i++) {
        Size tmpSize = map.getOutputSizes(ImageFormat.JPEG)[i];
        Log.i("sizes", "(" + tmpSize.getWidth() + "," +
            tmpSize.getHeight() + ")" + (double)tmpSize.getHeight() / (double)tmpSize.getWidth());
      }

      mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
      mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

      ///
      Point displaySize = new Point();
      mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
      int maxPreviewWidth = displaySize.x;
      int maxPreviewHeight = displaySize.y;

      if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
        maxPreviewWidth = MAX_PREVIEW_WIDTH;
      }

      if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
      }
      mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
          width, height, maxPreviewWidth, maxPreviewHeight, largest);

      Log.i("largest front", mPreviewSize.getWidth() + "," + mPreviewSize.getWidth());

      // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
      // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
      // garbage capture data.

      mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
      mCameraId = cameraId;
      return;
    }
  }


  /**
   * TODO: There are two excpetions. how to deal with them?
   * @param width
   * @param height
   */
  private void openCamera(int width, int height) throws CameraAccessException {
    setUpCameraOutputs(width, height);
    CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
    /**
     * Exception comment
     * if fail to open camera because of permission, throw CameraAccessException.
     * or if it takes 2.5 second to open camera , throw runtime error
     */
    try {
      mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
    }
    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      // Following method needs to check permissions above.
    }
    manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
  }

  /**
   * Closes the current {@link CameraDevice}.
   */
  private void closeCamera() {

    try {
      cameraOpenCloseLockAcquire();
      if ( mCaptureSession != null) {
        mCaptureSession.close();
        mCaptureSession = null;
      }
      if (mCameraDevice != null) {
        mCameraDevice.close();
        mCameraDevice = null;
      }
      if (mImageReader != null) {
        mImageReader.close();
        mImageReader = null;
      }
    } catch(RuntimeException e){
      // Interrupted while trying to lock camera closing.
      e.printStackTrace();

    } finally{
      mCameraOpenCloseLock.release();
    }

  }

  private void cameraOpenCloseLockAcquire(){
    try{
      mCameraOpenCloseLock.acquire();
    }catch (InterruptedException e){
      throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
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

  private SurfaceTexture mTexture;
  private Surface mSurface;


  private void createCameraPreviewSession() {

    CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback()  {

      @Override
      public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
        if (isCameraAlreadyClosed()) {
          return;
        }
        mCaptureSession = cameraCaptureSession;
        // Finally, we start displaying the camera preview.
        gotoPreviewState();
      }

      @Override
      public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

        createCameraPreviewSession();

      }

      private boolean isCameraAlreadyClosed(){
        if (null == mCameraDevice) {
          return true;
        }
        else{
          return false;
        }
      }
    };

    mTexture = mTextureView.getSurfaceTexture();

    // We configure the size of default buffer to be the size of camera preview we want.
    mTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

    // This is the output Surface we need to start preview.
    mSurface = new Surface(mTexture);

    try {
      // We set up a CaptureRequest.Builder with the output Surface.
      mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      mPreviewRequestBuilder.addTarget(mSurface);
      // Here, we create a CameraCaptureSession for camera preview.
      mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), cameraCaptureSessionStateCallback, null);
    } catch (CameraAccessException e){
      e.printStackTrace();
    }

  }

  private void takePicture() throws CameraAccessException {

    if (null == mActivity || null == mCameraDevice) {
      return;
    }

    addTargetToStillCaptureRequestBuilderForPreview();
    setupStillCaptureRequestBuilderForPreview();
    Log.i(TAG, "mCurrentOrientation : " + mCurrentOrientation);

    CameraCaptureSession.CaptureCallback cameraCaptureSessionCaptureCallback
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
        ToastHelper.showToast(mActivity, "Saved: " + mOutputPictureFile);
        Log.i(TAG, mOutputPictureFile.toString());
        gotoHomeActivityWithImageFilePath();
      }

      private void gotoHomeActivityWithImageFilePath(){
        Intent intent = new Intent(getActivity(), HomeActivity.class);
        intent.putExtra("image_filepath", mImageSaver.getOutputFilepath());
        startActivity(intent);
      }
    };

    mCaptureSession.capture(mStillCaptureRequestBuilder.build(), cameraCaptureSessionCaptureCallback, null);

  }

  private void addTargetToStillCaptureRequestBuilderForPreview() throws CameraAccessException {
    mStillCaptureRequestBuilder =
        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
    mStillCaptureRequestBuilder.addTarget(mImageReader.getSurface());

  }

  private void setupStillCaptureRequestBuilderForPreview(){
    // Use the same AE and AF modes as the preview.
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY);
    mStillCaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
    mStillCaptureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
  }

  /**
   * Unlock the focus. This method should be called when still image capture sequence is finished.
   */
  private void gotoPreviewState() {
    try {

      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mCurrentEffect);
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
      mPreviewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY);
      mPreviewRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG);

      mPreviewRequest = mPreviewRequestBuilder.build();      // After this, the camera will go back to the normal state of preview.
      mState = STATE_WAITING_PRECAPTURE;
      mCaptureSession.stopRepeating();
      mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }


  static final int EFFECT_SOLARIZE = 3;
  static final int EFFECT_POSTERIZE = 5;
  static final int EFFECT_COUNT = 9;
  /**
   * Pick the index of next effect.
   */
  public void adjustNextEffect() {
    this.mCurrentEffect = (mCurrentEffect + 1) % EFFECT_COUNT;
    if (mCurrentEffect == EFFECT_SOLARIZE) // skip solarize
      mCurrentEffect = EFFECT_SOLARIZE+1;
    else if (mCurrentEffect == EFFECT_POSTERIZE) // skip posterize
      mCurrentEffect = EFFECT_POSTERIZE+1;
  }

  /**
   * Pick the index of previous effect
   */
  public void adjustPrevEffect() {
    this.mCurrentEffect = mCurrentEffect - 1;
    if (mCurrentEffect == -1)
      mCurrentEffect = EFFECT_COUNT-1;
    else if (mCurrentEffect == EFFECT_SOLARIZE) // skip solarize
      mCurrentEffect = EFFECT_SOLARIZE-1;
    else if (mCurrentEffect == EFFECT_POSTERIZE) // skip posterize
      mCurrentEffect = EFFECT_POSTERIZE-1;
  }

  /**
   * This comment is provided by google android
   * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that is
   * at least as large as the respective texture view size, and that is at most as large as the
   * respective max size, and whose aspect ratio matches with the specified value. If such size
   * doesn't exist, choose the largest one that is at most as large as the respective max size, and
   * whose aspect ratio matches with the specified value.
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
    int width = aspectRatio.getWidth();
    int height = aspectRatio.getHeight();
    for (Size option : choices) {
      if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
          option.getHeight() == option.getWidth() * height / width) {
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


  // for obtaining orientation although orientation of app is locked to portrait.
  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.values.length > 4) {
      float[] truncatedRotationVector = new float[4];
      System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
      updateOrientationAndRotateButton(truncatedRotationVector);
    } else {
      updateOrientationAndRotateButton(event.values);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  private void updateOrientationAndRotateButton(float[] vectors) {
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

    mPreOrientation = mCurrentOrientation;   // temp saved value
    if (-45 <= roll && roll < 45) {
      mCurrentOrientation = 0;
    } else if (45 <= roll && roll < 135) {
      mCurrentOrientation = 90;
    } else if (-135 <= roll && roll < -45) {
      mCurrentOrientation = 270;
    } else {
      mCurrentOrientation = 180;
    }
    rotateButton();
  }

  private void rotateButton(){
    // button rotation.
    if ((mPreOrientation == 0 && mCurrentOrientation == 90) ||
        (mPreOrientation == 90 && mCurrentOrientation == 180) ||
        (mPreOrientation == 180 && mCurrentOrientation == 270) ||
        (mPreOrientation == 270 && mCurrentOrientation == 0)) {
      mBtnTakePicture.startAnimation(mAnimRotateRight);

    } else if ((mPreOrientation == 90 && mCurrentOrientation == 0) ||
        (mPreOrientation == 180 && mCurrentOrientation == 90) ||
        (mPreOrientation == 270 && mCurrentOrientation == 180) ||
        (mPreOrientation == 0 && mCurrentOrientation == 270)) {
      mBtnTakePicture.startAnimation(mAnimRotateLeft);
    }
  }


}
