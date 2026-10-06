package com.medilink.checkup.web;

import com.medilink.checkup.domain.GioiTinh;
import com.medilink.checkup.domain.HocSinh;
import com.medilink.checkup.domain.Truong;
import com.medilink.checkup.repo.HocSinhRepository;
import com.medilink.checkup.repo.TruongRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/truong")
public class TruongController {

    private final TruongRepository truongRepo;
    private final HocSinhRepository hocSinhRepo;

    public TruongController(TruongRepository truongRepo, HocSinhRepository hocSinhRepo) {
        this.truongRepo = truongRepo;
        this.hocSinhRepo = hocSinhRepo;
    }

    public record TaoTruongRequest(@NotBlank String ten, String diaChi, String nguoiLienHe, String dienThoai) {}

    public record TruongView(Long id, String ten, String diaChi, String nguoiLienHe, long soHocSinh,
                             List<String> lop) {}

    public record HocSinhView(Long id, String maDinhDanh, String hoTen, LocalDate ngaySinh, GioiTinh gioiTinh,
                              String lop, String khoi, String hoTenPhuHuynh, String dienThoaiPhuHuynh) {}

    @GetMapping
    public List<TruongView> danhSach() {
        return truongRepo.findAll().stream().map(this::view).toList();
    }

    @PostMapping
    public TruongView tao(@Valid @RequestBody TaoTruongRequest req) {
        return view(truongRepo.save(
                new Truong(req.ten(), req.diaChi(), req.nguoiLienHe(), req.dienThoai())));
    }

    @GetMapping("/{id}/hoc-sinh")
    public List<HocSinhView> hocSinh(@PathVariable Long id) {
        return hocSinhRepo.findByTruongIdOrderByLopAscHoTenAsc(id).stream()
                .map(h -> new HocSinhView(h.getId(), h.getMaDinhDanh(), h.getHoTen(), h.getNgaySinh(),
                        h.getGioiTinh(), h.getLop(), h.getKhoi(), h.getHoTenPhuHuynh(), h.getDienThoaiPhuHuynh()))
                .toList();
    }

    private TruongView view(Truong t) {
        List<HocSinh> hs = hocSinhRepo.findByTruongIdOrderByLopAscHoTenAsc(t.getId());
        return new TruongView(t.getId(), t.getTen(), t.getDiaChi(), t.getNguoiLienHe(), hs.size(),
                hs.stream().map(HocSinh::getLop).distinct().sorted().toList());
    }
}
