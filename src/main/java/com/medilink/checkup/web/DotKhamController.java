package com.medilink.checkup.web;

import com.medilink.checkup.domain.HangMuc;
import com.medilink.checkup.service.DotKhamService;
import com.medilink.checkup.web.dto.DotKhamDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dot-kham")
public class DotKhamController {

    private final DotKhamService service;

    public DotKhamController(DotKhamService service) {
        this.service = service;
    }

    @GetMapping
    public List<DotKhamView> danhSach() {
        return service.danhSach();
    }

    @PostMapping
    public DotKhamView tao(@Valid @RequestBody TaoDotKhamRequest req) {
        return service.tao(req);
    }

    @GetMapping("/{ma}")
    public DotKhamView chiTiet(@PathVariable String ma) {
        return service.chiTiet(ma);
    }

    /** Sinh phiếu khám, dựng bàn khám, tính checklist từ số học sinh thực tế. */
    @PostMapping("/{ma}/chuan-bi")
    public ChuanBiResult chuanBi(@PathVariable String ma) {
        return service.chuanBi(ma);
    }

    @GetMapping("/{ma}/ban-kham")
    public List<BanKhamView> banKham(@PathVariable String ma) {
        return service.banKham(ma);
    }

    @GetMapping("/{ma}/checklist")
    public List<TaiNguyenView> checklist(@PathVariable String ma) {
        return service.checklist(ma);
    }

    @PutMapping("/{ma}/checklist")
    public List<TaiNguyenView> capNhatChecklist(@PathVariable String ma,
                                                @Valid @RequestBody List<CapNhatTaiNguyenRequest> reqs) {
        return service.capNhatTaiNguyen(ma, reqs);
    }

    @GetMapping("/{ma}/kiem-ke")
    public KetQuaKiemKe kiemKe(@PathVariable String ma) {
        return service.kiemKe(ma);
    }

    @PostMapping("/{ma}/xuat-phat")
    public DotKhamView xuatPhat(@PathVariable String ma) {
        return service.xuatPhat(ma);
    }

    @PostMapping("/{ma}/bat-dau-kham")
    public DotKhamView batDauKham(@PathVariable String ma) {
        return service.batDauKham(ma);
    }

    @PostMapping("/{ma}/hoan-thanh")
    public DotKhamView hoanThanh(@PathVariable String ma) {
        return service.hoanThanh(ma);
    }

    /** UI dùng để dựng form nhập kết quả theo từng bàn. */
    @GetMapping("/hang-muc")
    public List<HangMucView> hangMuc() {
        return List.of(HangMuc.values()).stream().map(HangMucView::cua).toList();
    }
}
