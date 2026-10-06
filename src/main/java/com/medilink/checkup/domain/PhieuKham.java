package com.medilink.checkup.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Phiếu khám điện tử — bản sao số của tờ phiếu học sinh cầm đi giữa các bàn.
 * Phiếu <em>không</em> giữ thứ tự khám: thứ tự nằm ở thời điểm của từng {@link KetQuaKham}.
 */
@Entity
@Table(name = "phieu_kham",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_phieu_so", columnNames = "so_phieu"),
                @UniqueConstraint(name = "uk_phieu_hoc_sinh", columnNames = {"dot_kham_id", "hoc_sinh_id"})
        })
public class PhieuKham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dot_kham_id")
    private DotKham dotKham;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoc_sinh_id")
    private HocSinh hocSinh;

    /** In trên phiếu giấy và encode vào QR — dùng để nhận diện học sinh tại bàn. */
    @Column(name = "so_phieu", nullable = false)
    private String soPhieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiPhieu trangThai = TrangThaiPhieu.CHUA_KHAM;

    @Enumerated(EnumType.STRING)
    private PhanLoaiSucKhoe phanLoaiSucKhoe;

    @Column(length = 2000)
    private String ketLuan;

    private String nguoiKetLuan;
    private Instant thoiDiemKetLuan;

    protected PhieuKham() {}

    public PhieuKham(DotKham dotKham, HocSinh hocSinh, String soPhieu) {
        this.dotKham = dotKham;
        this.hocSinh = hocSinh;
        this.soPhieu = soPhieu;
    }

    public Long getId() { return id; }
    public DotKham getDotKham() { return dotKham; }
    public HocSinh getHocSinh() { return hocSinh; }
    public String getSoPhieu() { return soPhieu; }
    public TrangThaiPhieu getTrangThai() { return trangThai; }
    public PhanLoaiSucKhoe getPhanLoaiSucKhoe() { return phanLoaiSucKhoe; }
    public String getKetLuan() { return ketLuan; }
    public String getNguoiKetLuan() { return nguoiKetLuan; }
    public Instant getThoiDiemKetLuan() { return thoiDiemKetLuan; }

    public void capNhatTrangThai(TrangThaiPhieu moi) { this.trangThai = moi; }

    public void ketLuan(PhanLoaiSucKhoe phanLoai, String ketLuan, String nguoiKetLuan) {
        this.phanLoaiSucKhoe = phanLoai;
        this.ketLuan = ketLuan;
        this.nguoiKetLuan = nguoiKetLuan;
        this.thoiDiemKetLuan = Instant.now();
        this.trangThai = TrangThaiPhieu.DA_KET_LUAN;
    }
}
