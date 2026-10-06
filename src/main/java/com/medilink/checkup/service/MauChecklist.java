package com.medilink.checkup.service;

import com.medilink.checkup.domain.LoaiTaiNguyen;

import java.util.List;

/**
 * Định mức mặc định của một đợt khám. Điểm đau #2 — "không kiểm soát được số lượng vật tư":
 * thay vì nhẩm tay, số lượng cần được tính từ số học sinh thực tế và số bàn khám.
 */
final class MauChecklist {

    private MauChecklist() {}

    /**
     * @param dinhMucTren100 lượng cần cho 100 học sinh; 0 = dùng {@code soLuongCoDinh}
     * @param soLuongCoDinh  số lượng không phụ thuộc số học sinh (nhân sự, thiết bị)
     */
    record Mau(LoaiTaiNguyen loai, String ten, String donVi,
               double dinhMucTren100, int soLuongCoDinh, int duPhong) {

        int soLuongCanCho(int soHocSinh) {
            if (dinhMucTren100 > 0) {
                return (int) Math.ceil(dinhMucTren100 * soHocSinh / 100.0);
            }
            return soLuongCoDinh;
        }
    }

    static final List<Mau> MAC_DINH = List.of(
            new Mau(LoaiTaiNguyen.VAT_TU, "Phiếu khám in sẵn", "tờ", 100, 0, 10),
            new Mau(LoaiTaiNguyen.VAT_TU, "Que đè lưỡi", "cái", 100, 0, 20),
            new Mau(LoaiTaiNguyen.VAT_TU, "Găng tay y tế", "đôi", 30, 0, 10),
            new Mau(LoaiTaiNguyen.VAT_TU, "Khẩu trang", "cái", 20, 0, 10),
            new Mau(LoaiTaiNguyen.VAT_TU, "Bông, gạc", "gói", 3, 0, 2),
            new Mau(LoaiTaiNguyen.VAT_TU, "Cồn sát khuẩn", "lọ", 2, 0, 1),

            new Mau(LoaiTaiNguyen.THIET_BI, "Cân sức khỏe", "cái", 0, 1, 1),
            new Mau(LoaiTaiNguyen.THIET_BI, "Thước đo chiều cao", "cái", 0, 1, 0),
            new Mau(LoaiTaiNguyen.THIET_BI, "Bảng thị lực", "bộ", 0, 1, 0),
            new Mau(LoaiTaiNguyen.THIET_BI, "Đèn soi Tai - Mũi - Họng", "cái", 0, 1, 1),
            new Mau(LoaiTaiNguyen.THIET_BI, "Bộ dụng cụ Răng - Hàm - Mặt", "bộ", 0, 1, 1),
            new Mau(LoaiTaiNguyen.THIET_BI, "Máy đo huyết áp", "cái", 0, 1, 1),
            new Mau(LoaiTaiNguyen.THIET_BI, "Ống nghe", "cái", 0, 2, 1),
            new Mau(LoaiTaiNguyen.THIET_BI, "Bàn, ghế khám", "bộ", 0, 6, 1),

            new Mau(LoaiTaiNguyen.NHAN_SU, "Bác sĩ khám chuyên môn", "người", 0, 5, 0),
            new Mau(LoaiTaiNguyen.NHAN_SU, "Điều dưỡng ghi chép", "người", 0, 2, 0),
            new Mau(LoaiTaiNguyen.NHAN_SU, "Người phụ trách bàn kết luận", "người", 0, 1, 0));
}
