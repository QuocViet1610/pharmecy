package com.medilink.checkup;

import com.medilink.checkup.domain.*;
import com.medilink.checkup.repo.*;
import com.medilink.checkup.service.*;
import com.medilink.checkup.web.ApiException;
import com.medilink.checkup.web.dto.DotKhamDtos.*;
import com.medilink.checkup.web.dto.ImportDtos.CheDoImport;
import com.medilink.checkup.web.dto.ImportDtos.KetQuaImport;
import com.medilink.checkup.web.dto.KhamDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kiểm tra các quy tắc nghiệp vụ trong tài liệu "Quy trình khám sức khỏe định kỳ tại trường học". */
@SpringBootTest
@Transactional
class NghiepVuDotKhamTest {

    @Autowired ImportHocSinhService importService;
    @Autowired DotKhamService dotKhamService;
    @Autowired KhamService khamService;
    @Autowired KetLuanService ketLuanService;
    @Autowired BaoCaoService baoCaoService;
    @Autowired TruongRepository truongRepo;
    @Autowired TaiNguyenDotKhamRepository taiNguyenRepo;

    private Long truongId;

    @BeforeEach
    void chuanBiTruong() {
        truongId = truongRepo.save(new Truong("THCS Test", "Hà Nội", "Cô Y tế", "0900000000")).getId();
    }

    private static final String CSV_SACH = """
            ma_dinh_danh,ho_ten,ngay_sinh,gioi_tinh,lop,khoi
            T001,Nguyễn Văn A,12/05/2014,Nam,6A1,6
            T002,Trần Thị B,03/09/2014,Nữ,6A1,6
            T003,Lê Văn C,21/07/2014,Nam,6A1,6
            """;

    /* ---------- Điểm đau #1: danh sách sai/thiếu/trùng ---------- */

    @Test
    void import_bat_loi_truoc_ngay_kham_va_khong_ghi_du_lieu_o_che_do_kiem_tra() {
        String csv = """
                ma_dinh_danh,ho_ten,ngay_sinh,gioi_tinh,lop,khoi,ho_ten_phu_huynh,dien_thoai_phu_huynh
                T001,Nguyễn Văn A,12/05/2014,Nam,6A1,6,PH A,0912000001
                T002,,03/09/2014,Nữ,6A1,6,PH B,0912000002
                T003,Lê Văn C,31/02/2014,Nam,6A1,6,PH C,0912000003
                T001,Nguyễn Văn A,12/05/2014,Nam,6A1,6,PH A,0912000001
                T004,Phạm Thị D,18/11/2014,Khong ro,6A2,6,PH D,0912000004
                T005,Vũ Văn E,09/08/2014,Nam,,6,PH E,091
                """;

        KetQuaImport kq = importService.nhapDanhSach(truongId, csv, CheDoImport.KIEM_TRA);

        assertThat(kq.tongDong()).isEqualTo(6);
        assertThat(kq.soDongLoi()).isEqualTo(5);
        assertThat(kq.soDaLuu()).isZero();
        assertThat(loiCuaDong(kq, 3)).anyMatch(l -> l.contains("Thiếu họ tên"));
        // 31/02 không tồn tại — không được âm thầm thành 28/02
        assertThat(loiCuaDong(kq, 4)).anyMatch(l -> l.contains("không hợp lệ"));
        assertThat(loiCuaDong(kq, 5)).anyMatch(l -> l.contains("trùng với dòng 2"));
        assertThat(loiCuaDong(kq, 6)).anyMatch(l -> l.contains("Giới tính"));
        assertThat(loiCuaDong(kq, 7)).anyMatch(l -> l.contains("Thiếu lớp"));
        // Điện thoại sai dạng chỉ là cảnh báo, không chặn
        assertThat(kq.dongCanhBao()).isEmpty();
    }

    @Test
    void import_che_do_luu_chi_ghi_dong_hop_le_va_bat_trung_voi_du_lieu_da_co() {
        importService.nhapDanhSach(truongId, CSV_SACH, CheDoImport.LUU);

        KetQuaImport lanHai = importService.nhapDanhSach(truongId, """
                ma_dinh_danh,ho_ten,ngay_sinh,gioi_tinh,lop,khoi
                T002,Trần Thị B,03/09/2014,Nữ,6A1,6
                T009,Hoàng Văn F,01/03/2014,Nam,6A1,6
                """, CheDoImport.LUU);

        assertThat(lanHai.soDaLuu()).isEqualTo(1);
        assertThat(loiCuaDong(lanHai, 2)).anyMatch(l -> l.contains("đã có trong hệ thống"));
    }

    /* ---------- Điểm đau #2, #3: vật tư và kiểm kê trước xuất phát ---------- */

    @Test
    void checklist_tinh_so_luong_tu_so_hoc_sinh_thuc_te() {
        DotKhamView dot = taoDotKhamDaChuanBi();

        TaiNguyenView quede = checklist(dot.ma()).stream()
                .filter(t -> t.ten().equals("Que đè lưỡi")).findFirst().orElseThrow();

        // 3 học sinh, định mức 100/100 HS → cần 3, dự phòng 20
        assertThat(quede.soLuongCan()).isEqualTo(3);
        assertThat(quede.tongPhaiCo()).isEqualTo(23);
    }

    @Test
    void khong_cho_xuat_phat_khi_con_thieu_va_cho_xuat_phat_sau_khi_bo_sung() {
        DotKhamView dot = taoDotKhamDaChuanBi();

        assertThat(dotKhamService.kiemKe(dot.ma()).choPhepXuatPhat()).isFalse();
        assertThatThrownBy(() -> dotKhamService.xuatPhat(dot.ma()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("bổ sung trước khi xuất phát");

        boSungDuVatTu(dot.ma());

        assertThat(dotKhamService.kiemKe(dot.ma()).choPhepXuatPhat()).isTrue();
        assertThat(dotKhamService.xuatPhat(dot.ma()).trangThai()).isEqualTo(TrangThaiDotKham.SAN_SANG);
    }

    /* ---------- Nguyên tắc: khám tự do giữa các bàn ---------- */

    @Test
    void thu_tu_kham_khong_quan_trong_mien_la_du_hang_muc() {
        DotKhamView dot = moDotKham();

        List<HangMuc> thuTuA = List.of(HangMuc.MAT, HangMuc.RHM, HangMuc.THE_LUC, HangMuc.TMH, HangMuc.NOI_NHI);
        List<HangMuc> thuTuB = List.of(HangMuc.TMH, HangMuc.NOI_NHI, HangMuc.MAT, HangMuc.THE_LUC, HangMuc.RHM);
        thuTuA.forEach(hm -> ghi(dot.ma(), "T001", hm, KetLuanChuyenMon.BINH_THUONG));
        thuTuB.forEach(hm -> ghi(dot.ma(), "T002", hm, KetLuanChuyenMon.BINH_THUONG));

        assertThat(khamService.traCuu(dot.ma(), "T001").duHangMuc()).isTrue();
        assertThat(khamService.traCuu(dot.ma(), "T002").duHangMuc()).isTrue();
        assertThat(khamService.traCuu(dot.ma(), "T003").conThieu()).hasSize(5);
    }

    @Test
    void ghi_lai_cung_hang_muc_la_sua_ket_qua_khong_tao_dong_moi() {
        DotKhamView dot = moDotKham();

        ghi(dot.ma(), "T001", HangMuc.THE_LUC, KetLuanChuyenMon.BINH_THUONG);
        TienDoPhieu sauKhiSua = khamService.ghiKetQua(dot.ma(), new GhiKetQuaRequest(
                "T001", HangMuc.THE_LUC, null, "BS. Sửa", KetLuanChuyenMon.CAN_THEO_DOI,
                Map.of("chieuCao", "138"), "Đo lại"));

        assertThat(sauKhiSua.daKham()).hasSize(1);
        assertThat(sauKhiSua.daKham().getFirst().ketLuanChuyenMon()).isEqualTo(KetLuanChuyenMon.CAN_THEO_DOI);
        assertThat(sauKhiSua.daKham().getFirst().chiTiet()).containsEntry("chieuCao", "138");
    }

    @Test
    void khong_ghi_duoc_ket_qua_khi_dot_kham_chua_mo() {
        DotKhamView dot = taoDotKhamDaChuanBi();

        assertThatThrownBy(() -> ghi(dot.ma(), "T001", HangMuc.MAT, KetLuanChuyenMon.BINH_THUONG))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("chưa ghi kết quả được");
    }

    /* ---------- Điểm đau #6: chặn kết luận khi thiếu hạng mục ---------- */

    @Test
    void khong_ket_luan_duoc_khi_con_thieu_hang_muc() {
        DotKhamView dot = moDotKham();
        ghi(dot.ma(), "T001", HangMuc.MAT, KetLuanChuyenMon.BINH_THUONG);

        assertThatThrownBy(() -> ketLuanService.ketLuan(dot.ma(),
                new KetLuanRequest("T001", null, "Khỏe", "BS. Trưởng đoàn")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("còn thiếu 4 bàn");
    }

    @Test
    void ket_luan_dung_phan_loai_de_xuat_khi_bo_trong() {
        DotKhamView dot = moDotKham();
        khamDuHangMuc(dot.ma(), "T001", KetLuanChuyenMon.BINH_THUONG);
        // Một bàn báo cần theo dõi → đề xuất loại II
        ghi(dot.ma(), "T001", HangMuc.MAT, KetLuanChuyenMon.CAN_THEO_DOI);

        TienDoPhieu sau = ketLuanService.ketLuan(dot.ma(),
                new KetLuanRequest("T001", null, "Theo dõi thị lực", "BS. Trưởng đoàn"));

        assertThat(sau.trangThai()).isEqualTo(TrangThaiPhieu.DA_KET_LUAN);
        assertThat(sau.phanLoaiSucKhoe()).isEqualTo(PhanLoaiSucKhoe.II);
    }

    @Test
    void phieu_da_ket_luan_thi_khong_ghi_them_ket_qua() {
        DotKhamView dot = moDotKham();
        khamDuHangMuc(dot.ma(), "T001", KetLuanChuyenMon.BINH_THUONG);
        ketLuanService.ketLuan(dot.ma(), new KetLuanRequest("T001", null, "Khỏe", "BS. Trưởng đoàn"));

        assertThatThrownBy(() -> ghi(dot.ma(), "T001", HangMuc.MAT, KetLuanChuyenMon.BAT_THUONG))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("đã kết luận");
    }

    /* ---------- Điểm đau #7: tổng hợp và đóng đợt ---------- */

    @Test
    void khong_dong_duoc_dot_kham_khi_con_phieu_chua_ket_luan() {
        DotKhamView dot = moDotKham();
        khamDuHangMuc(dot.ma(), "T001", KetLuanChuyenMon.BINH_THUONG);
        ketLuanService.ketLuan(dot.ma(), new KetLuanRequest("T001", null, "Khỏe", "BS. Trưởng đoàn"));

        assertThatThrownBy(() -> dotKhamService.hoanThanh(dot.ma()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Còn 2/3 phiếu chưa kết luận");
    }

    @Test
    void bao_cao_tong_hop_dung_tien_do_va_hoc_sinh_con_thieu() {
        DotKhamView dot = moDotKham();
        khamDuHangMuc(dot.ma(), "T001", KetLuanChuyenMon.BINH_THUONG);
        ketLuanService.ketLuan(dot.ma(), new KetLuanRequest("T001", PhanLoaiSucKhoe.I, "Khỏe", "BS. Trưởng đoàn"));
        ghi(dot.ma(), "T002", HangMuc.MAT, KetLuanChuyenMon.BAT_THUONG);

        var bc = baoCaoService.baoCao(dot.ma());

        assertThat(bc.tongHocSinh()).isEqualTo(3);
        assertThat(bc.daKetLuan()).isEqualTo(1);
        assertThat(bc.dangKham()).isEqualTo(1);
        assertThat(bc.chuaKham()).isEqualTo(1);
        assertThat(bc.phanLoaiSucKhoe()).containsEntry(PhanLoaiSucKhoe.I, 1L);
        assertThat(bc.canTheoDoiTheoHangMuc()).containsEntry("Mắt", 1L);
        assertThat(bc.hocSinhConThieu()).hasSize(2);
        assertThat(baoCaoService.xuatCsv(dot.ma()).lines().count()).isEqualTo(4); // 1 tiêu đề + 3 học sinh
    }

    /* ---------- fixture ---------- */

    private DotKhamView taoDotKhamDaChuanBi() {
        importService.nhapDanhSach(truongId, CSV_SACH, CheDoImport.LUU);
        DotKhamView dot = dotKhamService.tao(new TaoDotKhamRequest(
                truongId, LocalDate.now(), EnumSet.allOf(HangMuc.class), List.of(), "Test"));
        dotKhamService.chuanBi(dot.ma());
        return dotKhamService.chiTiet(dot.ma());
    }

    private DotKhamView moDotKham() {
        DotKhamView dot = taoDotKhamDaChuanBi();
        boSungDuVatTu(dot.ma());
        dotKhamService.xuatPhat(dot.ma());
        return dotKhamService.batDauKham(dot.ma());
    }

    private void boSungDuVatTu(String ma) {
        DotKham dot = dotKhamService.timTheoMa(ma);
        List<CapNhatTaiNguyenRequest> body = taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dot.getId()).stream()
                .map(t -> new CapNhatTaiNguyenRequest(t.getId(), t.tongPhaiCo(), t.tongPhaiCo()))
                .toList();
        dotKhamService.capNhatTaiNguyen(ma, body);
    }

    private List<TaiNguyenView> checklist(String ma) {
        return dotKhamService.checklist(ma);
    }

    private TienDoPhieu ghi(String maDot, String maHocSinh, HangMuc hangMuc, KetLuanChuyenMon ketLuan) {
        return khamService.ghiKetQua(maDot, new GhiKetQuaRequest(
                maHocSinh, hangMuc, null, "BS. Test", ketLuan, Map.of(), null));
    }

    private void khamDuHangMuc(String maDot, String maHocSinh, KetLuanChuyenMon ketLuan) {
        EnumSet.allOf(HangMuc.class).forEach(hm -> ghi(maDot, maHocSinh, hm, ketLuan));
    }

    private List<String> loiCuaDong(KetQuaImport kq, int dong) {
        return kq.dongLoi().stream()
                .filter(d -> d.dong() == dong)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dòng " + dong + " không bị báo lỗi"))
                .loi();
    }
}
