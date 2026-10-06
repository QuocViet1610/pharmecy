package com.medilink.checkup.web.dto;

import com.medilink.checkup.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class KhamDtos {

    private KhamDtos() {}

    public record GhiKetQuaRequest(
            /** Mã định danh học sinh hoặc số phiếu (quét QR) — bàn khám dùng cái nào cũng được. */
            @NotBlank String maNhanDien,
            @NotNull HangMuc hangMuc,
            Long banKhamId,
            String bacSi,
            @NotNull KetLuanChuyenMon ketLuanChuyenMon,
            Map<String, String> chiTiet,
            String ghiChu
    ) {}

    public record KetQuaView(
            String hangMuc,
            String tenHangMuc,
            KetLuanChuyenMon ketLuanChuyenMon,
            String banKham,
            String bacSi,
            Map<String, String> chiTiet,
            String ghiChu,
            Instant thoiDiem
    ) {}

    /**
     * Tiến độ của một phiếu — thay cho việc nhìn tờ giấy để đoán học sinh đã qua bàn nào.
     * {@code conThieu} chính là cảnh báo trước khi cho vào bàn kết luận.
     */
    public record TienDoPhieu(
            String soPhieu,
            String maDinhDanh,
            String hoTen,
            String lop,
            String khoi,
            GioiTinh gioiTinh,
            TrangThaiPhieu trangThai,
            List<KetQuaView> daKham,
            List<DotKhamDtos.HangMucView> conThieu,
            boolean duHangMuc,
            PhanLoaiSucKhoe phanLoaiSucKhoe,
            PhanLoaiSucKhoe phanLoaiDeXuat,
            String ketLuan,
            String nguoiKetLuan
    ) {}

    public record KetLuanRequest(
            @NotBlank String maNhanDien,
            /** Bỏ trống = dùng phân loại hệ thống đề xuất từ kết quả các bàn. */
            PhanLoaiSucKhoe phanLoaiSucKhoe,
            String ketLuan,
            @NotBlank String nguoiKetLuan
    ) {}
}
