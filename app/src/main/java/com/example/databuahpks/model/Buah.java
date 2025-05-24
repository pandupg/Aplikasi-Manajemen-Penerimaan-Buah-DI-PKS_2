package com.example.databuahpks.model;

public class Buah {
    private String kodeTruck;
    private String namaPengemudi;
    private double beratDatang;
    private double beratPulang;
    private int jumlahMatang;
    private int jumlahLewatMatang;
    private int jumlahMentah;
    private int jumlahBusuk;
    private String tanggalInput;
    private String waktuInput;

    public Buah() {} // Wajib untuk Firestore

    public Buah(String kodeTruck, String namaPengemudi, double beratDatang, double beratPulang,
                int jumlahMatang, int jumlahLewatMatang, int jumlahMentah, int jumlahBusuk,
                String tanggalInput, String waktuInput) {
        this.kodeTruck = kodeTruck;
        this.namaPengemudi = namaPengemudi;
        this.beratDatang = beratDatang;
        this.beratPulang = beratPulang;
        this.jumlahMatang = jumlahMatang;
        this.jumlahLewatMatang = jumlahLewatMatang;
        this.jumlahMentah = jumlahMentah;
        this.jumlahBusuk = jumlahBusuk;
        this.tanggalInput = tanggalInput;
        this.waktuInput = waktuInput;
    }

    // Getter dan Setter (wajib semua field)
    public String getKodeTruck() { return kodeTruck; }
    public void setKodeTruck(String kodeTruck) { this.kodeTruck = kodeTruck; }

    public String getNamaPengemudi() { return namaPengemudi; }
    public void setNamaPengemudi(String namaPengemudi) { this.namaPengemudi = namaPengemudi; }

    public double getBeratDatang() { return beratDatang; }
    public void setBeratDatang(double beratDatang) { this.beratDatang = beratDatang; }

    public double getBeratPulang() { return beratPulang; }
    public void setBeratPulang(double beratPulang) { this.beratPulang = beratPulang; }

    public int getJumlahMatang() { return jumlahMatang; }
    public void setJumlahMatang(int jumlahMatang) { this.jumlahMatang = jumlahMatang; }

    public int getJumlahLewatMatang() { return jumlahLewatMatang; }
    public void setJumlahLewatMatang(int jumlahLewatMatang) { this.jumlahLewatMatang = jumlahLewatMatang; }

    public int getJumlahMentah() { return jumlahMentah; }
    public void setJumlahMentah(int jumlahMentah) { this.jumlahMentah = jumlahMentah; }

    public int getJumlahBusuk() { return jumlahBusuk; }
    public void setJumlahBusuk(int jumlahBusuk) { this.jumlahBusuk = jumlahBusuk; }

    public String getTanggalInput() { return tanggalInput; }
    public void setTanggalInput(String tanggalInput) { this.tanggalInput = tanggalInput; }

    public String getWaktuInput() { return waktuInput; }
    public void setWaktuInput(String waktuInput) { this.waktuInput = waktuInput; }
}