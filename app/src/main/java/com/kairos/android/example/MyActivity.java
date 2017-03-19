package com.kairos.android.example;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences.Editor;

import com.kairos.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MyActivity extends AppCompatActivity{

    private Button loadGallery;
    private Button loadCamera;
    private Button recognize;
    private Button refreshGallery;

    private ImageView imageView;
    private TextView statusText;

    private static final int GALLERY_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PICK_IMAGE_REQUEST = 3;
    private static final String PREF_NAME = "FaceRecPref";
    private static final String IMAGES_ENROLLED = "images_enrolled";

    private Kairos myKairos;
    private KairosListener listener;
    private SharedPreferences pref;
    private Editor editor;

    private String subjectId = "myimage";
    private String galleryId;
    private String selector = "FULL";
    private String multipleFaces = "false";
    private String minHeadScale = "0.25";
    private String threshold = "0.75";
    private String maxNumResults = "25";

    private Uri outputFileUri;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        loadGallery = (Button) findViewById(R.id.loada);
        loadCamera = (Button) findViewById(R.id.loadb);
        recognize = (Button) findViewById(R.id.recognize);
        refreshGallery = (Button) findViewById(R.id.refresh);
        imageView = (ImageView) findViewById(R.id.imageView1);
        statusText = (TextView) findViewById(R.id.status);

        pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = pref.edit();
        galleryId = pref.getString(MainActivity.GALLERY_NAME, MainActivity.GALLERY_NAME);

        initialiseKairos();
        progressDialog = new ProgressDialog(this);
        listener = new KairosListener() {
            @Override
            public void onSuccess(String response) {
                progressDialog.dismiss();
                try {
                    JSONObject root = new JSONObject(response);
                    JSONArray array = root.getJSONArray("images");
                    JSONObject jo = array.getJSONObject(0);
                    JSONObject transaction = jo.getJSONObject("transaction");

                    String status = transaction.getString("status");
                    if(status.equals("success")){
                        statusText.setTextColor(Color.GREEN);
                        statusText.setText("Its a MATCH");
                    }
                    else if(status.equals("failure")){
                        statusText.setTextColor(Color.RED);
                        statusText.setText(transaction.getString("message"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    JSONObject root = new JSONObject(response);
                    JSONArray array = root.getJSONArray("Errors");
                    JSONObject jo = array.getJSONObject(0);
                    String message = jo.getString("Message");

                    statusText.setTextColor(Color.RED);
                    statusText.setText(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("KAIROS_DEMO", response);
            }
            @Override
            public void onFail(String response) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Cannot fetch JSON", Toast.LENGTH_SHORT).show();
                Log.d("KAIROS_DEMO", response);
            }
        };

        loadGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imagePickIntent = new Intent(Intent.ACTION_PICK);
                imagePickIntent.setType("image/*");
                startActivityForResult(imagePickIntent, GALLERY_REQUEST);
            }
        });

        loadCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageView.getDrawable() == null){
                    Toast.makeText(getApplicationContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                }
                else{
                    Bitmap sample = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    try {
                        progressDialog.setMessage("Recognizing...");
                        progressDialog.show();
                        myKairos.recognize(sample, galleryId, selector, threshold, minHeadScale, maxNumResults, listener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        refreshGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    myKairos.deleteSubject(subjectId, galleryId, listener);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        /*recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.liz);
                Bitmap image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                try {
                    myKairos.enroll(image, subjectId, galleryId, selector, multipleFaces, minHeadScale, listener);
                    Toast.makeText(getApplicationContext(), "Image Enrolled in " + galleryId, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });*/

        /*String image = "http://media.kairos.com/liz.jpg";
        myKairos.detect(image, null, null, listener);
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.liz);
        String selector = "FULL";
        String minHeadScale = "0.25";
        myKairos.detect(image, selector, minHeadScale, listener);
        String image = "http://media.kairos.com/liz.jpg";
        String subjectId = "Elizabeth";
        String galleryId = "friends";
        myKairos.enroll(image, subjectId, galleryId, null, null, null, listener);*/
        //Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.liz);
        //myKairos.listGalleries(listener);                                            //List galleries
        //myKairos.listSubjectsForGallery("your_gallery_name", listener);              //List subjects in gallery
        //myKairos.deleteSubject("your_subject_id", "your_gallery_name", listener);    //Delete subject from gallery
        //myKairos.deleteGallery("your_gallery_name", listener);                       //Delete an entire gallery

    }

    private void imageChooser(int REQUEST_CODE) {
        File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Track'n'Train" + File.separator + "Store Picture" + File.separator);
        root.mkdirs();
        final String fname = "storePic" + System.currentTimeMillis() + ".jpg";
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);
        //Camera
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final String localPackageName = res.activityInfo.loadLabel(packageManager).toString();
            if (localPackageName.toLowerCase().equals("camera")) {
                final Intent intent = new Intent(captureIntent);
                intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                intent.setPackage(packageName);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                cameraIntents.add(intent);
            }
        }
        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");
        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
        startActivityForResult(chooserIntent, REQUEST_CODE);
    }

    private void initialiseKairos(){
        myKairos = new Kairos();
        String app_id = "ff849a7b";
        String api_key = "a0115dedb47393e59918822ec0989cd0";
        myKairos.setAuthentication(this, app_id, api_key);
        //myKairos.listGalleries(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null){
            Uri imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null){
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            final boolean isCamera;
            if (data == null) {
                isCamera = true;
            } else {
                final String action = data.getAction();
                if (action == null) {
                    isCamera = false;
                } else {
                    isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                }
            }
            Uri selectedImageUri;
            if (isCamera) {
                selectedImageUri = outputFileUri;
            } else {
                selectedImageUri = data == null ? null : data.getData();
            }
            imageView.setImageURI(selectedImageUri);
        }
    }*/


}


