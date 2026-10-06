package com.medilink.checkup.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Học sinh do nhà trường bàn giao. {@code maDinhDanh} là khóa nghiệp vụ, duy nhất trong một trường. */
@Entity
@Table(name = "hoc_sinh",
        uniqueConstraints = @UniqueConstraint(name = "uk_hoc_sinh_ma", columnNames = {"truong_id", "ma_dinh_danh"}))
public class HocSinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truong_id")
    private Truong truong;

    @Column(name = "ma_dinh_danh", nullable = false)
    private String maDinhDanh;

    @Column(nullable = false)
    private String hoTen;

    @Column(nullable = false)
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GioiTinh gioiTinh;

    @Column(nullable = false)
    private String lop;

    @Column(nullable = false)
    private String khoi;

    private String hoTenPhuHuynh;
    private String dienThoaiPhuHuynh;

    protected HocSinh() {}

    public HocSinh(Truong truong, String maDinhDanh, String hoTen, LocalDate ngaySinh,
                   GioiTinh gioiTinh, String lop, String khoi,
                   String hoTenPhuHuynh, String dienThoaiPhuHuynh) {
        this.truong = truong;
        this.maDinhDanh = maDinhDanh;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.lop = lop;
        this.khoi = khoi;
        this.hoTenPhuHuynh = hoTenPhuHuynh;
        this.dienThoaiPhuHuynh = dienThoaiPhuHuynh;
    }

    public Long getId() { return id; }
    public Truong getTruong() { return truong; }
    public String getMaDinhDanh() { return maDinhDanh; }
    public String getHoTen() { return hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public GioiTinh getGioiTinh() { return gioiTinh; }
    public String getLop() { return lop; }
    public String getKhoi() { return khoi; }
    public String getHoTenPhuHuynh() { return hoTenPhuHuynh; }
    public String getDienThoaiPhuHuynh() { return dienThoaiPhuHuynh; }
}
