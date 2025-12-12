package com.example.chronicav1.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper para gestionar permisos de la app
 */
public class PermissionsHelper {

    public static final int REQUEST_ALL_PERMISSIONS = 999;

    /**
     * Obtiene todos los permisos necesarios según la versión de Android
     */
    public static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Permisos comunes para todas las versiones
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Permisos de almacenamiento según versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            // Android 12 y menores
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                // Android 9 y menores
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        return permissions.toArray(new String[0]);
    }

    /**
     * Verifica si todos los permisos están concedidos
     */
    public static boolean hasAllPermissions(Activity activity) {
        String[] permissions = getRequiredPermissions();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Solicita todos los permisos necesarios
     */
    public static void requestAllPermissions(Activity activity) {
        String[] permissions = getRequiredPermissions();
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_ALL_PERMISSIONS);
    }

    /**
     * Obtiene lista de permisos denegados
     */
    public static List<String> getDeniedPermissions(Activity activity) {
        List<String> deniedPermissions = new ArrayList<>();
        String[] permissions = getRequiredPermissions();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(getPermissionName(permission));
            }
        }

        return deniedPermissions;
    }

    /**
     * Convierte el nombre técnico del permiso a uno legible
     */
    private static String getPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Cámara";
            case Manifest.permission.RECORD_AUDIO:
                return "Micrófono";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Ubicación";
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Fotos y videos";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Almacenamiento";
            default:
                return permission;
        }
    }

    /**
     * Verifica permiso específico de cámara
     */
    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verifica permiso específico de almacenamiento
     */
    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Verifica permiso específico de audio
     */
    public static boolean hasAudioPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verifica permiso específico de ubicación
     */
    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}