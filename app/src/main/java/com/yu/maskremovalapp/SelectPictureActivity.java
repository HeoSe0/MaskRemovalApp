package com.yu.maskremovalapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SelectPictureActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 0;
    private final String SERVER_URL = "http://222.104.195.204:10005/";

    private static int isSelected = 0;
    private int serverResponseCode = 0;

    private ImageView selectedImage;
    private Button reselectButton;
    private Button confirmButton;

    private ProgressDialog dialog = null;
    private TextView messageText;

    private static String imagePath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();
                    if (uri != null) {
                        selectedImage.setImageURI(uri);
                        imagePath = getRealPathFromURI(uri);
                    }
                    isSelected = 1;
                } catch (Exception e) {

                }
            } else if (resultCode == RESULT_CANCELED && isSelected == 0) {
                Toast.makeText(SelectPictureActivity.this, "사진 선택 취소", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }
        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        try (Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null)) {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        }
        return null;
    }

    public int uploadFile(String sourceFileUri) {


        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :" + sourceFileUri);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :" + sourceFileUri);
                }
            });

            return 0;

        }
        else
        {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(SERVER_URL + "UploadToServer.php");

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                // 이미지 파일 이름 설정 (image.jpg)
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + "image.jpg" + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                Log.d("FILENAME", fileName);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SelectPictureActivity.this, "처리 완료",
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(SelectPictureActivity.this, "MalformedURLException",
                                                              Toast.LENGTH_SHORT).show();
                    }
                });

                  Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(SelectPictureActivity.this, "Got Exception : see logcat ",
                                  Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server Exception", "Exception : "
                                                   + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_picture);

        selectedImage = findViewById(R.id.selected_image);
        messageText = findViewById(R.id.status_message);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_CODE);

        reselectButton = findViewById(R.id.reselect_button);
        reselectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(SelectPictureActivity.this, "", "처리중입니다.", true);
                new Thread(new Runnable() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                            }
                        });
                        uploadFile(imagePath);
                    }
                }).start();
                new ImageDownloadHelper(selectedImage).execute();

                // 이미지 이름에 들어갈 현재시간
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy_MM_dd_HH_mm_ss");
                String fileName = "RM_" + simpleDateFormat.format(date);

                // 저장 권한문제 해결 필요..
//                saveImage(selectedImage, fileName);
            }
        });
    }

    private void saveImage(View v, String filename){

        String storagePath =
                Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.i("STORAGE PATH", storagePath);
        String savePath = storagePath + "/Mask_Removal";
        File f = new File(savePath);

        // 권한문제 해결 필요.. mkdirs가 작동안함

        if (!f.isDirectory())f.mkdirs();

        v.buildDrawingCache();
        Bitmap bitmap = v.getDrawingCache();
        FileOutputStream fos;
        try{
            fos = new FileOutputStream(savePath+"/"+filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
