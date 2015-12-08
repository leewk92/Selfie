package com.estsoft.pilotproject.leewonkyung.selfie.Util;

import android.util.Size;

import java.util.Comparator;

/**
 * Compares two {@code Size}s based on their areas.
 */

public class CompareSizesByArea implements Comparator<Size> {

    public CompareSizesByArea() {
        super();
    }

    @Override
    public int compare(Size lhs, Size rhs) {
        // We cast here to ensure the multiplications won't overflow
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                (long) rhs.getWidth() * rhs.getHeight());
    }

}