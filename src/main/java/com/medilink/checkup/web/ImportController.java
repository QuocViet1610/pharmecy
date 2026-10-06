package com.medilink.checkup.web;

import com.medilink.checkup.service.ImportHocSinhService;
import com.medilink.checkup.web.dto.ImportDtos.CheDoImport;
import com.medilink.checkup.web.dto.ImportDtos.KetQuaImport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final ImportHocSinhService service;

    public ImportController(ImportHocSinhService service) {
        this.service = service;
    }

    /** Dán nội dung CSV (hoặc copy từ Excel) — mặc định chỉ kiểm tra, không ghi DB. */
    @PostMapping(path = "/hoc-sinh", consumes = MediaType.TEXT_PLAIN_VALUE)
    public KetQuaImport nhapTuText(@RequestParam Long truongId,
                                   @RequestParam(defaultValue = "KIEM_TRA") CheDoImport cheDo,
                                   @RequestBody String csv) {
        return service.nhapDanhSach(truongId, csv, cheDo);
    }

    @PostMapping(path = "/hoc-sinh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KetQuaImport nhapTuFile(@RequestParam Long truongId,
                                   @RequestParam(defaultValue = "KIEM_TRA") CheDoImport cheDo,
                                   @RequestPart("file") MultipartFile file) {
        try {
            return service.nhapDanhSach(truongId, new String(file.getBytes(), StandardCharsets.UTF_8), cheDo);
        } catch (IOException e) {
            throw ApiException.badRequest("KHONG_DOC_DUOC_FILE", "Không đọc được file: " + e.getMessage());
        }
    }
}
