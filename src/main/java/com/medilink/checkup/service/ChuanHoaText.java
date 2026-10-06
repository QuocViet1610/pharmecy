package com.medilink.checkup.service;

import java.text.Normalizer;

/**
 * Bỏ dấu tiếng Việt để tìm kiếm. Ở bàn khám, gõ "duc long" nhanh hơn "Đức Long" rất nhiều —
 * nhất là trên tablet — nên tên học sinh được lưu kèm một bản không dấu để so khớp.
 */
public final class ChuanHoaText {

    private ChuanHoaText() {}

    public static String boDau(String v) {
        if (v == null) return null;
        String s = Normalizer.normalize(v.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
        return s.replaceAll("\\s+", " ");
    }
}
