package com.kairos.android.example;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import android.content.SharedPreferences.Editor;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;

    private SharedPreferences sPref;
    private Editor editor;
    private Handler handler;
    public static Kairos myKairos;
    public KairosListener listener;

    private String subjectId = "myimage";
    private String galleryId;
    private String selector = "FULL";
    private String multipleFaces = "false";
    private String minHeadScale = "0.25";
    private String threshold = "0.75";
    private String maxNumResults = "25";

    private EditText galleryName;
    private Button start;
    private Intent cameraIntent;

    private int captureCount = 0;
    public static final String PREF_NAME = "FaceRecPref";
    public static final String GALLERY_NAME = "gallery_name";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 2);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 3);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 4);

        sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sPref.edit();

        if(sPref.getBoolean(KEY_IS_LOGGED_IN, false) == true){
            Intent intent = new Intent(this, MyActivity.class);
            startActivity(intent);
            finish();
        }

        /*editor.putBoolean("Started", false);
        editor.commit();*/

        galleryName = (EditText) findViewById(R.id.galleryid);
        start = (Button) findViewById(R.id.start);

        handler = new Handler();
        initialiseKairos();
        listener = new KairosListener() {
            @Override
            public void onSuccess(String response) {
                Log.d("KAIROS_DEMO", response);
            }
            @Override
            public void onFail(String response) {
                Log.d("KAIROS_DEMO", response);
            }
        };

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(galleryName.length() == 0 || galleryName.equals("")){
                    Toast.makeText(getApplicationContext(), "Please select a Gallery Name", Toast.LENGTH_SHORT).show();
                }
                else{
                    galleryId = galleryName.getText().toString();
                    editor.putString(GALLERY_NAME, galleryId);
                    requestCamera();
                }
            }
        });

    }

    private void requestCamera(){
        cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void initialiseKairos(){
        myKairos = new Kairos();
        String app_id = "ff849a7b";
        String api_key = "a0115dedb47393e59918822ec0989cd0";
        myKairos.setAuthentication(this, app_id, api_key);
        //myKairos.listGalleries(listener);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(captureCount == 3){
            Toast.makeText(getApplicationContext(), "All Set and Done", Toast.LENGTH_SHORT);
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.commit();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), MyActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 3000);
        }

        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null){
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            try {
                myKairos.enroll(photo, subjectId, galleryId, selector, multipleFaces, minHeadScale, listener);
                Toast.makeText(getApplicationContext(), "Image Enrolled in " + galleryId, Toast.LENGTH_SHORT).show();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        captureCount++;
                        requestCamera();
                    }
                }, 2000);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

    }


}
