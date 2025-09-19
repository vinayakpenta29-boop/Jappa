package com.example.datacapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1888;
    private static final int GALLERY_REQUEST = 1889;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private ImageView imageView;
    private RecyclerView tableView;
    private TableAdapter tableAdapter;
    private ArrayList<TableModel> tableData = new ArrayList<>();
    private OCRManager ocrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button cameraBtn = findViewById(R.id.cameraBtn);
        Button galleryBtn = findViewById(R.id.galleryBtn);

        tableView = findViewById(R.id.tableView);
        tableAdapter = new TableAdapter(tableData);
        tableView.setLayoutManager(new LinearLayoutManager(this));
        tableView.setAdapter(tableAdapter);

        ocrManager = new OCRManager(this, results -> {
            tableData.clear();
            tableData.addAll(results);
            tableAdapter.notifyDataSetChanged();
        });

        cameraBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        galleryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST);
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap photo = null;
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == CAMERA_REQUEST && data.getExtras() != null) {
                photo = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == GALLERY_REQUEST && data.getData() != null) {
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (photo != null) {
            imageView.setImageBitmap(photo);
            ocrManager.processImage(photo);
        }
    }
}
