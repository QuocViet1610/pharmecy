package com.medilink.checkup.domain;

import jakarta.persistence.*;

/**
 * Một dòng checklist nhân sự / vật tư / thiết bị của đợt khám.
 * {@code dinhMucTren100HocSinh} cho phép tính {@code soLuongCan} từ số học sinh thực tế,
 * thay vì để người chuẩn bị nhẩm tay.
 */
@Entity
@Table(name = "tai_nguyen_dot_kham")
public class TaiNguyenDotKham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dot_kham_id")
    private DotKham dotKham;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoaiTaiNguyen loai;

    @Column(nullable = false)
    private String ten;

    private String donVi;

    /** 0 = không theo đầu học sinh (ví dụ: cân, thước đo — tính theo số bàn). */
    @Column(nullable = false)
    private double dinhMucTren100HocSinh;

    @Column(nullable = false)
    private int soLuongCan;

    @Column(nullable = false)
    private int soLuongDuPhong;

    @Column(nullable = false)
    private int soLuongDaChuanBi;

    /** Đếm lại ở bước kiểm kê sáng ngày khám; null = chưa kiểm kê. */
    private Integer soLuongKiemKe;

    protected TaiNguyenDotKham() {}

    public TaiNguyenDotKham(DotKham dotKham, LoaiTaiNguyen loai, String ten, String donVi,
                            double dinhMucTren100HocSinh, int soLuongCan, int soLuongDuPhong) {
        this.dotKham = dotKham;
        this.loai = loai;
        this.ten = ten;
        this.donVi = donVi;
        this.dinhMucTren100HocSinh = dinhMucTren100HocSinh;
        this.soLuongCan = soLuongCan;
        this.soLuongDuPhong = soLuongDuPhong;
    }

    public Long getId() { return id; }
    public DotKham getDotKham() { return dotKham; }
    public LoaiTaiNguyen getLoai() { return loai; }
    public String getTen() { return ten; }
    public String getDonVi() { return donVi; }
    public double getDinhMucTren100HocSinh() { return dinhMucTren100HocSinh; }
    public int getSoLuongCan() { return soLuongCan; }
    public int getSoLuongDuPhong() { return soLuongDuPhong; }
    public int getSoLuongDaChuanBi() { return soLuongDaChuanBi; }
    public Integer getSoLuongKiemKe() { return soLuongKiemKe; }

    /** Tổng phải có khi xuất phát = số lượng cần + dự phòng. */
    public int tongPhaiCo() { return soLuongCan + soLuongDuPhong; }

    /** Số thực tế dùng để đối chiếu: ưu tiên số đã kiểm kê, chưa kiểm kê thì lấy số khai báo chuẩn bị. */
    public int soThucTe() { return soLuongKiemKe != null ? soLuongKiemKe : soLuongDaChuanBi; }

    public int soConThieu() { return Math.max(0, tongPhaiCo() - soThucTe()); }

    public boolean du() { return soConThieu() == 0; }

    public void capNhatChuanBi(int soLuongDaChuanBi) { this.soLuongDaChuanBi = soLuongDaChuanBi; }

    public void kiemKe(int soDem) { this.soLuongKiemKe = soDem; }

    public void capNhatDinhMuc(int soLuongCan, int soLuongDuPhong) {
        this.soLuongCan = soLuongCan;
        this.soLuongDuPhong = soLuongDuPhong;
    }
}
