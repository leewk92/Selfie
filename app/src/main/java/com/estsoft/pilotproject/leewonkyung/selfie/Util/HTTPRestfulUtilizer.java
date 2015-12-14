package com.estsoft.pilotproject.leewonkyung.selfie.Util;

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
public class HTTPRestfulUtilizer {

    public String url;
    public static Bundle inputBundle;
    public static JSONArray outputJsonArray;
    public static JSONObject outputJsonObject;
    public String HTTPRestType;
    public static String outputString;
    public String token;
    public Context mContext;
    public HttpAsyncTask task;
    public String photo=null;
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Context getmContext() {
        return mContext;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    // Constructors
    public HTTPRestfulUtilizer(){

    }
    // Constructor for POST with Context
    public HTTPRestfulUtilizer(Context mContext, String url, String HTTPRestType, Bundle inputBundle) {
        this.mContext = mContext;
        this.url = url;
        this.HTTPRestType = HTTPRestType;
        this.inputBundle = inputBundle;
//        Log.d("HTTP Constructor url", url);
        task = new HttpAsyncTask();
        // new HttpAsyncTask().execute(url,HTTPRestType);
    }
    // Constructor for GET
    public HTTPRestfulUtilizer(Context mContext, String url, String HTTPRestType) {
        this.mContext = mContext;
        this.url = url;
        this.HTTPRestType = HTTPRestType;
        task = new HttpAsyncTask();
     //   Log.d("HTTP Constructor url",url);
        // new HttpAsyncTask().execute(url,HTTPRestType);
    }
 /*
    // Constructor for POST
    public HTTPRestfulUtilizer(String url, String HTTPRestType, Bundle inputBundle) {

        this.url = url;
        this.HTTPRestType = HTTPRestType;
        this.inputBundle = inputBundle;
        Log.d("HTTP Constructor url", url);
        task = new HttpAsyncTask();
       // new HttpAsyncTask().execute(url,HTTPRestType);
    }
    // Constructor for GET
    public HTTPRestfulUtilizer(String url, String HTTPRestType) {
        this.url = url;
        this.HTTPRestType = HTTPRestType;
        task = new HttpAsyncTask();
        Log.d("HTTP Constructor url",url);
       // new HttpAsyncTask().execute(url,HTTPRestType);
    }
*/
    public void doExecution(){

        task.execute(url, HTTPRestType);
    }

    public String PUT(String url, Bundle bundle){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPut httpPut = new HttpPut(url);

            String json = "";
            if(photo != null){

                Bitmap bm = BitmapFactory.decodeFile(photo);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 25, baos); // bm is the bitmap object
                byte[] b = baos.toByteArray();

                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                bundle.putCharSequence("photo",encodedImage);
            }
            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            for( String key : bundle.keySet()){
                jsonObject.accumulate(key, bundle.get(key));
            }

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();
            Log.d("firstjson_PUT", json);
            json = json.replace("\"[", "[");
            json = json.replace("]\"", "]");
  //          Log.d("secondjson", json);
            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json,"UTF-8");
           // StringEntity se = new StringEntity(json);
           // MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();

           // multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            // multipartEntity.addBinaryBody("someName", file, ContentType.create("image/jpeg"), file.getName());
           // multipartEntity.addPart("", new StringBody(json, ContentType.TEXT_PLAIN));


            // 6. set httpPost Entity
            httpPut.setEntity(se);

           // httpPut.setEntity(multipartEntity.build());

            // 7. Set some headers to inform server about the type of the content
            httpPut.setHeader("Cache-Control", "no-cache");
            httpPut.setHeader("Accept", "application/json;charset=utf-8");
            httpPut.setHeader("Content-type", "application/json");
//            AppPrefs appPrefs = new AppPrefs(mContext);
//            token = appPrefs.getToken();
//            if( token != ""){
//                httpPut.setHeader("Authorization","Token "+token);
//            }
            // 8. Execute PUT request to the given URL
 //           Log.d("contents",convertInputStreamToString(httpPut.getEntity().getContent()));
            HttpResponse httpResponse = httpclient.execute(httpPut);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.d("HTTP PUT ResultStream", result);
            }else {
                result = "Did not work!";
                Log.d("HTTP PUT ResultStream", result);
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        outputString = result;
        try {
            outputJsonObject = new JSONObject(outputString);
        }catch (Exception e){
            outputJsonObject = new JSONObject();
        }

        try {
            outputJsonArray = new JSONArray(outputString);

        }catch (Exception e){
            outputJsonArray = new JSONArray();
        }
        return result;
    }

    public String POST(String url, Bundle bundle){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json = "";

            if(photo != null){

                Bitmap bm = BitmapFactory.decodeFile(photo);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 25, baos); // bm is the bitmap object
                byte[] b = baos.toByteArray();

                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                bundle.putCharSequence("photo",encodedImage);
            }
            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            for( String key : bundle.keySet()){
                jsonObject.accumulate(key, bundle.get(key));
 //               Log.d("key,value", key + " " + bundle.get(key));
            }

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();
 //           Log.d("firstjson",json);
            json = json.replace("\"[", "[");
            json = json.replace("]\"", "]");
  //          Log.d("secondjson", json);
            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity

            StringEntity se = new StringEntity(json,"UTF-8");


            //MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();

            //multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            // multipartEntity.addBinaryBody("someName", file, ContentType.create("image/jpeg"), file.getName());
            //multipartEntity.addPart("content", json);
            //multipartEntity.addTextBody("content",json);
            // 6. set httpPost Entity
            httpPost.setEntity(se);
            //httpPost.setEntity(multipartEntity.build());

            // 7. Set some headers to inform server about the type of the content


            httpPost.setHeader("Accept", "application/json;charset=utf-8");
            httpPost.setHeader("Content-type", "application/json");
//            AppPrefs appPrefs = new AppPrefs(mContext);
//            token = appPrefs.getToken();
//            if( token != ""){
//                httpPost.setHeader("Authorization","Token "+token);
//            }
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

           // se.consumeContent();

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.d("HTTP POST ResultStream", result);
            }else {
                result = "Did not work!";
                Log.d("HTTP POST ResultStream", result);
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        outputString = result;
        try {
            outputJsonObject = new JSONObject(outputString);
        }catch (Exception e){
            outputJsonObject = new JSONObject();
        }

        try {
            outputJsonArray = new JSONArray(outputString);

        }catch (Exception e){
            outputJsonArray = new JSONArray();
        }

        return result;
    }


    public String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpGet httpGet = new HttpGet(url);


            // 7. Set some headers to inform server about the type of the content
            httpGet.setHeader("Accept", "application/json;charset=utf-8");
            httpGet.setHeader("Content-type", "application/json");
//            AppPrefs appPrefs = new AppPrefs(mContext);
//            token = appPrefs.getToken();
//            if( token != ""){
//                httpGet.setHeader("Authorization","Token "+token);
//            }
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpGet);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.d("HTTP GET ResultStream", result);
            }
            else {
                result = "Did not work!";
                Log.d("HTTP GET ResultStream", result);
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        outputString = result;
        try {
            outputJsonObject = new JSONObject(outputString);
        }catch (Exception e){
            outputJsonObject = new JSONObject();
        }

        try {
            outputJsonArray = new JSONArray(outputString);

        }catch (Exception e){
            outputJsonArray = new JSONArray();
        }

        return result;
    }

    public String DELETE(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpDelete httpDelete = new HttpDelete(url);


            // 7. Set some headers to inform server about the type of the content
            httpDelete.setHeader("Accept", "application/json;charset=utf-8");
            //httpDelete.setHeader("Content-type", "application/json");
//            AppPrefs appPrefs = new AppPrefs(mContext);
//            token = appPrefs.getToken();
//            if( token != ""){
//                httpDelete.setHeader("Authorization","Token "+token);
//            }
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpDelete);

            // 9. receive response as inputStream
     /*       inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.d("HTTP GET ResultStream", result);
            }
            else {
                result = "Did not work!";
                Log.d("HTTP GET ResultStream", result);
            }*/
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        outputString = result;
        try {
            outputJsonObject = new JSONObject(outputString);
        }catch (Exception e){
            outputJsonObject = new JSONObject();
        }

        try {
            outputJsonArray = new JSONArray(outputString);

        }catch (Exception e){
            outputJsonArray = new JSONArray();
        }

        return result;
    }




    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


    public class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String url = strings[0];
            String sHTTPRestType = strings[1];
            if(sHTTPRestType == "POST") {
                outputString = POST(url, inputBundle);
                try {
                    outputJsonObject = new JSONObject(outputString);
                    outputJsonArray = new JSONArray(outputString);

                }catch (Exception e){
                    outputJsonObject = new JSONObject();
                    outputJsonArray = new JSONArray();
                }
                return outputString;
            }
            else if(sHTTPRestType == "GET") {
                outputString = GET(url);
                return outputString;
            }
            else if(sHTTPRestType == "PUT"){
                outputString = PUT(url, inputBundle);
                try {
                    outputJsonObject = new JSONObject(outputString);
                    outputJsonArray = new JSONArray(outputString);

                }catch (Exception e){
                    outputJsonObject = new JSONObject();
                    outputJsonArray = new JSONArray();
                }
                return outputString;
            }else{
                outputString = GET(url);
                return outputString;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("HTTPREQUEST OUTPUT",outputString.toString());
        }
    }

// 포스트 위드 사진

    public String POST_withImage(String url, Bundle bundle){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost;

            String json = "";

            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            for( String key : bundle.keySet()){
                jsonObject.accumulate(key, bundle.get(key));
            }

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();
//            Log.d("firstjson",json);
            json = json.replace("\"[", "[");
            json = json.replace("]\"", "]");
 //           Log.d("secondjson", json);
            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);



            // 5. set json to StringEntity
            //StringEntity se = new StringEntity(json,"UTF-8");
            //MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();

            //multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            // multipartEntity.addBinaryBody("someName", file, ContentType.create("image/jpeg"), file.getName());
            //multipartEntity.addPart("content", json);
            //multipartEntity.addTextBody("content",json);
            // 6. set httpPost Entity
            httpPost = returnPostFileIncludeImage(photo,url,json);
           // httpPost.setEntity(se);
            //httpPost.setEntity(multipartEntity.build());

            // 7. Set some headers to inform server about the type of the content
    //        httpPost.setHeader("Accept", "application/json;charset=utf-8");
    //        httpPost.setHeader("Content-type", "application/json");
    //        AppPrefs appPrefs = new AppPrefs(mContext);
    //        token = appPrefs.getToken();
    //        if( token != ""){
    //            httpPost.setHeader("Authorization","Token "+token);
    //        }
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.d("HTTP POST ResultStream", result);
            }else {
                result = "Did not work!";
                Log.d("HTTP POST ResultStream", result);
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        outputString = result;
        try {
            outputJsonObject = new JSONObject(outputString);
        }catch (Exception e){
            outputJsonObject = new JSONObject();
        }

        try {
            outputJsonArray = new JSONArray(outputString);

        }catch (Exception e){
            outputJsonArray = new JSONArray();
        }
        return result;
    }



    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Bundle getInputBundle() {
        return inputBundle;
    }

    public void setInputBundle(Bundle inputBundle) {
        this.inputBundle = inputBundle;
    }

    public JSONArray getOutputJsonArray() {
        return outputJsonArray;
    }

    public void setOutputJsonArray(JSONArray outputJsonArray) {
        this.outputJsonArray = outputJsonArray;
    }
    public JSONObject getOutputJsonObject() {
        return outputJsonObject;
    }

    public void setOutputJsonObject(JSONObject outputJsonObject) {
        this.outputJsonObject = outputJsonObject;
    }

    public String getHTTPRestType() {
        return HTTPRestType;
    }

    public void setHTTPRestType(String HTTPRestType) {
        this.HTTPRestType = HTTPRestType;
    }

    public String getOutputString() {
        return outputString;
    }

    public void setOutputString(String outputString) {
        this.outputString = outputString;
    }
    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }




    public HttpPost returnPostFileIncludeImage(String filePath, String postUrl, String json)
            throws Exception {

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "xxxxxxxx";
        // Having HttpClient to respond to both HTTP and HTTPS url connection by accepting the urls along with keystore / trust certificates

        Bitmap bm = BitmapFactory.decodeFile(filePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 25, baos); // bm is the bitmap object
        byte[] b = baos.toByteArray();

        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);


        String str = twoHyphens + boundary + lineEnd;
        String str2 = "Content-Disposition: form-data; name=\"title\"";
        String str3 = "Content-Type: application/json";
        String str4 = "Content-Disposition: form-data; name=\"photo\"";
        String str5 = "Content-Type: image/jpg\r\nContent-Tranfer-Encoding: base64";
        String str6 = twoHyphens + boundary + twoHyphens;
        String str7 = "";
        for( String key : inputBundle.keySet()){
            str7 = str7+ "\r\n--" + boundary + "\r\n";
            str7 = str7+ "Content-Type: text;charset=utf-8";
            str7 = str7 + "Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n" + inputBundle.get(key);

        }

//        String StrTotal = str + str2 + "\r\n" + str3 + "\r\n" + "\r\n" + json + "\r\n" + str
//                + str4 + "\r\n" + str5 + "\r\n" + "\r\n" + encodedImage + "\r\n" + str6;
        String StrTotal = str  +str7 + "\r\n" + str
                + str4 + "\r\n" + str5 + "\r\n" + "\r\n" + encodedImage + "\r\n" + str6;

        //System.out.print("Multipart request string is "+StrTotal);

        HttpPost httpPost = new HttpPost(postUrl);

        httpPost.setHeader("Accept", "application/json;charset=utf-8");
        httpPost.setHeader("Content-type", "multipart/form-data;boundary=" + boundary);
//        httpPost.setHeader("Content-Transfer-Encoding", "base64");
//        AppPrefs appPrefs = new AppPrefs(mContext);
//        String token = appPrefs.getToken();
//        if (token != "") {
//            httpPost.setHeader("Authorization", "Token " + token);
//        }
// System.out.println("Sending Post proxy request: " + post);

        StringEntity se = new StringEntity(StrTotal);
        se.setContentEncoding("UTF-8");
        httpPost.setEntity(se);
//        Log.d("postData", StrTotal);
        return httpPost;
    }

    public String executeClient(String filePath, String postURL, Bundle inputBundle) {
        try {
            URL url = new URL(postURL);
            String boundary = "SpecificString";
            URLConnection con = url.openConnection();
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());

            for( String key : inputBundle.keySet()){
                wr.writeBytes("\r\n--" + boundary + "\r\n");
                wr.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n" + inputBundle.get(key));

            }
			/*
			wr.writeBytes("\r\n--" + boundary + "\r\n");
			wr.writeBytes("Content-Disposition: form-data; name=\"msg\"\r\n\r\n" + commentPutEdit.getText().toString());
			wr.writeBytes("\r\n--" + boundary + "\r\n");
			wr.writeBytes("Content-Disposition: form-data; name=\"suid\"\r\n\r\n" + clickSuid);
			wr.writeBytes("\r\n--" + boundary + "\r\n");
			wr.writeBytes("Content-Disposition: form-data; name=\"uuid\"\r\n\r\n" + uuid);*/

            wr.writeBytes("\r\n--" + boundary + "\r\n");
            wr.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"image.jpg\"\r\n");
            wr.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

//            Log.d("upload path", filePath);
            FileInputStream fileInputStream = new FileInputStream(filePath);
            int bytesAvailable = fileInputStream.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0)
            {
                // Upload file part(s)
                DataOutputStream dataWrite = new DataOutputStream(con.getOutputStream());
                dataWrite.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            fileInputStream.close();

            wr.writeBytes("\r\n--" + boundary + "--\r\n");
//            Log.d("inputData",wr.toString());
            wr.flush();
            DataInputStream is = new DataInputStream(con.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
 //           Log.d("HTTP Post ResultStream" , sb.toString());
            outputString = sb.toString();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return null;
    }

}




