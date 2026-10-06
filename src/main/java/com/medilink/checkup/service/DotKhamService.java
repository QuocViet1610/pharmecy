package com.medilink.checkup.service;

import com.medilink.checkup.domain.*;
import com.medilink.checkup.repo.*;
import com.medilink.checkup.web.ApiException;
import com.medilink.checkup.web.dto.DotKhamDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Vòng đời đợt khám: tạo → chuẩn bị → kiểm kê → xuất phát → bắt đầu khám → hoàn thành. */
@Service
public class DotKhamService {

    private final DotKhamRepository dotKhamRepo;
    private final TruongRepository truongRepo;
    private final HocSinhRepository hocSinhRepo;
    private final PhieuKhamRepository phieuRepo;
    private final BanKhamRepository banRepo;
    private final TaiNguyenDotKhamRepository taiNguyenRepo;
    private final KetQuaKhamRepository ketQuaRepo;

    public DotKhamService(DotKhamRepository dotKhamRepo, TruongRepository truongRepo,
                          HocSinhRepository hocSinhRepo, PhieuKhamRepository phieuRepo,
                          BanKhamRepository banRepo, TaiNguyenDotKhamRepository taiNguyenRepo,
                          KetQuaKhamRepository ketQuaRepo) {
        this.dotKhamRepo = dotKhamRepo;
        this.truongRepo = truongRepo;
        this.hocSinhRepo = hocSinhRepo;
        this.phieuRepo = phieuRepo;
        this.banRepo = banRepo;
        this.taiNguyenRepo = taiNguyenRepo;
        this.ketQuaRepo = ketQuaRepo;
    }

    @Transactional
    public DotKhamView tao(TaoDotKhamRequest req) {
        Truong truong = truongRepo.findById(req.truongId())
                .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_TRUONG",
                        "Không tìm thấy trường id=" + req.truongId()));
        DotKham dotKham = dotKhamRepo.save(new DotKham(sinhMa(req.ngayKham()), truong, req.ngayKham(),
                req.hangMucBatBuoc(), req.lop(), req.ghiChu()));
        return view(dotKham);
    }

    /**
     * Giai đoạn 2 — cơ sở y tế nhận danh sách và chuẩn bị: sinh phiếu khám cho từng học sinh,
     * dựng bàn khám theo gói khám và tính checklist vật tư từ số học sinh thực tế.
     */
    @Transactional
    public ChuanBiResult chuanBi(String ma) {
        DotKham dotKham = timTheoMa(ma);
        if (dotKham.getTrangThai() != TrangThaiDotKham.NHAP
                && dotKham.getTrangThai() != TrangThaiDotKham.DANG_CHUAN_BI) {
            throw ApiException.conflict("TRANG_THAI_KHONG_HOP_LE",
                    "Đợt khám đang ở trạng thái " + dotKham.getTrangThai() + ", không chuẩn bị lại được");
        }

        List<HocSinh> hocSinh = dsHocSinh(dotKham);
        if (hocSinh.isEmpty()) {
            throw ApiException.conflict("CHUA_CO_HOC_SINH",
                    "Chưa có học sinh nào cho đợt khám — nhập danh sách của nhà trường trước");
        }

        int soPhieuMoi = sinhPhieu(dotKham, hocSinh);
        int soBan = sinhBanKham(dotKham);
        int soDongChecklist = sinhChecklist(dotKham, hocSinh.size());

        dotKham.chuyenTrangThai(TrangThaiDotKham.DANG_CHUAN_BI);

        Map<String, Integer> theoLop = hocSinh.stream().collect(Collectors.groupingBy(
                HocSinh::getLop, TreeMap::new, Collectors.summingInt(h -> 1)));
        return new ChuanBiResult(dotKham.getMa(), soPhieuMoi, soBan, soDongChecklist, theoLop);
    }

    private List<HocSinh> dsHocSinh(DotKham dotKham) {
        Long truongId = dotKham.getTruong().getId();
        if (dotKham.getLopThamGia().isEmpty()) {
            return hocSinhRepo.findByTruongIdOrderByLopAscHoTenAsc(truongId);
        }
        return hocSinhRepo.findByTruongIdAndLopInOrderByLopAscHoTenAsc(
                truongId, List.copyOf(dotKham.getLopThamGia()));
    }

    /** Idempotent: chạy lại sau khi nhà trường bổ sung học sinh chỉ sinh phiếu cho người còn thiếu. */
    private int sinhPhieu(DotKham dotKham, List<HocSinh> hocSinh) {
        long daCo = phieuRepo.countByDotKhamId(dotKham.getId());
        int soThuTu = (int) daCo;
        int moi = 0;
        for (HocSinh hs : hocSinh) {
            if (phieuRepo.existsByDotKhamIdAndHocSinhId(dotKham.getId(), hs.getId())) continue;
            soThuTu++;
            phieuRepo.save(new PhieuKham(dotKham, hs, "%s-%04d".formatted(dotKham.getMa(), soThuTu)));
            moi++;
        }
        return moi;
    }

    private int sinhBanKham(DotKham dotKham) {
        List<BanKham> daCo = banRepo.findByDotKhamIdOrderByHangMucAscTenAsc(dotKham.getId());
        Set<HangMuc> daDung = daCo.stream().map(BanKham::getHangMuc).collect(Collectors.toSet());
        for (HangMuc hm : dotKham.getHangMucBatBuoc()) {
            if (daDung.contains(hm)) continue;
            banRepo.save(new BanKham(dotKham, hm, "Bàn " + hm.getTenHienThi(), null));
        }
        return banRepo.findByDotKhamIdOrderByHangMucAscTenAsc(dotKham.getId()).size();
    }

    private int sinhChecklist(DotKham dotKham, int soHocSinh) {
        List<TaiNguyenDotKham> daCo = taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId());
        Map<String, TaiNguyenDotKham> theoTen = daCo.stream()
                .collect(Collectors.toMap(TaiNguyenDotKham::getTen, t -> t, (a, b) -> a));

        for (MauChecklist.Mau mau : MauChecklist.MAC_DINH) {
            int can = mau.soLuongCanCho(soHocSinh);
            TaiNguyenDotKham hienCo = theoTen.get(mau.ten());
            if (hienCo != null) {
                // Danh sách học sinh thay đổi → định mức phải thay đổi theo.
                hienCo.capNhatDinhMuc(can, mau.duPhong());
            } else {
                taiNguyenRepo.save(new TaiNguyenDotKham(dotKham, mau.loai(), mau.ten(), mau.donVi(),
                        mau.dinhMucTren100(), can, mau.duPhong()));
            }
        }
        return taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId()).size();
    }

    @Transactional
    public List<TaiNguyenView> capNhatTaiNguyen(String ma, List<CapNhatTaiNguyenRequest> reqs) {
        DotKham dotKham = timTheoMa(ma);
        Map<Long, TaiNguyenDotKham> theoId = taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId())
                .stream().collect(Collectors.toMap(TaiNguyenDotKham::getId, t -> t));
        for (CapNhatTaiNguyenRequest req : reqs) {
            TaiNguyenDotKham t = theoId.get(req.id());
            if (t == null) {
                throw ApiException.notFound("KHONG_TIM_THAY_TAI_NGUYEN",
                        "Dòng checklist id=" + req.id() + " không thuộc đợt khám " + ma);
            }
            if (req.soLuongDaChuanBi() != null) t.capNhatChuanBi(req.soLuongDaChuanBi());
            if (req.soLuongKiemKe() != null) t.kiemKe(req.soLuongKiemKe());
        }
        return checklist(ma);
    }

    @Transactional(readOnly = true)
    public List<TaiNguyenView> checklist(String ma) {
        DotKham dotKham = timTheoMa(ma);
        return taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId()).stream()
                .map(DotKhamService::taiNguyenView)
                .toList();
    }

    /**
     * Giai đoạn 3 — kiểm kê trước khi xuất phát. Điểm đau #3: thiếu đồ chỉ phát hiện khi đã đến trường.
     * Hàm này trả về đúng danh sách còn thiếu để bổ sung ngay tại cơ sở y tế.
     */
    @Transactional(readOnly = true)
    public KetQuaKiemKe kiemKe(String ma) {
        DotKham dotKham = timTheoMa(ma);
        List<TaiNguyenDotKham> tatCa = taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId());
        List<TaiNguyenView> thieu = tatCa.stream().filter(t -> !t.du()).map(DotKhamService::taiNguyenView).toList();

        List<String> canhBao = new ArrayList<>();
        long chuaDem = tatCa.stream().filter(t -> t.getSoLuongKiemKe() == null).count();
        if (chuaDem > 0) {
            canhBao.add(chuaDem + " dòng chưa kiểm kê thực tế — đang lấy theo số khai báo đã chuẩn bị");
        }
        long soPhieu = phieuRepo.countByDotKhamId(dotKham.getId());
        tatCa.stream()
                .filter(t -> "Phiếu khám in sẵn".equals(t.getTen()) && t.soThucTe() < soPhieu)
                .findFirst()
                .ifPresent(t -> canhBao.add("Số phiếu in (" + t.soThucTe() + ") ít hơn số học sinh ("
                        + soPhieu + ") — sẽ có học sinh không có phiếu"));

        return new KetQuaKiemKe(thieu.isEmpty(), thieu.size(), thieu, canhBao);
    }

    /** Chốt: chỉ cho xuất phát khi kiểm kê không còn dòng thiếu. */
    @Transactional
    public DotKhamView xuatPhat(String ma) {
        DotKham dotKham = timTheoMa(ma);
        if (dotKham.getTrangThai() != TrangThaiDotKham.DANG_CHUAN_BI) {
            throw ApiException.conflict("TRANG_THAI_KHONG_HOP_LE",
                    "Chỉ xuất phát được từ trạng thái DANG_CHUAN_BI, hiện tại là " + dotKham.getTrangThai());
        }
        KetQuaKiemKe kq = kiemKe(ma);
        if (!kq.choPhepXuatPhat()) {
            throw ApiException.conflict("THIEU_VAT_TU",
                    "Còn " + kq.soDongThieu() + " dòng chưa đủ, bổ sung trước khi xuất phát",
                    Map.of("dongThieu", kq.dongThieu()));
        }
        dotKham.chuyenTrangThai(TrangThaiDotKham.SAN_SANG);
        return view(dotKham);
    }

    @Transactional
    public DotKhamView batDauKham(String ma) {
        DotKham dotKham = timTheoMa(ma);
        if (dotKham.getTrangThai() != TrangThaiDotKham.SAN_SANG) {
            throw ApiException.conflict("TRANG_THAI_KHONG_HOP_LE",
                    "Phải xuất phát (SAN_SANG) trước khi bắt đầu khám, hiện tại là " + dotKham.getTrangThai());
        }
        dotKham.chuyenTrangThai(TrangThaiDotKham.DANG_KHAM);
        return view(dotKham);
    }

    /** Điểm đau #7 — chỉ đóng đợt khi mọi phiếu đã có kết luận, tránh hồ sơ treo sau khi rời trường. */
    @Transactional
    public DotKhamView hoanThanh(String ma) {
        DotKham dotKham = timTheoMa(ma);
        long tong = phieuRepo.countByDotKhamId(dotKham.getId());
        long daKetLuan = phieuRepo.countByDotKhamIdAndTrangThai(dotKham.getId(), TrangThaiPhieu.DA_KET_LUAN);
        if (daKetLuan < tong) {
            throw ApiException.conflict("CON_PHIEU_CHUA_KET_LUAN",
                    "Còn " + (tong - daKetLuan) + "/" + tong + " phiếu chưa kết luận",
                    Map.of("soPhieuChuaKetLuan", tong - daKetLuan));
        }
        dotKham.chuyenTrangThai(TrangThaiDotKham.HOAN_THANH);
        return view(dotKham);
    }

    @Transactional(readOnly = true)
    public List<DotKhamView> danhSach() {
        return dotKhamRepo.findAllByOrderByNgayKhamDesc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public DotKhamView chiTiet(String ma) {
        return view(timTheoMa(ma));
    }

    @Transactional(readOnly = true)
    public List<BanKhamView> banKham(String ma) {
        DotKham dotKham = timTheoMa(ma);
        Map<HangMuc, Long> luot = ketQuaRepo.demTheoHangMuc(dotKham.getId()).stream()
                .collect(Collectors.toMap(r -> (HangMuc) r[0], r -> (Long) r[1]));
        return banRepo.findByDotKhamIdOrderByHangMucAscTenAsc(dotKham.getId()).stream()
                .map(b -> new BanKhamView(b.getId(), b.getHangMuc().name(), b.getHangMuc().getTenHienThi(),
                        b.getTen(), b.getNhanSu(), luot.getOrDefault(b.getHangMuc(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DotKham timTheoMa(String ma) {
        return dotKhamRepo.findByMa(ma)
                .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_DOT_KHAM", "Không tìm thấy đợt khám " + ma));
    }

    DotKhamView view(DotKham d) {
        return new DotKhamView(d.getMa(), d.getTruong().getTen(), d.getNgayKham(), d.getTrangThai(),
                d.getHangMucBatBuoc().stream().map(HangMucView::cua).toList(),
                phieuRepo.countByDotKhamId(d.getId()),
                phieuRepo.countByDotKhamIdAndTrangThai(d.getId(), TrangThaiPhieu.DA_KET_LUAN),
                d.getGhiChu());
    }

    static TaiNguyenView taiNguyenView(TaiNguyenDotKham t) {
        return new TaiNguyenView(t.getId(), t.getLoai(), t.getTen(), t.getDonVi(),
                t.getSoLuongCan(), t.getSoLuongDuPhong(), t.tongPhaiCo(),
                t.getSoLuongDaChuanBi(), t.getSoLuongKiemKe(), t.soConThieu(), t.du());
    }

    private String sinhMa(LocalDate ngay) {
        String goc = "DK" + ngay.toString().replace("-", "");
        String ma = goc;
        int i = 1;
        while (dotKhamRepo.existsByMa(ma)) {
            ma = goc + "-" + (++i);
        }
        return ma;
    }
}
