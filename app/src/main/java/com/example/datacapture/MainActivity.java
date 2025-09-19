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
    private static final int FRONT_CAMERA_REQUEST = 2101;
    private static final int BACK_CAMERA_REQUEST = 2102;
    private static final int FRONT_GALLERY_REQUEST = 2103;
    private static final int BACK_GALLERY_REQUEST = 2104;
    private static final int CAMERA_PERMISSION_CODE = 1001;

    private ImageView imageView;
    private RecyclerView tableView;
    private TableAdapter tableAdapter;
    private ArrayList<TableModel> tableData = new ArrayList<>();
    private OCRManager ocrManager;
    private Bitmap frontImage = null;
    private Bitmap backImage = null;

    // Room database
    private MyDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);

        // Two buttons for Front and Back
        Button frontBtn = findViewById(R.id.frontBtn);
        Button backBtn = findViewById(R.id.backBtn);

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
                frontImage = null;
                backImage = null;
                imageView.setImageDrawable(null);
            }
        });

        frontBtn.setOnClickListener(v -> selectImage(true));
        backBtn.setOnClickListener(v -> selectImage(false));
    }

    private void selectImage(boolean isFront) {
        String[] options = {"Camera", "Gallery"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(isFront ? "Select Front Side" : "Select Back Side")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, isFront ? FRONT_CAMERA_REQUEST : BACK_CAMERA_REQUEST);
                        }
                    } else {
                        // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, isFront ? FRONT_GALLERY_REQUEST : BACK_GALLERY_REQUEST);
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
            if ((requestCode == FRONT_CAMERA_REQUEST || requestCode == BACK_CAMERA_REQUEST) && data.getExtras() != null) {
                photo = (Bitmap) data.getExtras().get("data");
            } else if ((requestCode == FRONT_GALLERY_REQUEST || requestCode == BACK_GALLERY_REQUEST) && data.getData() != null) {
                try {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), data.getData());
                        photo = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                            // Target size (max 1000x1000 to avoid crash; adjust as needed)
                            int targetWidth = 1000;
                            int targetHeight = 1000;
                            int srcWidth = info.getSize().getWidth();
                            int srcHeight = info.getSize().getHeight();
                            float ratio = Math.min((float)targetWidth / srcWidth, (float)targetHeight / srcHeight);
                            if (ratio < 1.0f) {
                                decoder.setTargetSize((int)(srcWidth * ratio), (int)(srcHeight * ratio));
                            }
                        });
                    } else {
                        // For API < 29; downsample bitmap
                        android.net.Uri uri = data.getData();
                        android.content.ContentResolver resolver = getContentResolver();
                        android.graphics.BitmapFactory.Options onlyBoundsOpts = new android.graphics.BitmapFactory.Options();
                        onlyBoundsOpts.inJustDecodeBounds = true;
                        InputStream input = resolver.openInputStream(uri);
                        android.graphics.BitmapFactory.decodeStream(input, null, onlyBoundsOpts);
                        input.close();

                        int srcWidth = onlyBoundsOpts.outWidth;
                        int srcHeight = onlyBoundsOpts.outHeight;
                        int targetWidth = 1000, targetHeight = 1000;

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
                        input.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (photo != null) {
            imageView.setImageBitmap(photo);
            if (requestCode == FRONT_CAMERA_REQUEST || requestCode == FRONT_GALLERY_REQUEST) {
                frontImage = photo;
            } else if (requestCode == BACK_CAMERA_REQUEST || requestCode == BACK_GALLERY_REQUEST) {
                backImage = photo;
            }
            if (frontImage != null && backImage != null) {
                ocrManager.setFrontImage(frontImage);
                ocrManager.setBackImage(backImage);
                ocrManager.processBoth();
            }
        }
    }

    // --- ROOM Persistence Methods ---

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
