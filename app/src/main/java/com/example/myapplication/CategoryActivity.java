package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CategoryActivity extends AppCompatActivity {

    Spinner category;
    Button submit;
    String selectedImagePath;
    ImageView image;
    Uri selectedImageUri;
    Bitmap bitmap;
    String categorySelected;
    TextView label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Bundle bundle = getIntent().getExtras();
        selectedImageUri = (Uri) bundle.get("BitmapImage");



        File im = new File(selectedImageUri.getEncodedPath());

//        image = findViewById(R.id.imageToSubmit);

            Bitmap myBitmap = BitmapFactory.decodeFile(im.getAbsolutePath());

            ImageView myImage = (ImageView) findViewById(R.id.imageToSubmit);

//            myImage.setImageBitmap(myBitmap);

        myImage.setImageURI(selectedImageUri);

        category = findViewById(R.id.category);
        String[] categories = new String[] {"Portrait", "Fashion", "Sports", "Still", "Wildlife", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        category.setAdapter(adapter);

        submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categorySelected = category.getSelectedItem().toString();
                sendImageAndCategory();

            }
        });
    }

    private void sendImageAndCategory(){

//        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("category", categorySelected)
//                .addFormDataPart("image", selectedImageUri.toString(), selectedImageUri.getPath())

//        String postUrl = "http://192.168.0.127:9000/upload";
//        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
//
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath(), options);


//        OkHttpClient okHttpClient = new OkHttpClient();
//        RequestBody formBody = new FormBody.Builder().add("category", categorySelected).build();
//
//        Request request = new Request.Builder().url("http://192.168.0.127/uploadText").post(formBody).build();
//
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//
//                        label = findViewById(R.id.label);
//                        try {
//                            label.setText(response.body().string());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                });
//            }
//        });

    }

    private String getFilePath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(projection[0]);
            String picturePath = cursor.getString(columnIndex); // returns null
            cursor.close();
            return picturePath;
        }
        return null;
    }

}