package com.medilink.checkup.domain;

import jakarta.persistence.*;

/** Bàn/trạm khám được setup tại trường. Một hạng mục có thể có nhiều bàn để giảm thời gian chờ. */
@Entity
@Table(name = "ban_kham")
public class BanKham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dot_kham_id")
    private DotKham dotKham;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HangMuc hangMuc;

    @Column(nullable = false)
    private String ten;

    private String nhanSu;

    protected BanKham() {}

    public BanKham(DotKham dotKham, HangMuc hangMuc, String ten, String nhanSu) {
        this.dotKham = dotKham;
        this.hangMuc = hangMuc;
        this.ten = ten;
        this.nhanSu = nhanSu;
    }

    public Long getId() { return id; }
    public DotKham getDotKham() { return dotKham; }
    public HangMuc getHangMuc() { return hangMuc; }
    public String getTen() { return ten; }
    public String getNhanSu() { return nhanSu; }

    public void doiNhanSu(String nhanSu) { this.nhanSu = nhanSu; }
}
