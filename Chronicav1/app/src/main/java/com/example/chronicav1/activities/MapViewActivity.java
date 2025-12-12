package com.example.chronicav1.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity que muestra todas las entradas en un mapa
 */
public class MapViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Vistas
    private Toolbar toolbar;
    private GoogleMap mMap;
    private CardView cvMarkerInfo;
    private ImageView ivMarkerImage;
    private TextView tvMarkerTitle, tvMarkerDate, tvMarkerLocation;
    private ImageButton btnViewEntry;

    // Datos
    private List<Entry> entryList;
    private Map<Marker, Entry> markerEntryMap;
    private Entry selectedEntry;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Inicializar listas
        entryList = new ArrayList<>();
        markerEntryMap = new HashMap<>();

        // Inicializar vistas
        initViews();

        // Configurar toolbar
        setupToolbar();

        // Configurar mapa
        setupMap();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        cvMarkerInfo = findViewById(R.id.cvMarkerInfo);
        ivMarkerImage = findViewById(R.id.ivMarkerImage);
        tvMarkerTitle = findViewById(R.id.tvMarkerTitle);
        tvMarkerDate = findViewById(R.id.tvMarkerDate);
        tvMarkerLocation = findViewById(R.id.tvMarkerLocation);
        btnViewEntry = findViewById(R.id.btnViewEntry);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Lugares");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragmentView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar listener de marcadores
        mMap.setOnMarkerClickListener(marker -> {
            Entry entry = markerEntryMap.get(marker);
            if (entry != null) {
                showMarkerInfo(entry);
            }
            return true;
        });

        // Listener cuando se hace click en el mapa (no en un marcador)
        mMap.setOnMapClickListener(latLng -> {
            cvMarkerInfo.setVisibility(View.GONE);
        });

        // Cargar entradas y marcadores
        loadEntriesWithLocation();
    }

    private void setupListeners() {
        btnViewEntry.setOnClickListener(v -> {
            if (selectedEntry != null) {
                Intent intent = new Intent(MapViewActivity.this, EntryDetailActivity.class);
                intent.putExtra("ENTRY", selectedEntry);
                startActivity(intent);
            }
        });
    }

    private void loadEntriesWithLocation() {
        db.collection("entries")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entryList.clear();
                    markerEntryMap.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No hay entradas con ubicación",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    boolean hasValidLocation = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Entry entry = document.toObject(Entry.class);
                        entry.setId(document.getId());

                        // Solo agregar si tiene ubicación válida
                        if (entry.getLatitude() != 0 && entry.getLongitude() != 0) {
                            entryList.add(entry);

                            // Crear marcador
                            LatLng position = new LatLng(entry.getLatitude(), entry.getLongitude());
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(entry.getTitle())
                                    .snippet(entry.getDate()));

                            // Asociar marcador con entrada
                            markerEntryMap.put(marker, entry);

                            // Agregar al builder de bounds
                            boundsBuilder.include(position);
                            hasValidLocation = true;
                        }
                    }

                    // Ajustar cámara para mostrar todos los marcadores
                    if (hasValidLocation) {
                        try {
                            LatLngBounds bounds = boundsBuilder.build();
                            int padding = 100; // padding en píxeles
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                        } catch (IllegalStateException e) {
                            // Si solo hay un marcador, hacer zoom normal
                            if (!entryList.isEmpty()) {
                                Entry firstEntry = entryList.get(0);
                                LatLng position = new LatLng(firstEntry.getLatitude(),
                                        firstEntry.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12));
                            }
                        }

                        Toast.makeText(this,
                                entryList.size() + " entrada(s) encontrada(s)",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "No hay entradas con ubicación válida",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al cargar entradas: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showMarkerInfo(Entry entry) {
        selectedEntry = entry;

        // Mostrar información
        tvMarkerTitle.setText(entry.getTitle());
        tvMarkerDate.setText(entry.getDate());
        tvMarkerLocation.setText(entry.getLocation());

        // Cargar imagen
        if (entry.getImageBase64() != null && !entry.getImageBase64().isEmpty()) {
            Glide.with(this)
                    .load(entry.getImageBase64())
                    .centerCrop()
                    .placeholder(R.drawable.ic_logo)
                    .into(ivMarkerImage);
        } else {
            ivMarkerImage.setImageResource(R.drawable.ic_logo);
        }

        // Mostrar tarjeta
        cvMarkerInfo.setVisibility(View.VISIBLE);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        // Si la tarjeta está visible, ocultarla primero
        if (cvMarkerInfo.getVisibility() == View.VISIBLE) {
            cvMarkerInfo.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}