package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.app.Activity;
import android.widget.Toast;

/**
 * This class helps making toast on the activity or fragment.
 * Created by LeeWonKyung on 2015-12-18.
 */
public class ToastHelper {

  /**
   * Shows a {@link Toast} on the UI thread.
   *
   * @param text The message to show
   */
  static public void showToast(final Activity activity, final String text) {

    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
        }
      });
    }
  }

}
