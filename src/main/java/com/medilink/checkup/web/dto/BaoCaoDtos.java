package com.medilink.checkup.web.dto;

import com.medilink.checkup.domain.PhanLoaiSucKhoe;

import java.util.List;
import java.util.Map;

public final class BaoCaoDtos {

    private BaoCaoDtos() {}

    public record TienDoLop(String lop, long tongSo, long chuaKham, long dangKham, long duHangMuc, long daKetLuan) {}

    public record TaiBan(String hangMuc, String tenHangMuc, long daKham, long conLai) {}

    public record HocSinhConThieu(String soPhieu, String hoTen, String lop, List<String> hangMucConThieu) {}

    public record BaoCaoDotKham(
            String maDotKham,
            String tenTruong,
            long tongHocSinh,
            long daKetLuan,
            long duHangMucChoKetLuan,
            long dangKham,
            long chuaKham,
            int phanTramHoanThanh,
            List<TienDoLop> theoLop,
            List<TaiBan> theoBan,
            Map<PhanLoaiSucKhoe, Long> phanLoaiSucKhoe,
            Map<String, Long> canTheoDoiTheoHangMuc,
            List<HocSinhConThieu> hocSinhConThieu
    ) {}
}
