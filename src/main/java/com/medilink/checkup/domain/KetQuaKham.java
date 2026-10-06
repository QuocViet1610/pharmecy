package com.medilink.checkup.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kết quả một bàn ghi vào phiếu. Mỗi (phiếu, hạng mục) chỉ có một bản ghi —
 * bác sĩ ghi lại lần hai là sửa kết quả, không tạo dòng mới.
 */
@Entity
@Table(name = "ket_qua_kham",
        uniqueConstraints = @UniqueConstraint(name = "uk_ket_qua", columnNames = {"phieu_kham_id", "hang_muc"}))
public class KetQuaKham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_kham_id")
    private PhieuKham phieuKham;

    @Enumerated(EnumType.STRING)
    @Column(name = "hang_muc", nullable = false)
    private HangMuc hangMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ban_kham_id")
    private BanKham banKham;

    private String bacSi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KetLuanChuyenMon ketLuanChuyenMon;

    /** Chỉ số theo chuyên môn (chieuCao, canNang, thiLucPhai...) — mỗi bàn ghi khóa của riêng mình. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ket_qua_chi_tiet", joinColumns = @JoinColumn(name = "ket_qua_id"))
    @MapKeyColumn(name = "chi_so")
    @Column(name = "gia_tri")
    private Map<String, String> chiTiet = new LinkedHashMap<>();

    @Column(length = 1000)
    private String ghiChu;

    @Column(nullable = false)
    private Instant thoiDiem = Instant.now();

    protected KetQuaKham() {}

    public KetQuaKham(PhieuKham phieuKham, HangMuc hangMuc, BanKham banKham, String bacSi,
                      KetLuanChuyenMon ketLuanChuyenMon, Map<String, String> chiTiet, String ghiChu) {
        this.phieuKham = phieuKham;
        this.hangMuc = hangMuc;
        this.banKham = banKham;
        this.bacSi = bacSi;
        this.ketLuanChuyenMon = ketLuanChuyenMon;
        if (chiTiet != null) this.chiTiet.putAll(chiTiet);
        this.ghiChu = ghiChu;
    }

    public Long getId() { return id; }
    public PhieuKham getPhieuKham() { return phieuKham; }
    public HangMuc getHangMuc() { return hangMuc; }
    public BanKham getBanKham() { return banKham; }
    public String getBacSi() { return bacSi; }
    public KetLuanChuyenMon getKetLuanChuyenMon() { return ketLuanChuyenMon; }
    public Map<String, String> getChiTiet() { return chiTiet; }
    public String getGhiChu() { return ghiChu; }
    public Instant getThoiDiem() { return thoiDiem; }

    /** Bác sĩ sửa kết quả đã ghi (ghi sai bàn, sai chỉ số) — vẫn trong ngày khám. */
    public void ghiLai(BanKham banKham, String bacSi, KetLuanChuyenMon ketLuan,
                       Map<String, String> chiTiet, String ghiChu) {
        this.banKham = banKham;
        this.bacSi = bacSi;
        this.ketLuanChuyenMon = ketLuan;
        this.chiTiet.clear();
        if (chiTiet != null) this.chiTiet.putAll(chiTiet);
        this.ghiChu = ghiChu;
        this.thoiDiem = Instant.now();
    }
}
