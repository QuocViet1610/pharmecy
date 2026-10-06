package com.medilink.checkup.domain;

import java.util.List;

/**
 * Hạng mục khám = một bàn/trạm khám trong khu khám.
 * {@code goiY} là các chỉ số bàn đó thường ghi — UI dùng để dựng form, không ràng buộc cứng.
 */
public enum HangMuc {
    THE_LUC("Thể lực", List.of("chieuCao", "canNang")),
    MAT("Mắt", List.of("thiLucPhai", "thiLucTrai")),
    TMH("Tai - Mũi - Họng", List.of("tai", "mui", "hong")),
    RHM("Răng - Hàm - Mặt", List.of("rang", "ham")),
    NOI_NHI("Nội / Nhi", List.of("huyetAp", "mach", "tim", "phoi"));

    private final String tenHienThi;
    private final List<String> goiY;

    HangMuc(String tenHienThi, List<String> goiY) {
        this.tenHienThi = tenHienThi;
        this.goiY = goiY;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public List<String> getGoiY() {
        return goiY;
    }
}
