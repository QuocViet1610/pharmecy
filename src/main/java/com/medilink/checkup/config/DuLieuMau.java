package com.medilink.checkup.config;

import com.medilink.checkup.domain.*;
import com.medilink.checkup.repo.*;
import com.medilink.checkup.service.DotKhamService;
import com.medilink.checkup.web.dto.DotKhamDtos.TaoDotKhamRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Dữ liệu mẫu cho demo: 1 trường, 3 lớp (75 học sinh), 1 đợt khám đã chuẩn bị nhưng
 * checklist còn thiếu vài dòng — để thấy ngay chốt "không cho xuất phát khi thiếu vật tư".
 */
@Component
public class DuLieuMau implements ApplicationRunner {

    private static final String[] HO = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"};
    private static final String[] DEM_NAM = {"Văn", "Hữu", "Quang", "Minh", "Đức", "Thành"};
    private static final String[] DEM_NU = {"Thị", "Thu", "Ngọc", "Thanh", "Khánh", "Mai"};
    private static final String[] TEN_NAM = {"An", "Bình", "Cường", "Dũng", "Hải", "Khoa", "Long", "Nam", "Phúc", "Sơn", "Tuấn", "Việt"};
    private static final String[] TEN_NU = {"Anh", "Chi", "Dung", "Hà", "Hương", "Linh", "Mai", "Ngân", "Như", "Phương", "Trang", "Yến"};

    private final TruongRepository truongRepo;
    private final HocSinhRepository hocSinhRepo;
    private final DotKhamRepository dotKhamRepo;
    private final TaiNguyenDotKhamRepository taiNguyenRepo;
    private final DotKhamService dotKhamService;

    public DuLieuMau(TruongRepository truongRepo, HocSinhRepository hocSinhRepo, DotKhamRepository dotKhamRepo,
                     TaiNguyenDotKhamRepository taiNguyenRepo, DotKhamService dotKhamService) {
        this.truongRepo = truongRepo;
        this.hocSinhRepo = hocSinhRepo;
        this.dotKhamRepo = dotKhamRepo;
        this.taiNguyenRepo = taiNguyenRepo;
        this.dotKhamService = dotKhamService;
    }

    /**
     * {@code @Transactional} chỉ có tác dụng khi Spring gọi qua proxy — nên phương thức seed phải là
     * {@code run} của bean này, không phải một hàm nội bộ do lambda gọi (self-invocation bỏ qua proxy,
     * các thay đổi trên entity sẽ không được flush).
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (truongRepo.count() > 0) return;

        Truong truong = truongRepo.save(new Truong(
                "Trường THCS Lê Quý Đôn", "12 Nguyễn Trãi, Thanh Xuân, Hà Nội", "Cô Hoà - Y tế trường", "0912345678"));

        Random rnd = new Random(20261006);
        int stt = 0;
        for (String lop : List.of("6A1", "6A2", "7A1")) {
            String khoi = lop.substring(0, 1);
            int namSinh = 2026 - (Integer.parseInt(khoi) + 6);
            for (int i = 0; i < 25; i++) {
                stt++;
                boolean nam = rnd.nextBoolean();
                String hoTen = HO[rnd.nextInt(HO.length)] + " "
                        + (nam ? DEM_NAM[rnd.nextInt(DEM_NAM.length)] : DEM_NU[rnd.nextInt(DEM_NU.length)]) + " "
                        + (nam ? TEN_NAM[rnd.nextInt(TEN_NAM.length)] : TEN_NU[rnd.nextInt(TEN_NU.length)]);
                hocSinhRepo.save(new HocSinh(truong,
                        "HS%05d".formatted(stt),
                        hoTen,
                        LocalDate.of(namSinh, 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)),
                        nam ? GioiTinh.NAM : GioiTinh.NU,
                        lop, khoi,
                        "Phụ huynh " + hoTen,
                        "09%08d".formatted(rnd.nextInt(100_000_000))));
            }
        }

        dotKhamService.tao(new TaoDotKhamRequest(truong.getId(), LocalDate.now(),
                EnumSet.allOf(HangMuc.class), List.of("6A1", "6A2", "7A1"),
                "Khám sức khỏe định kỳ đầu năm học"));
        DotKham dotKham = dotKhamRepo.findAllByOrderByNgayKhamDesc().getFirst();
        dotKhamService.chuanBi(dotKham.getMa());

        // Cố tình để 2 dòng chưa đủ: demo chốt kiểm kê trước khi xuất phát.
        for (TaiNguyenDotKham t : taiNguyenRepo.findByDotKhamIdOrderByLoaiAscTenAsc(dotKham.getId())) {
            boolean coTinhThieu = "Que đè lưỡi".equals(t.getTen()) || "Máy đo huyết áp".equals(t.getTen());
            t.capNhatChuanBi(coTinhThieu ? t.tongPhaiCo() - Math.max(1, t.tongPhaiCo() / 4) : t.tongPhaiCo());
        }
    }
}
