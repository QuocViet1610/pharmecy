package com.medilink.checkup.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "truong")
public class Truong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ten;

    private String diaChi;
    private String nguoiLienHe;
    private String dienThoai;

    protected Truong() {}

    public Truong(String ten, String diaChi, String nguoiLienHe, String dienThoai) {
        this.ten = ten;
        this.diaChi = diaChi;
        this.nguoiLienHe = nguoiLienHe;
        this.dienThoai = dienThoai;
    }

    public Long getId() { return id; }
    public String getTen() { return ten; }
    public String getDiaChi() { return diaChi; }
    public String getNguoiLienHe() { return nguoiLienHe; }
    public String getDienThoai() { return dienThoai; }
}
