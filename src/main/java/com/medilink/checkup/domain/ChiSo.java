package com.medilink.checkup.domain;

/**
 * Một chỉ số bàn khám ghi vào phiếu. UI dựng form từ đây, không hardcode.
 *
 * @param ma   khóa lưu trong {@link KetQuaKham#getChiTiet()}
 * @param nhan nhãn hiển thị kèm đơn vị
 * @param kieu "so" → bật bàn phím số trên tablet; "text" → nhập tự do
 * @param vd   giá trị ví dụ, dùng làm placeholder
 */
public record ChiSo(String ma, String nhan, String kieu, String vd) {

    public static ChiSo so(String ma, String nhan, String vd) {
        return new ChiSo(ma, nhan, "so", vd);
    }

    public static ChiSo text(String ma, String nhan, String vd) {
        return new ChiSo(ma, nhan, "text", vd);
    }
}
