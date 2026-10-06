package com.medilink.checkup.service;

import com.medilink.checkup.domain.*;
import com.medilink.checkup.repo.KetQuaKhamRepository;
import com.medilink.checkup.repo.PhieuKhamRepository;
import com.medilink.checkup.web.dto.BaoCaoDtos.*;
import com.medilink.checkup.web.dto.KhamDtos.TienDoPhieu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Điểm đau #7 — tổng hợp hàng trăm/hàng nghìn phiếu. Dữ liệu đã nằm trong đợt khám nên báo cáo
 * là truy vấn, không phải gõ lại từ phiếu giấy.
 */
@Service
public class BaoCaoService {

    private final DotKhamService dotKhamService;
    private final KhamService khamService;
    private final PhieuKhamRepository phieuRepo;
    private final KetQuaKhamRepository ketQuaRepo;

    public BaoCaoService(DotKhamService dotKhamService, KhamService khamService,
                         PhieuKhamRepository phieuRepo, KetQuaKhamRepository ketQuaRepo) {
        this.dotKhamService = dotKhamService;
        this.khamService = khamService;
        this.phieuRepo = phieuRepo;
        this.ketQuaRepo = ketQuaRepo;
    }

    @Transactional(readOnly = true)
    public BaoCaoDotKham baoCao(String maDotKham) {
        DotKham dotKham = dotKhamService.timTheoMa(maDotKham);
        List<PhieuKham> phieu = phieuRepo.findByDotKham(dotKham.getId());
        List<TienDoPhieu> tienDo = phieu.stream().map(khamService::tienDo).toList();

        long tong = tienDo.size();
        long daKetLuan = dem(tienDo, TrangThaiPhieu.DA_KET_LUAN);
        long duHangMuc = dem(tienDo, TrangThaiPhieu.DU_HANG_MUC);
        long dangKham = dem(tienDo, TrangThaiPhieu.DANG_KHAM);
        long chuaKham = dem(tienDo, TrangThaiPhieu.CHUA_KHAM);

        List<TienDoLop> theoLop = tienDo.stream()
                .collect(Collectors.groupingBy(TienDoPhieu::lop, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new TienDoLop(e.getKey(), e.getValue().size(),
                        dem(e.getValue(), TrangThaiPhieu.CHUA_KHAM),
                        dem(e.getValue(), TrangThaiPhieu.DANG_KHAM),
                        dem(e.getValue(), TrangThaiPhieu.DU_HANG_MUC),
                        dem(e.getValue(), TrangThaiPhieu.DA_KET_LUAN)))
                .toList();

        Map<HangMuc, Long> luot = ketQuaRepo.demTheoHangMuc(dotKham.getId()).stream()
                .collect(Collectors.toMap(r -> (HangMuc) r[0], r -> (Long) r[1]));
        List<TaiBan> theoBan = dotKham.getHangMucBatBuoc().stream().sorted()
                .map(hm -> {
                    long daKham = luot.getOrDefault(hm, 0L);
                    return new TaiBan(hm.name(), hm.getTenHienThi(), daKham, tong - daKham);
                })
                .toList();

        Map<PhanLoaiSucKhoe, Long> phanLoai = new EnumMap<>(PhanLoaiSucKhoe.class);
        phieu.stream()
                .filter(p -> p.getPhanLoaiSucKhoe() != null)
                .forEach(p -> phanLoai.merge(p.getPhanLoaiSucKhoe(), 1L, Long::sum));

        Map<String, Long> canTheoDoi = new LinkedHashMap<>();
        ketQuaRepo.findByDotKham(dotKham.getId()).stream()
                .filter(k -> k.getKetLuanChuyenMon() != KetLuanChuyenMon.BINH_THUONG)
                .forEach(k -> canTheoDoi.merge(k.getHangMuc().getTenHienThi(), 1L, Long::sum));

        List<HocSinhConThieu> conThieu = tienDo.stream()
                .filter(t -> !t.duHangMuc())
                .map(t -> new HocSinhConThieu(t.soPhieu(), t.hoTen(), t.lop(),
                        t.conThieu().stream().map(h -> h.ten()).toList()))
                .toList();

        int phanTram = tong == 0 ? 0 : (int) Math.round(daKetLuan * 100.0 / tong);
        return new BaoCaoDotKham(dotKham.getMa(), dotKham.getTruong().getTen(), tong, daKetLuan, duHangMuc,
                dangKham, chuaKham, phanTram, theoLop, theoBan, phanLoai, canTheoDoi, conThieu);
    }

    /** Xuất CSV để cơ sở y tế lưu trữ và gửi lại nhà trường. */
    @Transactional(readOnly = true)
    public String xuatCsv(String maDotKham) {
        DotKham dotKham = dotKhamService.timTheoMa(maDotKham);
        List<HangMuc> hangMuc = dotKham.getHangMucBatBuoc().stream().sorted().toList();

        StringBuilder sb = new StringBuilder();
        sb.append("so_phieu,ma_dinh_danh,ho_ten,lop,khoi,gioi_tinh,trang_thai");
        hangMuc.forEach(hm -> sb.append(',').append(hm.name().toLowerCase()));
        sb.append(",phan_loai_suc_khoe,ket_luan,nguoi_ket_luan,hang_muc_con_thieu\n");

        for (PhieuKham p : phieuRepo.findByDotKham(dotKham.getId())) {
            TienDoPhieu t = khamService.tienDo(p);
            Map<String, String> theoHangMuc = t.daKham().stream()
                    .collect(Collectors.toMap(k -> k.hangMuc(), k -> k.ketLuanChuyenMon().name(), (a, b) -> a));
            sb.append(o(t.soPhieu())).append(',').append(o(t.maDinhDanh())).append(',').append(o(t.hoTen()))
                    .append(',').append(o(t.lop())).append(',').append(o(t.khoi()))
                    .append(',').append(t.gioiTinh()).append(',').append(t.trangThai());
            hangMuc.forEach(hm -> sb.append(',').append(theoHangMuc.getOrDefault(hm.name(), "")));
            sb.append(',').append(t.phanLoaiSucKhoe() == null ? "" : t.phanLoaiSucKhoe())
                    .append(',').append(o(t.ketLuan()))
                    .append(',').append(o(t.nguoiKetLuan()))
                    .append(',').append(o(t.conThieu().stream().map(h -> h.ten()).collect(Collectors.joining(" | "))))
                    .append('\n');
        }
        return sb.toString();
    }

    private static long dem(List<TienDoPhieu> ds, TrangThaiPhieu tt) {
        return ds.stream().filter(t -> t.trangThai() == tt).count();
    }

    private static String o(String v) {
        if (v == null || v.isEmpty()) return "";
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
