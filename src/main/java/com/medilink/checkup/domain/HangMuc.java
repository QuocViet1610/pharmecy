package com.medilink.checkup.domain;

import java.util.List;

/** Hạng mục khám = một bàn/trạm khám trong khu khám, kèm các chỉ số bàn đó thường ghi. */
public enum HangMuc {
    THE_LUC("Thể lực", List.of(
            ChiSo.so("chieuCao", "Chiều cao (cm)", "138"),
            ChiSo.so("canNang", "Cân nặng (kg)", "32"))),

    MAT("Mắt", List.of(
            ChiSo.text("thiLucPhai", "Thị lực mắt phải", "10/10"),
            ChiSo.text("thiLucTrai", "Thị lực mắt trái", "10/10"))),

    TMH("Tai - Mũi - Họng", List.of(
            ChiSo.text("tai", "Tai", "bình thường"),
            ChiSo.text("mui", "Mũi", "bình thường"),
            ChiSo.text("hong", "Họng", "bình thường"))),

    RHM("Răng - Hàm - Mặt", List.of(
            ChiSo.text("rang", "Răng", "không sâu"),
            ChiSo.text("ham", "Hàm, khớp", "bình thường"))),

    NOI_NHI("Nội / Nhi", List.of(
            ChiSo.text("huyetAp", "Huyết áp (mmHg)", "100/60"),
            ChiSo.so("mach", "Mạch (lần/phút)", "85"),
            ChiSo.text("tim", "Tim", "bình thường"),
            ChiSo.text("phoi", "Phổi", "bình thường")));

    private final String tenHienThi;
    private final List<ChiSo> chiSo;

    HangMuc(String tenHienThi, List<ChiSo> chiSo) {
        this.tenHienThi = tenHienThi;
        this.chiSo = chiSo;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public List<ChiSo> getChiSo() {
        return chiSo;
    }
}
