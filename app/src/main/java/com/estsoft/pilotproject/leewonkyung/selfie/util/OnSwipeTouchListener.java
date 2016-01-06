package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * This swipeTouchListener is used for changing filter on Camera preview.
 * TODO : I'm not sure that it is proper to locate this listener inside of Camera class as static private class or not.
 */
public class OnSwipeTouchListener implements OnTouchListener {

  public final GestureDetector gestureDetector;

  public OnSwipeTouchListener(Context context) {
    gestureDetector = new GestureDetector(context, new GestureListener());
  }

  @Override
  public boolean onTouch(final View v, final MotionEvent event) {

    return gestureDetector.onTouchEvent(event);
  }

  private final class GestureListener extends SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public boolean onDown(MotionEvent e) {
      onActionDown(e);
      return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      boolean result = false;

      float diffY = e2.getY() - e1.getY();
      float diffX = e2.getX() - e1.getX();
      if (Math.abs(diffX) > Math.abs(diffY)) {
        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
          if (diffX > 0) {
            onSwipeRight();
          } else {
            onSwipeLeft();
          }
        }
        result = true;
      } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffY > 0) {
          onSwipeBottom();
        } else {
          onSwipeTop();
        }
      }
      result = true;


      return result;
    }
  }

  // for override
  public void onSwipeRight() {
  }

  public void onSwipeLeft() {
  }

  public void onSwipeTop() {
  }

  public void onSwipeBottom() {
  }

  public void onActionDown(MotionEvent event) {
  }
}