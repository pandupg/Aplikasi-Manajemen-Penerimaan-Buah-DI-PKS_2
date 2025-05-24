package com.example.databuahpks.model;

public class Truck {
    private String kodeTruck;
    private String namaPengemudi;

    public Truck() {} // Wajib untuk Firestore

    public Truck(String kodeTruck, String namaPengemudi) {
        this.kodeTruck = kodeTruck;
        this.namaPengemudi = namaPengemudi;
    }

    public String getKodeTruck() {
        return kodeTruck;
    }

    public void setKodeTruck(String kodeTruck) {
        this.kodeTruck = kodeTruck;
    }

    public String getNamaPengemudi() {
        return namaPengemudi;
    }

    public void setNamaPengemudi(String namaPengemudi) {
        this.namaPengemudi = namaPengemudi;
    }
}