package com.medilink.checkup.service;

import com.medilink.checkup.domain.*;
import com.medilink.checkup.repo.*;
import com.medilink.checkup.web.ApiException;
import com.medilink.checkup.web.dto.DotKhamDtos.HangMucView;
import com.medilink.checkup.web.dto.KhamDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Nghiệp vụ tại khu khám. Nguyên tắc của tài liệu được giữ nguyên:
 * <b>thứ tự khám không quan trọng</b> — học sinh vào bàn nào trước cũng được,
 * phần mềm chỉ chịu trách nhiệm biết ai đã qua bàn nào và còn thiếu bàn nào.
 */
@Service
public class KhamService {

    private final DotKhamService dotKhamService;
    private final PhieuKhamRepository phieuRepo;
    private final BanKhamRepository banRepo;
    private final KetQuaKhamRepository ketQuaRepo;

    public KhamService(DotKhamService dotKhamService, PhieuKhamRepository phieuRepo,
                       BanKhamRepository banRepo, KetQuaKhamRepository ketQuaRepo) {
        this.dotKhamService = dotKhamService;
        this.phieuRepo = phieuRepo;
        this.banRepo = banRepo;
        this.ketQuaRepo = ketQuaRepo;
    }

    /** Điểm đau #4 — nhận diện học sinh tại bàn bằng mã định danh hoặc số phiếu/QR, không dựa vào tờ giấy. */
    @Transactional(readOnly = true)
    public TienDoPhieu traCuu(String maDotKham, String maNhanDien) {
        return tienDo(timPhieu(maDotKham, maNhanDien));
    }

    /**
     * Điểm đau #5 — một bàn ghi kết quả vào phiếu. Ghi lần hai cho cùng hạng mục là sửa kết quả,
     * không sinh thêm dòng, nên bác sĩ ghi nhầm vẫn sửa được ngay tại bàn.
     */
    @Transactional
    public TienDoPhieu ghiKetQua(String maDotKham, GhiKetQuaRequest req) {
        DotKham dotKham = dotKhamService.timTheoMa(maDotKham);
        if (dotKham.getTrangThai() != TrangThaiDotKham.DANG_KHAM) {
            throw ApiException.conflict("DOT_KHAM_CHUA_MO",
                    "Đợt khám đang ở trạng thái " + dotKham.getTrangThai() + ", chưa ghi kết quả được");
        }
        if (!dotKham.getHangMucBatBuoc().contains(req.hangMuc())) {
            throw ApiException.badRequest("HANG_MUC_NGOAI_GOI_KHAM",
                    "Hạng mục " + req.hangMuc() + " không thuộc gói khám của đợt " + maDotKham);
        }

        PhieuKham phieu = timPhieu(maDotKham, req.maNhanDien());
        if (phieu.getTrangThai() == TrangThaiPhieu.DA_KET_LUAN) {
            throw ApiException.conflict("PHIEU_DA_KET_LUAN",
                    "Phiếu " + phieu.getSoPhieu() + " đã kết luận, không ghi thêm kết quả");
        }

        BanKham ban = chonBan(dotKham, req);

        ketQuaRepo.findByPhieuKhamIdAndHangMuc(phieu.getId(), req.hangMuc())
                .ifPresentOrElse(
                        cu -> cu.ghiLai(ban, req.bacSi(), req.ketLuanChuyenMon(), req.chiTiet(), req.ghiChu()),
                        () -> ketQuaRepo.save(new KetQuaKham(phieu, req.hangMuc(), ban, req.bacSi(),
                                req.ketLuanChuyenMon(), req.chiTiet(), req.ghiChu())));

        TienDoPhieu tienDo = tienDo(phieu);
        phieu.capNhatTrangThai(tienDo.duHangMuc() ? TrangThaiPhieu.DU_HANG_MUC : TrangThaiPhieu.DANG_KHAM);
        return tienDo(phieu);
    }

    private BanKham chonBan(DotKham dotKham, GhiKetQuaRequest req) {
        if (req.banKhamId() != null) {
            BanKham ban = banRepo.findById(req.banKhamId())
                    .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_BAN",
                            "Không tìm thấy bàn khám id=" + req.banKhamId()));
            if (!ban.getDotKham().getId().equals(dotKham.getId())) {
                throw ApiException.badRequest("BAN_KHAC_DOT_KHAM",
                        "Bàn khám id=" + req.banKhamId() + " không thuộc đợt khám " + dotKham.getMa());
            }
            if (ban.getHangMuc() != req.hangMuc()) {
                throw ApiException.badRequest("BAN_SAI_HANG_MUC",
                        "Bàn '" + ban.getTen() + "' phụ trách " + ban.getHangMuc()
                                + ", không ghi được kết quả " + req.hangMuc());
            }
            return ban;
        }
        return banRepo.findByDotKhamIdAndHangMuc(dotKham.getId(), req.hangMuc()).stream().findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public TienDoPhieu tienDoTheoSoPhieu(String soPhieu) {
        PhieuKham phieu = phieuRepo.findBySoPhieu(soPhieu)
                .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_PHIEU", "Không tìm thấy phiếu " + soPhieu));
        return tienDo(phieu);
    }

    @Transactional(readOnly = true)
    public List<TienDoPhieu> tienDoDotKham(String maDotKham, String lop, Boolean conThieu) {
        DotKham dotKham = dotKhamService.timTheoMa(maDotKham);
        return phieuRepo.findByDotKham(dotKham.getId()).stream()
                .filter(p -> lop == null || lop.isBlank() || lop.equalsIgnoreCase(p.getHocSinh().getLop()))
                .map(this::tienDo)
                .filter(t -> conThieu == null || conThieu != t.duHangMuc())
                .toList();
    }

    PhieuKham timPhieu(String maDotKham, String maNhanDien) {
        DotKham dotKham = dotKhamService.timTheoMa(maDotKham);
        return phieuRepo.traCuu(dotKham.getId(), maNhanDien.trim())
                .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_PHIEU",
                        "Không tìm thấy học sinh/phiếu '" + maNhanDien + "' trong đợt khám " + maDotKham));
    }

    /** Tiến độ = đã khám những bàn nào, còn thiếu bàn nào trong gói khám bắt buộc. */
    TienDoPhieu tienDo(PhieuKham phieu) {
        List<KetQuaKham> ketQua = ketQuaRepo.findByPhieuKhamIdOrderByThoiDiemAsc(phieu.getId());
        Set<HangMuc> daKham = EnumSet.noneOf(HangMuc.class);
        ketQua.forEach(k -> daKham.add(k.getHangMuc()));

        List<HangMucView> conThieu = phieu.getDotKham().getHangMucBatBuoc().stream()
                .filter(hm -> !daKham.contains(hm))
                .sorted()
                .map(HangMucView::cua)
                .toList();

        HocSinh hs = phieu.getHocSinh();
        return new TienDoPhieu(
                phieu.getSoPhieu(), hs.getMaDinhDanh(), hs.getHoTen(), hs.getLop(), hs.getKhoi(), hs.getGioiTinh(),
                phieu.getTrangThai(),
                ketQua.stream().map(KhamService::ketQuaView).toList(),
                conThieu,
                conThieu.isEmpty(),
                phieu.getPhanLoaiSucKhoe(),
                PhanLoaiDeXuat.tu(ketQua),
                phieu.getKetLuan(),
                phieu.getNguoiKetLuan());
    }

    static KetQuaView ketQuaView(KetQuaKham k) {
        return new KetQuaView(k.getHangMuc().name(), k.getHangMuc().getTenHienThi(), k.getKetLuanChuyenMon(),
                k.getBanKham() == null ? null : k.getBanKham().getTen(), k.getBacSi(),
                new LinkedHashMap<>(k.getChiTiet()), k.getGhiChu(), k.getThoiDiem());
    }
}
