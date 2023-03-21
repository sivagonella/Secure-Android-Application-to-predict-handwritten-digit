package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.PathUtils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
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

    Button submit;
    Uri selectedImageUri;
    String categorySelected;
    File image = null;
    int number;
    SecretKey secretKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Bundle bundle = getIntent().getExtras();
        selectedImageUri = (Uri) bundle.get("BitmapImage");
        secretKey = (SecretKey) bundle.get("SecretKey");

        InputStream inputStream;
        OutputStream outputStream;

        try {
            inputStream = getApplicationContext().getContentResolver().openInputStream(selectedImageUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                image = new File(getApplicationContext().getCacheDir(), LocalDateTime.now() + ".png");
            }
            image.createNewFile();
            outputStream = new FileOutputStream(image);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(inputStream, outputStream);
                outputStream.flush();
            }
            encrypt();
        }catch (Exception e){

        }

        ImageView imageView = findViewById(R.id.imageToSubmit);
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
        imageView.setImageBitmap(bitmap);

        submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendImageAndCategory();
            }
        });
    }

    ActivityResultLauncher<Intent> launchAnotherActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                }
            }
    );

    protected void sendImageAndCategory() {

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
                "image",
                image.getName(),
                RequestBody.create(image, MediaType.parse("image/*png"))).build();
//        byte[] imageData = getByteArrayFromUri(selectedImageUri);
//        RequestBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addPart(
//                        Headers.of("Content-Disposition", "form-data; name=\"image\""),
//                        new ByteArrayRequestBody(imageData, MediaType.parse("image/png"))
//                )
//                .build();
//        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), imageData);


        Request request = new Request.Builder().url(getString(R.string.ip_address) + "upload").post(requestBody).build();
        OkHttpClient okHttpClient = new OkHttpClient();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Failure connecting to server", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    number = jsonObject.getInt("number");
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent changeActivityIntent = new Intent(CategoryActivity.this, SuccessScreenActivity.class);
                        changeActivityIntent.putExtra("number", number);
                        launchAnotherActivity.launch(changeActivityIntent);
                    }
                });
            }
        });

    };

    private void encrypt() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(selectedImageUri);
        ContextWrapper contextWrapper = new ContextWrapper(getApplication());
        File photoDir = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // on below line creating a file for encrypted file.
        image = new File(photoDir, "encfile" + ".png");

        // on below line creating a variable for file output stream.
        FileOutputStream fos = new FileOutputStream(image.getPath());

        // on below line creating a variable for secret key.
        // creating a variable for secret key and passing our secret key
        // and algorithm for encryption.
//        SecretKeySpec sks = new SecretKeySpec("12345678123456781234567812345678".getBytes(), "AES");

        // on below line creating a variable for cipher and initializing it
        Cipher cipher = Cipher.getInstance("AES");

        // on below line initializing cipher and
        // specifying decrypt mode to encrypt.
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // on below line creating cos
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        int b;
        byte[] d = new byte[8];
        while ((b = fis.read(d)) != -1) {
            cos.write(d, 0, b);
        }

        // on below line closing
        // our cos and fis.
        cos.flush();
        cos.close();
        fis.close();
    }

}