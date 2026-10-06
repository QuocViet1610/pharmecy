package com.medilink.checkup.service;

import com.medilink.checkup.domain.GioiTinh;
import com.medilink.checkup.domain.HocSinh;
import com.medilink.checkup.domain.Truong;
import com.medilink.checkup.repo.HocSinhRepository;
import com.medilink.checkup.repo.TruongRepository;
import com.medilink.checkup.web.ApiException;
import com.medilink.checkup.web.dto.ImportDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

/**
 * Điểm đau #1 — "danh sách nhà trường gửi sang sai, thiếu hoặc trùng".
 * Bắt lỗi ở đây, trước ngày khám, là lúc duy nhất còn sửa được mà không ảnh hưởng đợt khám.
 */
@Service
public class ImportHocSinhService {

    /**
     * ResolverStyle.STRICT (bắt buộc dùng 'uuuu' thay 'yyyy') — nếu để mặc định SMART thì
     * "31/02/2014" bị âm thầm sửa thành 28/02 và lỗi nhập liệu không bao giờ lộ ra.
     */
    private static final List<DateTimeFormatter> DINH_DANG_NGAY = List.of(
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT));

    /** Cột bắt buộc, theo đúng thứ tự trong file mẫu. */
    static final List<String> COT = List.of(
            "ma_dinh_danh", "ho_ten", "ngay_sinh", "gioi_tinh", "lop", "khoi",
            "ho_ten_phu_huynh", "dien_thoai_phu_huynh");

    private final TruongRepository truongRepo;
    private final HocSinhRepository hocSinhRepo;

    public ImportHocSinhService(TruongRepository truongRepo, HocSinhRepository hocSinhRepo) {
        this.truongRepo = truongRepo;
        this.hocSinhRepo = hocSinhRepo;
    }

    @Transactional
    public KetQuaImport nhapDanhSach(Long truongId, String csv, CheDoImport cheDo) {
        Truong truong = truongRepo.findById(truongId)
                .orElseThrow(() -> ApiException.notFound("KHONG_TIM_THAY_TRUONG", "Không tìm thấy trường id=" + truongId));

        List<String> dong = csv.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (dong.isEmpty()) {
            throw ApiException.badRequest("FILE_RONG", "File danh sách không có dòng nào");
        }

        int batDau = laDongTieuDe(dong.getFirst()) ? 1 : 0;
        Map<String, Integer> maDaGap = new HashMap<>();
        List<DongImport> tatCa = new ArrayList<>();

        for (int i = batDau; i < dong.size(); i++) {
            int soDong = i + 1;
            tatCa.add(soiMotDong(truong, dong.get(i), soDong, maDaGap));
        }

        List<DongImport> hopLe = tatCa.stream().filter(DongImport::hopLe).toList();
        List<DongImport> dongLoi = tatCa.stream().filter(d -> !d.hopLe()).toList();
        List<DongImport> dongCanhBao = tatCa.stream().filter(d -> d.hopLe() && !d.canhBao().isEmpty()).toList();

        int daLuu = 0;
        if (cheDo == CheDoImport.LUU) {
            daLuu = luu(truong, dong, batDau, tatCa);
        }

        return new KetQuaImport(cheDo, tatCa.size(), hopLe.size(), dongLoi.size(), daLuu, dongLoi, dongCanhBao);
    }

    private int luu(Truong truong, List<String> dong, int batDau, List<DongImport> daSoi) {
        int daLuu = 0;
        for (int i = batDau; i < dong.size(); i++) {
            DongImport ketQua = daSoi.get(i - batDau);
            if (!ketQua.hopLe()) continue;
            String[] o = tach(dong.get(i));
            hocSinhRepo.save(new HocSinh(
                    truong,
                    lay(o, 0),
                    lay(o, 1),
                    docNgay(lay(o, 2)),
                    docGioiTinh(lay(o, 3)),
                    lay(o, 4),
                    khoiTuLop(lay(o, 5), lay(o, 4)),
                    lay(o, 6),
                    lay(o, 7)));
            daLuu++;
        }
        return daLuu;
    }

    private DongImport soiMotDong(Truong truong, String raw, int soDong, Map<String, Integer> maDaGap) {
        String[] o = tach(raw);
        List<String> loi = new ArrayList<>();
        List<String> canhBao = new ArrayList<>();

        if (o.length < 6) {
            loi.add("Dòng chỉ có " + o.length + " cột, cần tối thiểu 6 cột: " + String.join(", ", COT.subList(0, 6)));
        }

        String ma = lay(o, 0);
        String hoTen = lay(o, 1);
        String ngaySinh = lay(o, 2);
        String gioiTinh = lay(o, 3);
        String lop = lay(o, 4);

        if (ma == null) loi.add("Thiếu mã định danh");
        if (hoTen == null) loi.add("Thiếu họ tên");
        else if (hoTen.length() < 3) canhBao.add("Họ tên quá ngắn, kiểm tra lại: '" + hoTen + "'");
        if (lop == null) loi.add("Thiếu lớp");

        LocalDate ngay = null;
        if (ngaySinh == null) {
            loi.add("Thiếu ngày sinh");
        } else {
            ngay = docNgay(ngaySinh);
            if (ngay == null) {
                loi.add("Ngày sinh '" + ngaySinh + "' không hợp lệ — sai định dạng dd/MM/yyyy"
                        + " hoặc ngày không tồn tại");
            } else if (ngay.isAfter(LocalDate.now())) {
                loi.add("Ngày sinh '" + ngaySinh + "' ở tương lai");
            } else {
                int tuoi = LocalDate.now().getYear() - ngay.getYear();
                if (tuoi < 3 || tuoi > 25) {
                    canhBao.add("Tuổi tính ra " + tuoi + " — không giống học sinh phổ thông, kiểm tra lại ngày sinh");
                }
            }
        }

        if (gioiTinh == null) {
            canhBao.add("Thiếu giới tính, mặc định KHAC");
        } else if (docGioiTinh(gioiTinh) == GioiTinh.KHAC && !"khac".equals(chuanHoa(gioiTinh))) {
            loi.add("Giới tính '" + gioiTinh + "' không hợp lệ (Nam / Nữ / Khác)");
        }

        String dienThoai = lay(o, 7);
        if (dienThoai != null && !dienThoai.matches("0\\d{9,10}")) {
            canhBao.add("Điện thoại phụ huynh '" + dienThoai + "' không đúng dạng 10–11 số bắt đầu bằng 0");
        }

        if (ma != null) {
            Integer dongTruoc = maDaGap.put(ma, soDong);
            if (dongTruoc != null) {
                loi.add("Mã định danh '" + ma + "' bị trùng với dòng " + dongTruoc + " trong cùng file");
            } else if (hocSinhRepo.findByTruongIdAndMaDinhDanh(truong.getId(), ma).isPresent()) {
                loi.add("Mã định danh '" + ma + "' đã có trong hệ thống — nghi nhập trùng danh sách");
            }
        }

        return new DongImport(soDong, ma, hoTen, lop, loi, canhBao);
    }

    private boolean laDongTieuDe(String dong) {
        String chuanHoa = chuanHoa(dong);
        return chuanHoa.contains("ma_dinh_danh") || chuanHoa.contains("ho_ten") || chuanHoa.contains("ho ten");
    }

    private String[] tach(String dong) {
        return dong.split("[,;\\t]", -1);
    }

    private String lay(String[] o, int i) {
        if (i >= o.length) return null;
        String v = o[i].trim().replaceAll("^\"|\"$", "").trim();
        return v.isEmpty() ? null : v;
    }

    private LocalDate docNgay(String v) {
        if (v == null) return null;
        for (DateTimeFormatter f : DINH_DANG_NGAY) {
            try {
                return LocalDate.parse(v, f);
            } catch (DateTimeParseException ignored) {
                // thử định dạng tiếp theo
            }
        }
        return null;
    }

    private GioiTinh docGioiTinh(String v) {
        return switch (chuanHoa(v)) {
            case "nam", "m", "male", "1" -> GioiTinh.NAM;
            case "nu", "f", "female", "2" -> GioiTinh.NU;
            default -> GioiTinh.KHAC;
        };
    }

    /** Khối trống thì suy ra từ tên lớp ("6A2" → "6"). */
    private String khoiTuLop(String khoi, String lop) {
        if (khoi != null) return khoi;
        if (lop == null) return "?";
        String so = lop.replaceAll("\\D.*$", "");
        return so.isEmpty() ? "?" : so;
    }

    private String chuanHoa(String v) {
        if (v == null) return "";
        String s = java.text.Normalizer.normalize(v.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return s.replace('đ', 'd');
    }
}
