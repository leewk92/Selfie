package com.estsoft.pilotproject.leewonkyung.selfie.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by wonkyung on 2015-07-13.
 */
public class HTTPRestfulHelper {

  static final String TAG = "HTTPRestfulHelper";

  public String mUrl;
  public Bundle mInputBundle;
  public JSONArray mOutputJsonArray;
  public JSONObject mOutputJsonObject;
  public String mOutputString;
  public String mHTTPRestType;
  public Context mContext;
  public HttpAsyncTask mTask;
  public String mPhotoPath = null;


  // Constructors
  public HTTPRestfulHelper() {
  }

  // Constructor for POST with Context
  public HTTPRestfulHelper(Context mContext, String url, String HTTPRestType, Bundle inputBundle) {
    this.mContext = mContext;
    this.mUrl = url;
    this.mHTTPRestType = HTTPRestType;
    this.mInputBundle = inputBundle;

    mTask = new HttpAsyncTask();
    // new HttpAsyncTask().execute(url,HTTPRestType);
  }

  public void doExecution() {
    mTask.execute(mUrl, mHTTPRestType);
  }

  public String POST(String url, Bundle bundle) {
    InputStream inputStream = null;
    String result = "";
    // 1. create HttpClient
    HttpClient httpclient = new DefaultHttpClient();

    // 2. make POST request to the given URL
    HttpPost httpPost = new HttpPost(url);

    String json = "";

    if (mPhotoPath != null) {

      Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath);
      ByteArrayOutputStream byteArrayOutputStream =  new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 25, byteArrayOutputStream); // bm is the bitmap object
      byte[] byteArray = byteArrayOutputStream.toByteArray();

      String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
      bundle.putCharSequence("photo", encodedImage);
    }

    try {

      // 3. build jsonObject
      JSONObject jsonObject = new JSONObject();
      for (String key : bundle.keySet()) {
        jsonObject.accumulate(key, bundle.get(key));
      }

      // 4. convert JSONObject to JSON to String
      json = jsonObject.toString();
      json = json.replace("\"[", "[");
      json = json.replace("]\"", "]");

       // 5. set json to StringEntity
      StringEntity se = new StringEntity(json, "UTF-8");

      // 6. set httpPost Entity
      httpPost.setEntity(se);

      // 7. Set some headers to inform server about the type of the content
      httpPost.setHeader("Accept", "application/json;charset=utf-8");

      // 8. Execute POST request to the given URL
      HttpResponse httpResponse = httpclient.execute(httpPost);

      // 9. receive response as inputStream
      inputStream = httpResponse.getEntity().getContent();

      // 10. convert inputstream to string
      if (inputStream != null) {
        result = convertInputStreamToString(inputStream);
        Log.i("HTTP POST ResultStream", result);
      } else {
        result = "Did not work!";
        Log.i(TAG, result);
      }
    } catch (IOException e) {
      // caused by convertInputStream function
      e.printStackTrace();
    } catch( JSONException e){
      e.printStackTrace();
    }

    // 11. return result
    mOutputString = result;
    try {
      mOutputJsonObject = new JSONObject(mOutputString);
    } catch (JSONException e) {
      e.printStackTrace();
      mOutputJsonObject = new JSONObject();
    }

    try {
      mOutputJsonArray = new JSONArray(mOutputString);
    } catch (JSONException e) {
      e.printStackTrace();
      mOutputJsonArray = new JSONArray();
    }

    return result;
  }

  public static String convertInputStreamToString(InputStream inputStream) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    String line = "";
    String result = "";
    while ((line = bufferedReader.readLine()) != null)
      result += line;

    inputStream.close();
    return result;

  }


  public class HttpAsyncTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... strings) {
      String url = strings[0];
      String sHTTPRestType = strings[1];
      if (sHTTPRestType == "POST") {
        mOutputString = POST(url, mInputBundle);
        try {
          mOutputJsonObject = new JSONObject(mOutputString);
          mOutputJsonArray = new JSONArray(mOutputString);

        } catch (JSONException e) {
          mOutputJsonObject = new JSONObject();
          mOutputJsonArray = new JSONArray();
        }
      }
      else{
        // get, delete, put case. not used for this application
      }
      return mOutputString;
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      Log.i(TAG, mOutputString.toString());
    }
  }

  /**
   *  getters and setters
   */
  public String getUrl() {
    return mUrl;
  }

  public void setUrl(String url) {
    this.mUrl = url;
  }

  public Bundle getInputBundle() {
    return mInputBundle;
  }

  public void setInputBundle(Bundle inputBundle) {
    this.mInputBundle = inputBundle;
  }

  public JSONArray getOutputJsonArray() {
    return mOutputJsonArray;
  }

  public void setOutputJsonArray(JSONArray outputJsonArray) {
    this.mOutputJsonArray = outputJsonArray;
  }

  public JSONObject getOutputJsonObject() {
    return mOutputJsonObject;
  }

  public void setOutputJsonObject(JSONObject outputJsonObject) {
    this.mOutputJsonObject = outputJsonObject;
  }

  public String getHTTPRestType() {
    return mHTTPRestType;
  }

  public void setHTTPRestType(String HTTPRestType) {
    this.mHTTPRestType = HTTPRestType;
  }

  public String getOutputString() {
    return mOutputString;
  }

  public void setOutputString(String outputString) {
    this.mOutputString = outputString;
  }

  public String getPhoto() {
    return mPhotoPath;
  }

  public void setPhoto(String photo) {
    this.mPhotoPath = photo;
  }

  public Context getContext() {
    return mContext;
  }

  public void setContext(Context mContext) {
    this.mContext = mContext;
  }

  public void setTask(HttpAsyncTask mTask){
    this.mTask = mTask;
  }
}




