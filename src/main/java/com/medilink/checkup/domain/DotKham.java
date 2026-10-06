package com.medilink.checkup.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Đợt khám — gốc của toàn bộ nghiệp vụ: phiếu, bàn khám, vật tư, tiến độ, báo cáo đều treo vào đây. */
@Entity
@Table(name = "dot_kham")
public class DotKham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truong_id")
    private Truong truong;

    @Column(nullable = false)
    private LocalDate ngayKham;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiDotKham trangThai = TrangThaiDotKham.NHAP;

    /** Gói khám: các hạng mục học sinh buộc phải hoàn thành trước khi tới bàn kết luận. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dot_kham_hang_muc", joinColumns = @JoinColumn(name = "dot_kham_id"))
    @Column(name = "hang_muc")
    @Enumerated(EnumType.STRING)
    private Set<HangMuc> hangMucBatBuoc = EnumSet.noneOf(HangMuc.class);

    /** Lớp tham gia đợt khám; rỗng = toàn trường. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dot_kham_lop", joinColumns = @JoinColumn(name = "dot_kham_id"))
    @Column(name = "lop")
    private Set<String> lopThamGia = new LinkedHashSet<>();

    private String ghiChu;

    protected DotKham() {}

    public DotKham(String ma, Truong truong, LocalDate ngayKham, Set<HangMuc> hangMucBatBuoc,
                   Collection<String> lopThamGia, String ghiChu) {
        this.ma = ma;
        this.truong = truong;
        this.ngayKham = ngayKham;
        this.hangMucBatBuoc = EnumSet.copyOf(hangMucBatBuoc);
        if (lopThamGia != null) this.lopThamGia.addAll(lopThamGia);
        this.ghiChu = ghiChu;
    }

    public Long getId() { return id; }
    public String getMa() { return ma; }
    public Truong getTruong() { return truong; }
    public LocalDate getNgayKham() { return ngayKham; }
    public TrangThaiDotKham getTrangThai() { return trangThai; }
    public Set<HangMuc> getHangMucBatBuoc() { return hangMucBatBuoc; }
    public Set<String> getLopThamGia() { return lopThamGia; }
    public String getGhiChu() { return ghiChu; }

    public void chuyenTrangThai(TrangThaiDotKham moi) { this.trangThai = moi; }
}
