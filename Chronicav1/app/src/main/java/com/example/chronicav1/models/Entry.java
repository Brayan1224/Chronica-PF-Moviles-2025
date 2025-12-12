package com.example.chronicav1.models;

import java.io.Serializable;

/**
 * Modelo de datos para una entrada del diario
 * Implementa Serializable para poder pasar objetos entre Activities
 */
public class Entry implements Serializable {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String date;
    private String location;
    private String imageBase64;
    private String audioFileName;
    private double latitude;
    private double longitude;
    private long timestamp;

    // Constructor vacío requerido para Firebase
    public Entry() {
    }

    // Constructor completo
    public Entry(String id, String userId, String title, String content,
                 String date, String location, double latitude, double longitude) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.date = date;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageUrl(String imageUrl) {
        this.imageBase64 = imageUrl;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    public void setAudioFileName(String audioUrl) {
        this.audioFileName = audioUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Método para obtener vista previa del contenido
    public String getPreview() {
        if (content != null && content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }
}