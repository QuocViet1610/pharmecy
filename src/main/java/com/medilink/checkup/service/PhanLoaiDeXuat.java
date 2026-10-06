package com.medilink.checkup.service;

import com.medilink.checkup.domain.KetLuanChuyenMon;
import com.medilink.checkup.domain.KetQuaKham;
import com.medilink.checkup.domain.PhanLoaiSucKhoe;

import java.util.List;

/**
 * Gợi ý phân loại sức khỏe từ kết luận của các bàn — người phụ trách bàn kết luận vẫn là người quyết định,
 * phần mềm chỉ tổng hợp để không phải đọc lại từng ô trên phiếu.
 */
final class PhanLoaiDeXuat {

    private PhanLoaiDeXuat() {}

    static PhanLoaiSucKhoe tu(List<KetQuaKham> ketQua) {
        if (ketQua.isEmpty()) return null;
        long batThuong = ketQua.stream().filter(k -> k.getKetLuanChuyenMon() == KetLuanChuyenMon.BAT_THUONG).count();
        long canTheoDoi = ketQua.stream().filter(k -> k.getKetLuanChuyenMon() == KetLuanChuyenMon.CAN_THEO_DOI).count();
        if (batThuong >= 2) return PhanLoaiSucKhoe.IV;
        if (batThuong == 1) return PhanLoaiSucKhoe.III;
        if (canTheoDoi >= 2) return PhanLoaiSucKhoe.III;
        if (canTheoDoi == 1) return PhanLoaiSucKhoe.II;
        return PhanLoaiSucKhoe.I;
    }
}
