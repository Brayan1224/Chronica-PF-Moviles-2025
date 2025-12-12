package com.example.chronicav1.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chronicav1.R;
import com.example.chronicav1.adapters.EntryAdapter;
import com.example.chronicav1.models.Entry;
import com.example.chronicav1.utils.PermissionsHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity principal que muestra la lista de entradas del diario
 */
public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etSearch;
    private RecyclerView rvEntries;
    private FloatingActionButton fabNewEntry;

    private EntryAdapter entryAdapter;
    private List<Entry> entryList;
    private List<Entry> filteredList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Verificar usuario autenticado
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();

        // Solicitar permisos si no están concedidos
        if (!PermissionsHelper.hasAllPermissions(this)) {
            showPermissionsDialog();
        }

        // Inicializar vistas
        initViews();

        // Configurar Toolbar
        setupToolbar();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar FAB
        setupFab();

        // Configurar búsqueda
        setupSearch();

        // Cargar entradas desde Firebase
        loadEntriesFromFirebase();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        rvEntries = findViewById(R.id.rvEntries);
        fabNewEntry = findViewById(R.id.fabNewEntry);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mis Recuerdos");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Cerrar sesión en Firebase
                    mAuth.signOut();

                    // Mostrar mensaje
                    Toast.makeText(MainActivity.this,
                            "Sesión cerrada correctamente",
                            Toast.LENGTH_SHORT).show();

                    // Ir al login
                    goToLogin();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupRecyclerView() {
        entryList = new ArrayList<>();
        filteredList = new ArrayList<>();
        entryAdapter = new EntryAdapter(this, filteredList);

        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(entryAdapter);

        // Listener para clicks en las entradas
        entryAdapter.setOnItemClickListener(entry -> {
            Intent intent = new Intent(MainActivity.this, EntryDetailActivity.class);
            intent.putExtra("ENTRY", entry);
            startActivity(intent);
        });
    }

    private void setupFab() {
        fabNewEntry.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEntries(String searchText) {
        filteredList.clear();

        if (searchText.isEmpty()) {
            filteredList.addAll(entryList);
        } else {
            String searchLower = searchText.toLowerCase();
            for (Entry entry : entryList) {
                if (entry.getTitle().toLowerCase().contains(searchLower) ||
                        entry.getContent().toLowerCase().contains(searchLower) ||
                        entry.getLocation().toLowerCase().contains(searchLower)) {
                    filteredList.add(entry);
                }
            }
        }

        entryAdapter.notifyDataSetChanged();
    }

    private void loadEntriesFromFirebase() {
        db.collection("entries")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entryList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Entry entry = document.toObject(Entry.class);
                        entry.setId(document.getId());
                        entryList.add(entry);
                    }

                    filteredList.clear();
                    filteredList.addAll(entryList);
                    entryAdapter.notifyDataSetChanged();

                    if (entryList.isEmpty()) {
                        Toast.makeText(this, "No hay entradas. ¡Crea tu primera entrada!",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar entradas: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showPermissionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage("Chronica necesita acceso a:\n\n" +
                        "• Cámara - Para tomar fotos\n" +
                        "• Micrófono - Para grabar audio\n" +
                        "• Ubicación - Para registrar lugares\n" +
                        "• Fotos - Para seleccionar imágenes")
                .setPositiveButton("Conceder permisos", (dialog, which) -> {
                    PermissionsHelper.requestAllPermissions(MainActivity.this);
                })
                .setNegativeButton("Más tarde", null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsHelper.REQUEST_ALL_PERMISSIONS) {
            List<String> deniedPermissions = PermissionsHelper.getDeniedPermissions(this);

            if (deniedPermissions.isEmpty()) {
                Toast.makeText(this, "Todos los permisos concedidos ✓", Toast.LENGTH_SHORT).show();
            } else {
                String message = "Permisos denegados:\n" + String.join(", ", deniedPermissions);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Mostrar diálogo para ir a configuración
                new AlertDialog.Builder(this)
                        .setTitle("Permisos requeridos")
                        .setMessage("Algunos permisos fueron denegados. Puedes habilitarlos manualmente en Configuración → Apps → Chronica → Permisos")
                        .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando vuelvas a esta actividad
        if (currentUserId != null) {
            loadEntriesFromFirebase();
        }
    }
}