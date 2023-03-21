package com.example.myapplication;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView pagenameTextView;
    private Button nextButton, openGalleryButton, openCameraButton;
    private ImageView imageView;
    String selectedImagePath;
    Bitmap selectedImageBitmap;
    Uri selectedImageUri;
    SecretKey secretKey;
    PublicKey publicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pagenameTextView = findViewById(R.id.pagename);
        nextButton = findViewById(R.id.next);
        openGalleryButton = findViewById(R.id.galleryButton);
        openCameraButton = findViewById(R.id.cameraButton);
        imageView = findViewById(R.id.imageView);

        secretKey = generateAESKey();
        getRsaPublicKeyFromServer();

        openGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery(view);
            }
        });

        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera(view);
            }
        });

        nextButton.setEnabled(false);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage(view);
            }
        });

        OkHttpClient okHttpClient = new OkHttpClient();

        Request request = new Request.Builder().url(getString(R.string.ip_address)).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "server_down", Toast.LENGTH_SHORT).show();
                        pagenameTextView.setText("Error connecting to the server");
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                pagenameTextView.setText("Capture an image to continue");
            }
        });
    }

    private void sendAESKeyToServer() {
        Cipher rsaCipher = null;
        try{
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = new byte[0];
            encryptedAesKey = rsaCipher.doFinal(secretKey.getEncoded());
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), encryptedAesKey);
            OkHttpClient client = new OkHttpClient();
            Request request_key = new Request.Builder()
                    .url(getString(R.string.ip_address) + "aesKey")
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request_key).execute();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey generateAESKey() {
        SecretKey aesKey = null;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            aesKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return aesKey;
    }

    private void getRsaPublicKeyFromServer() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(getString(R.string.ip_address))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching public key");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                JsonObject json = new JsonParser().parse(responseBody).getAsJsonObject();
                String publicKeyPEM = json.get("publicKey").getAsString();

                // Load public key
                try {
                    Security.addProvider(new BouncyCastleProvider());

                    PemReader pemReader = new PemReader(new StringReader(publicKeyPEM));
                    PemObject pemObject = pemReader.readPemObject();

                    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemObject.getContent());
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    publicKey = keyFactory.generatePublic(publicKeySpec);
                    sendAESKeyToServer();

                    // ... (Use the public key for encryption, for example)
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    ActivityResultLauncher<Intent> launchImageActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    if(data != null && data.getData() != null){
                        selectedImageUri = data.getData();
                        selectedImagePath = selectedImageUri.getPath();
                        try{
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                                    this.getContentResolver(), selectedImageUri);
                            encrypt();
//                            imageView.setImageBitmap(selectedImageBitmap);
                            decrypt();
                            nextButton.setEnabled(true);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void encrypt() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(selectedImageUri);
        ContextWrapper contextWrapper = new ContextWrapper(getApplication());
        File photoDir = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // on below line creating a file for encrypted file.
        File file = new File(photoDir, "encfile" + ".png");

        // on below line creating a variable for file output stream.
        FileOutputStream fos = new FileOutputStream(file.getPath());

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

    public void decrypt() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        // on below line creating and initializing variable for context wrapper.
        ContextWrapper contextWrapper = new ContextWrapper(getApplication());

        // on below line creating a file for getting photo directory.
        File photoDir = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // on below line creating a new file for encrypted image.
        File file = new File(photoDir, "encfile" + ".png");

        // on below line creating input stream for file with file path.
        FileInputStream fis = new FileInputStream(file.getPath());

        // on below line creating a file for decrypted image.
        File decFile = new File(photoDir, "decfile.png");

        // on below line creating an file output stream for decrypted image.
        FileOutputStream fos = new FileOutputStream(decFile.getPath());

        // creating a variable for secret key and passing our secret key
        // and algorithm for encryption.
//        SecretKeySpec sks = new SecretKeySpec("12345678123456781234567812345678".getBytes(), "AES");

        // on below line creating a variable
        // for cipher and initializing it
        Cipher cipher = Cipher.getInstance("AES");

        // on below line initializing cipher and
        // specifying decrypt mode to decrypt.
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // on below line creating a variable for cipher input stream.
        CipherInputStream cis = new CipherInputStream(fis, cipher);

        // on below line creating a variable b.
        int b;
        byte[] d = new byte[8];
        while ((b = cis.read(d)) != -1) {
            fos.write(d, 0, b);
        }

        // on below line flushing our fos,
        // closing fos and closing cis.
        fos.flush();
        fos.close();
        cis.close();

        // displaying toast message.
        Toast.makeText(this, "File decrypted successfully..", Toast.LENGTH_SHORT).show();

        // on below line creating an image file
        // from decrypted image file path.
        File imgFile = new File(decFile.getPath());
        if (imgFile.exists()) {
            // creating bitmap for image and displaying
            // that bitmap in our image view.
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getPath());
            imageView.setImageBitmap(bitmap);
        }
    }


    public void openGallery(View view) {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        launchImageActivity.launch(intent);
    }


    ActivityResultLauncher<Intent> startCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        imageView.setImageURI(selectedImageUri);

                        nextButton.setEnabled(true);
                    }
                }
            });
    private void openCamera(View view) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(photoFile != null){
            selectedImageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
            startCamera.launch(cameraIntent);
        }

    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//    }

    ActivityResultLauncher<Intent> launchAnotherActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                }
            }
    );

    private void nextPage(View view){
        Intent myIntent = new Intent(MainActivity.this, CategoryActivity.class);
        myIntent.putExtra("BitmapImage", selectedImageUri);
        myIntent.putExtra("SecretKey", secretKey);
        launchAnotherActivity.launch(myIntent);
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private static class PublicKeyResponse {
        public String publicKey;
    }

}