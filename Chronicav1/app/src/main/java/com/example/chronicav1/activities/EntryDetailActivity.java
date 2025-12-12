package com.example.chronicav1.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.chronicav1.R;
import com.example.chronicav1.models.Entry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

/**
 * Activity para mostrar el detalle completo de una entrada del diario
 */
public class EntryDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Vistas
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivEntryImage;
    private TextView tvEntryTitle, tvEntryDate, tvEntryLocation, tvEntryContent;
    private TextView tvAudioDuration, tvDetailLocation, tvCoordinates;
    private CardView cvAudioPlayer;
    private ImageButton btnPlayPause;
    private SeekBar seekBarAudio;
    private MaterialButton btnEditEntry, btnDeleteEntry;
    private GoogleMap mMap;

    // Datos
    private Entry entry;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Obtener entrada desde intent
        entry = (Entry) getIntent().getSerializableExtra("ENTRY");

        if (entry == null) {
            Toast.makeText(this, "Error al cargar entrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar vistas
        initViews();

        // Configurar toolbar
        setupToolbar();

        // Configurar mapa
        setupMap();

        // Cargar datos
        loadEntryData();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivEntryImage = findViewById(R.id.ivEntryImage);
        tvEntryTitle = findViewById(R.id.tvEntryTitle);
        tvEntryDate = findViewById(R.id.tvEntryDate);
        tvEntryLocation = findViewById(R.id.tvEntryLocation);
        tvEntryContent = findViewById(R.id.tvEntryContent);
        cvAudioPlayer = findViewById(R.id.cvAudioPlayer);
        tvAudioDuration = findViewById(R.id.tvAudioDuration);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBarAudio = findViewById(R.id.seekBarAudio);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        btnEditEntry = findViewById(R.id.btnEditEntry);
        btnDeleteEntry = findViewById(R.id.btnDeleteEntry);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        collapsingToolbar.setTitle(entry.getTitle());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragmentDetail);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Mostrar ubicación de la entrada
        if (entry.getLatitude() != 0 && entry.getLongitude() != 0) {
            LatLng location = new LatLng(entry.getLatitude(), entry.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(entry.getTitle()));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private void loadEntryData() {
        // Título y contenido
        tvEntryTitle.setText(entry.getTitle());
        tvEntryContent.setText(entry.getContent());
        tvEntryDate.setText(entry.getDate());
        tvEntryLocation.setText(entry.getLocation());

        // Coordenadas
        tvDetailLocation.setText(entry.getLocation());
        String coordinates = String.format("%.4f°N, %.4f°W",
                entry.getLatitude(), entry.getLongitude());
        tvCoordinates.setText(coordinates);

        // Cargar imagen desde Base64 si existe
        if (entry.getImageBase64() != null && !entry.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(entry.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                ivEntryImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                ivEntryImage.setImageResource(R.drawable.ic_logo);
            }
        } else {
            ivEntryImage.setImageResource(R.drawable.ic_logo);
        }

        // Configurar audio si existe
        if (entry.getAudioFileName() != null && !entry.getAudioFileName().isEmpty()) {
            cvAudioPlayer.setVisibility(View.VISIBLE);
            setupAudioPlayer();
        } else {
            cvAudioPlayer.setVisibility(View.GONE);
        }
    }

    private void setupAudioPlayer() {
        mediaPlayer = new MediaPlayer();

        try {
            File audioFile = new File(getFilesDir(), "audio/" + entry.getAudioFileName());

            if (audioFile.exists()) {

                mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    int duration = mp.getDuration();
                    seekBarAudio.setMax(duration);
                    tvAudioDuration.setText(formatTime(duration));
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    seekBarAudio.setProgress(0);
                });

            } else {
                Toast.makeText(this, "Audio no encontrado", Toast.LENGTH_SHORT).show();
                cvAudioPlayer.setVisibility(View.GONE);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar audio", Toast.LENGTH_SHORT).show();
            cvAudioPlayer.setVisibility(View.GONE);
        }
    }


    private void setupListeners() {
        // Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (isPlaying) {
                    pauseAudio();
                } else {
                    playAudio();
                }
            }
        });

        // SeekBar
        seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Botón Editar
        btnEditEntry.setOnClickListener(v -> {
            Intent intent = new Intent(EntryDetailActivity.this, EditEntryActivity.class);
            intent.putExtra("ENTRY", entry);
            startActivity(intent);
        });

        // Botón Eliminar
        btnDeleteEntry.setOnClickListener(v -> showDeleteDialog());
    }

    private void playAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            seekBarAudio.setProgress(mediaPlayer.getCurrentPosition());
            handler.postDelayed(this::updateSeekBar, 100);
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar entrada")
                .setMessage("¿Estás seguro de que deseas eliminar esta entrada? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteEntry())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteEntry() {
        // Mostrar progreso
        btnDeleteEntry.setEnabled(false);
        btnDeleteEntry.setText("Eliminando...");

        // Eliminar de Firestore
        db.collection("entries").document(entry.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Eliminar imagen si existe
                    if (entry.getImageBase64() != null && !entry.getImageBase64().isEmpty()) {
                        deleteFileFromStorage(entry.getImageBase64());
                    }

                    // Eliminar audio si existe
                    if (entry.getAudioFileName() != null && !entry.getAudioFileName().isEmpty()) {
                        deleteFileFromStorage(entry.getAudioFileName());
                    }

                    Toast.makeText(this, "Entrada eliminada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnDeleteEntry.setEnabled(true);
                    btnDeleteEntry.setText("Eliminar");
                });
    }

    private void deleteFileFromStorage(String fileUrl) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);
            fileRef.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && isPlaying) {
            pauseAudio();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos de la entrada por si fue editada
        reloadEntryData();
    }

    private void reloadEntryData() {
        db.collection("entries").document(entry.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Entry updatedEntry = documentSnapshot.toObject(Entry.class);
                        if (updatedEntry != null) {
                            updatedEntry.setId(documentSnapshot.getId());
                            entry = updatedEntry;
                            loadEntryData();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently fail, keep showing current data
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}