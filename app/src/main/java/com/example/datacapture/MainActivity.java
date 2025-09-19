package com.example.datacapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 2101;
    private static final int GALLERY_REQUEST = 2102;
    private static final int CAMERA_PERMISSION_CODE = 1001;

    private ImageView imageView;
    private RecyclerView tableView;
    private TableAdapter tableAdapter;
    private ArrayList<TableModel> tableData = new ArrayList<>();
    private OCRManager ocrManager;
    private Bitmap inputImage = null;
    private MyDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);

        Button importBtn = findViewById(R.id.frontBtn); // now single button, rename in layout as needed

        tableView = findViewById(R.id.tableView);
        tableView.setLayoutManager(new LinearLayoutManager(this));
        tableAdapter = new TableAdapter(tableData);
        tableView.setAdapter(tableAdapter);

        db = Room.databaseBuilder(getApplicationContext(),
                MyDatabase.class, "table_database")
                .allowMainThreadQueries()
                .build();

        loadTableData();

        ocrManager = new OCRManager(this, new OCRManager.OCRResultListener() {
            @Override
            public void onOCRResult(TableModel result) {
                result.no = tableData.size() + 1;
                insertTableRow(result);
                inputImage = null;
                imageView.setImageDrawable(null);
            }
        });

        importBtn.setOnClickListener(v -> selectImage());
    }

    private void selectImage() {
        String[] options = {"Camera", "Gallery"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, CAMERA_REQUEST);
                        }
                    } else {
                        // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, GALLERY_REQUEST);
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, user can retry
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap photo = null;
        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == CAMERA_REQUEST && data.getExtras() != null) {
                    photo = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST && data.getData() != null) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), data.getData());
                        photo = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                            int targetWidth = 600;
                            int targetHeight = 600;
                            int srcWidth = info.getSize().getWidth();
                            int srcHeight = info.getSize().getHeight();
                            float ratio = Math.min((float) targetWidth / srcWidth, (float) targetHeight / srcHeight);
                            if (ratio < 1.0f) {
                                decoder.setTargetSize((int)(srcWidth * ratio), (int)(srcHeight * ratio));
                            }
                        });
                    } else {
                        android.net.Uri uri = data.getData();
                        android.content.ContentResolver resolver = getContentResolver();
                        android.graphics.BitmapFactory.Options onlyBoundsOpts = new android.graphics.BitmapFactory.Options();
                        onlyBoundsOpts.inJustDecodeBounds = true;
                        InputStream input = resolver.openInputStream(uri);
                        android.graphics.BitmapFactory.decodeStream(input, null, onlyBoundsOpts);
                        if (input != null) input.close();

                        int srcWidth = onlyBoundsOpts.outWidth;
                        int srcHeight = onlyBoundsOpts.outHeight;
                        int targetWidth = 600, targetHeight = 600;

                        int inSampleSize = 1;
                        if (srcHeight > targetHeight || srcWidth > targetWidth) {
                            final int halfHeight = srcHeight / 2;
                            final int halfWidth = srcWidth / 2;
                            while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                                inSampleSize *= 2;
                            }
                        }
                        android.graphics.BitmapFactory.Options bitmapOpts = new android.graphics.BitmapFactory.Options();
                        bitmapOpts.inSampleSize = inSampleSize;
                        input = resolver.openInputStream(uri);
                        photo = android.graphics.BitmapFactory.decodeStream(input, null, bitmapOpts);
                        if (input != null) input.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                photo = null;
            }
        }
        if (photo != null) {
            imageView.setImageBitmap(photo);
            inputImage = photo;
            ocrManager.setFrontImage(inputImage); // Reuse the same field/method
            ocrManager.setBackImage(null);
            ocrManager.processBoth(); // For single image use, just implement single image parse inside OCRManager for now!
        } else if (resultCode == RESULT_OK && data != null) {
            Toast.makeText(this, "Could not read image. Try with a different image.", Toast.LENGTH_LONG).show();
        }
    }

    private void insertTableRow(TableModel row) {
        db.tableModelDao().insert(row);
        loadTableData();
    }

    private void loadTableData() {
        tableData.clear();
        List<TableModel> saved = db.tableModelDao().getAll();
        if (saved != null) tableData.addAll(saved);
        tableAdapter.notifyDataSetChanged();
    }
}
