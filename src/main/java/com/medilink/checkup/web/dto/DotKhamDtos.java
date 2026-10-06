package com.medilink.checkup.web.dto;

import com.medilink.checkup.domain.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DotKhamDtos {

    private DotKhamDtos() {}

    public record TaoDotKhamRequest(
            @NotNull Long truongId,
            @NotNull @FutureOrPresent LocalDate ngayKham,
            @NotEmpty Set<HangMuc> hangMucBatBuoc,
            /** Rỗng = lấy toàn trường. */
            List<String> lop,
            String ghiChu
    ) {}

    public record DotKhamView(
            String ma,
            String tenTruong,
            LocalDate ngayKham,
            TrangThaiDotKham trangThai,
            List<HangMucView> hangMucBatBuoc,
            long soPhieu,
            long soDaKetLuan,
            String ghiChu
    ) {}

    public record HangMucView(String ma, String ten, List<String> chiSoGoiY) {
        public static HangMucView cua(HangMuc hm) {
            return new HangMucView(hm.name(), hm.getTenHienThi(), hm.getGoiY());
        }
    }

    public record BanKhamView(Long id, String hangMuc, String tenHangMuc, String ten,
                              String nhanSu, long soLuotDaKham) {}

    public record TaiNguyenView(
            Long id,
            LoaiTaiNguyen loai,
            String ten,
            String donVi,
            int soLuongCan,
            int soLuongDuPhong,
            int tongPhaiCo,
            int soLuongDaChuanBi,
            Integer soLuongKiemKe,
            int soConThieu,
            boolean du
    ) {}

    public record CapNhatTaiNguyenRequest(
            @NotNull Long id,
            @PositiveOrZero Integer soLuongDaChuanBi,
            @PositiveOrZero Integer soLuongKiemKe
    ) {}

    /** Chốt kiểm soát trước khi xe rời cơ sở y tế. */
    public record KetQuaKiemKe(
            boolean choPhepXuatPhat,
            int soDongThieu,
            List<TaiNguyenView> dongThieu,
            List<String> canhBao
    ) {}

    public record ChuanBiResult(
            String maDotKham,
            int soPhieuDaSinh,
            int soBanKham,
            int soDongChecklist,
            Map<String, Integer> soHocSinhTheoLop
    ) {}
}
