package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.system.ErrnoException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility for dealing with file
 * Created by LeeWonKyung on 2015-12-14.
 */
public class FileHelper {

  static final String TAG = "FileHelper";
  /**
   * like linux command rm
   * @param inputPath
   * @param inputFile
   */
  public static void deleteFile(String inputPath, String inputFile) {
    try {
      // delete the original file
      new File(inputPath + inputFile).delete();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * like linux command mv
   * @param inputPath
   * @param outputPath
   */
  public static void moveFile(String inputPath, String outputPath) {

    InputStream inputStream = null;
    OutputStream outputStream = null;
    final String inputFile = inputPath.substring(inputPath.lastIndexOf(File.separator) + 1, inputPath.length());
    try {
      //create output directory if it doesn't exist
      File dir = new File(outputPath);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      inputStream = new FileInputStream(inputPath);
      outputStream = new FileOutputStream(outputPath + inputFile);

      byte[] buffer = new byte[1024];
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      inputStream.close();
      // write the output file
      outputStream.flush();
      outputStream.close();
      // delete the original file
      new File(inputPath).delete();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * like linux commmand cp
   * @param inputPath
   * @param inputFile
   * @param outputPath
   */
  public static void copyFile(String inputPath, String inputFile, String outputPath) {

    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {

      //create output directory if it doesn't exist
      File dir = new File(outputPath);
      if (!dir.exists()) {
        dir.mkdirs();
      }
      inputStream = new FileInputStream(inputPath + inputFile);
      outputStream = new FileOutputStream(outputPath + inputFile);
      byte[] buffer = new byte[1024];
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      inputStream.close();
      // write the output file (You have now copied the file)
      outputStream.flush();
      outputStream.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
