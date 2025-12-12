package com.example.chronicav1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.chronicav1.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewEntryActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 102;
    private static final int REQUEST_AUDIO_PERMISSION = 103;
    private static final int REQUEST_PICK_IMAGE = 200;
    private static final int REQUEST_CAPTURE_IMAGE = 201;
    private static final int MAX_AUDIO_DURATION = 120000;

    private Toolbar toolbar;
    private TextInputEditText etTitle, etContent;
    private CardView cvImagePreview, cvCamera, cvMicrophone, cvLocation, cvMapPreview;
    private ImageView ivImagePreview, ivMicIcon;
    private ImageButton btnRemoveImage;
    private TextView tvMicText, tvLocationText;
    private MaterialButton btnSaveEntry;
    private GoogleMap mMap;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private Uri imageUri;
    private Bitmap selectedBitmap;
    private String audioPath;
    private double latitude = 0;
    private double longitude = 0;
    private String locationAddress = "";
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private Handler handler = new Handler();

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupToolbar();
        setupListeners();
        setupMap();
        getCurrentLocation();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        cvImagePreview = findViewById(R.id.cvImagePreview);
        ivImagePreview = findViewById(R.id.ivImagePreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        cvCamera = findViewById(R.id.cvCamera);
        cvMicrophone = findViewById(R.id.cvMicrophone);
        cvLocation = findViewById(R.id.cvLocation);
        cvMapPreview = findViewById(R.id.cvMapPreview);
        ivMicIcon = findViewById(R.id.ivMicIcon);
        tvMicText = findViewById(R.id.tvMicText);
        tvLocationText = findViewById(R.id.tvLocationText);
        btnSaveEntry = findViewById(R.id.btnSaveEntry);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nueva Entrada");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        cvCamera.setOnClickListener(v -> showImageOptions());
        cvMicrophone.setOnClickListener(v -> toggleRecording());
        cvLocation.setOnClickListener(v -> getCurrentLocation());
        btnSaveEntry.setOnClickListener(v -> saveEntry());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (latitude != 0 && longitude != 0) {
            updateMapLocation(latitude, longitude);
        }
    }

    private void updateMapLocation(double lat, double lng) {
        if (mMap != null) {
            LatLng location = new LatLng(lat, lng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(location).title("Mi ubicación"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                        updateMapLocation(latitude, longitude);
                        Toast.makeText(this, "Ubicación obtenida", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                locationAddress = address.getLocality() + ", " + address.getCountryName();
                tvLocationText.setText(locationAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
            locationAddress = "Lat: " + lat + ", Lng: " + lng;
            tvLocationText.setText(locationAddress);
        }
    }

    private void showImageOptions() {
        String[] options = {"Tomar foto", "Seleccionar de galería"};

        new AlertDialog.Builder(this)
                .setTitle("Agregar imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndCapture();
                    } else {
                        checkStoragePermissionAndPick();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            captureImage();
        }
    }

    private void checkStoragePermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                pickImage();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                pickImage();
            }
        } else {
            pickImage();
        }
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = Intent.createChooser(takePictureIntent, "Tomar foto");
        startActivityForResult(chooser, REQUEST_CAPTURE_IMAGE);
    }

    private void pickImage() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        Intent chooser = Intent.createChooser(pickIntent, "Seleccionar imagen");
        startActivityForResult(chooser, REQUEST_PICK_IMAGE);
    }

    private void removeImage() {
        imageUri = null;
        selectedBitmap = null;
        cvImagePreview.setVisibility(View.GONE);
        Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show();
    }

    private void toggleRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
            return;
        }

        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            File audioDir = new File(getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String audioFileName = "AUDIO_" + timeStamp + ".3gp";
            File audioFile = new File(audioDir, audioFileName);
            audioPath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            tvMicText.setText("Grabando...");
            ivMicIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));

            handler.postDelayed(() -> {
                if (isRecording) {
                    stopRecording();
                    Toast.makeText(this, "Grabación máxima: 2 minutos", Toast.LENGTH_SHORT).show();
                }
            }, MAX_AUDIO_DURATION);

            Toast.makeText(this, "Grabando audio (máx 2 min)", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al grabar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                isRecording = false;
                tvMicText.setText("Audio grabado ✓");
                ivMicIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                handler.removeCallbacksAndMessages(null);

                Toast.makeText(this, "Audio guardado", Toast.LENGTH_SHORT).show();

            } catch (RuntimeException e) {
                e.printStackTrace();
                audioPath = null;
                Toast.makeText(this, "Error al detener grabación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveEntry() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("El título es requerido");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            etContent.setError("El contenido es requerido");
            etContent.requestFocus();
            return;
        }

        btnSaveEntry.setEnabled(false);
        btnSaveEntry.setText("Guardando...");

        String entryId = db.collection("entries").document().getId();
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis();

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("id", entryId);
        entryData.put("userId", currentUserId);
        entryData.put("title", title);
        entryData.put("content", content);
        entryData.put("date", date);
        entryData.put("location", locationAddress);
        entryData.put("latitude", latitude);
        entryData.put("longitude", longitude);
        entryData.put("timestamp", timestamp);

        if (selectedBitmap != null) {
            try {
                Bitmap resized = resizeBitmap(selectedBitmap, 800, 800);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] imageBytes = baos.toByteArray();

                // Verificar tamaño real antes de Base64
                if (imageBytes.length > 750_000) {
                    Toast.makeText(this, "Imagen muy grande. Usa una más pequeña.", Toast.LENGTH_LONG).show();
                    btnSaveEntry.setEnabled(true);
                    btnSaveEntry.setText("Guardar Entrada");
                    return;
                }

                // Codificar sin saltos de línea
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                entryData.put("imageBase64", base64Image);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show();
            }
        }


        if (audioPath != null && !audioPath.isEmpty()) {
            String newName = entryId + ".3gp";

            File oldFile = new File(audioPath);  // el archivo grabado
            File newFile = new File(getFilesDir(), "audio/" + newName);

            if (oldFile.renameTo(newFile)) {
                entryData.put("audioFileName", newName);  // SE GUARDA EN FIREBASE
            }

        }

        db.collection("entries").document(entryId)
                .set(entryData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Entrada guardada ✓", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveEntry.setEnabled(true);
                    btnSaveEntry.setText("Guardar Entrada");
                });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap.getWidth() <= maxWidth && bitmap.getHeight() <= maxHeight) {
            return bitmap;
        }

        float scale = Math.min(
                (float) maxWidth / bitmap.getWidth(),
                (float) maxHeight / bitmap.getHeight()
        );

        int newWidth = Math.round(bitmap.getWidth() * scale);
        int newHeight = Math.round(bitmap.getHeight() * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_PICK_IMAGE) {
                    imageUri = data.getData();
                    if (imageUri != null) {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        ivImagePreview.setImageBitmap(selectedBitmap);
                        cvImagePreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        selectedBitmap = (Bitmap) extras.get("data");
                        ivImagePreview.setImageBitmap(selectedBitmap);
                        cvImagePreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Foto capturada", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_CAMERA_PERMISSION:
                    captureImage();
                    break;
                case REQUEST_STORAGE_PERMISSION:
                    pickImage();
                    break;
                case REQUEST_LOCATION_PERMISSION:
                    getCurrentLocation();
                    break;
                case REQUEST_AUDIO_PERMISSION:
                    startRecording();
                    break;
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
