package com.medilink.checkup.web;

import com.medilink.checkup.service.KetLuanService;
import com.medilink.checkup.service.KhamService;
import com.medilink.checkup.web.dto.KhamDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dot-kham/{ma}")
public class KhamController {

    private final KhamService khamService;
    private final KetLuanService ketLuanService;

    public KhamController(KhamService khamService, KetLuanService ketLuanService) {
        this.khamService = khamService;
        this.ketLuanService = ketLuanService;
    }

    /** Quét QR hoặc gõ mã định danh tại bàn khám. */
    @GetMapping("/tra-cuu")
    public TienDoPhieu traCuu(@PathVariable String ma, @RequestParam("q") String q) {
        return khamService.traCuu(ma, q);
    }

    /** Ghi kết quả tại một bàn — gọi được theo thứ tự bất kỳ. */
    @PostMapping("/ket-qua")
    public TienDoPhieu ghiKetQua(@PathVariable String ma, @Valid @RequestBody GhiKetQuaRequest req) {
        return khamService.ghiKetQua(ma, req);
    }

    /** Bảng theo dõi khu khám: ai đã đủ, ai còn thiếu bàn nào. */
    @GetMapping("/tien-do")
    public List<TienDoPhieu> tienDo(@PathVariable String ma,
                                    @RequestParam(required = false) String lop,
                                    @RequestParam(required = false) Boolean conThieu) {
        return khamService.tienDoDotKham(ma, lop, conThieu);
    }

    @PostMapping("/ket-luan")
    public TienDoPhieu ketLuan(@PathVariable String ma, @Valid @RequestBody KetLuanRequest req) {
        return ketLuanService.ketLuan(ma, req);
    }
}
