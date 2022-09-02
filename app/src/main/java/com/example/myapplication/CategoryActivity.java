package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.PathUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class CategoryActivity extends AppCompatActivity {

    Spinner category;
    Button submit;
    Uri selectedImageUri;
    String categorySelected;
    File image = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Bundle bundle = getIntent().getExtras();
        selectedImageUri = (Uri) bundle.get("BitmapImage");

//        Bitmap bitmap = null;
//        try {
//            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        ImageView imageView = findViewById(R.id.imageToSubmit);
//        imageView.setImageBitmap(bitmap);

        InputStream inputStream;
        OutputStream outputStream;

        try {
            inputStream = getApplicationContext().getContentResolver().openInputStream(selectedImageUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                image = new File(getApplicationContext().getCacheDir(), LocalDateTime.now() + ".jpg");
            }
            image.createNewFile();
            outputStream = new FileOutputStream(image);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(inputStream, outputStream);
                outputStream.flush();
            }
        }catch (Exception e){

        }

        ImageView imageView = findViewById(R.id.imageToSubmit);
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
        imageView.setImageBitmap(bitmap);

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

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
                "image",
                image.getName(),
                RequestBody.create(image, MediaType.parse("image/*jpg"))).
                addFormDataPart("category", categorySelected).build();

        Request request = new Request.Builder().url("http://192.168.0.127:9000/upload").post(requestBody).build();
        OkHttpClient okHttpClient = new OkHttpClient();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.label);
                        responseText.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.label);
                        try {
                            responseText.setText("Server's Response\n" + response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


}