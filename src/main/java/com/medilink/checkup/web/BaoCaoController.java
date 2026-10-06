package com.medilink.checkup.web;

import com.medilink.checkup.service.BaoCaoService;
import com.medilink.checkup.web.dto.BaoCaoDtos.BaoCaoDotKham;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/bao-cao")
public class BaoCaoController {

    private final BaoCaoService service;

    public BaoCaoController(BaoCaoService service) {
        this.service = service;
    }

    @GetMapping("/{ma}")
    public BaoCaoDotKham baoCao(@PathVariable String ma) {
        return service.baoCao(ma);
    }

    @GetMapping("/{ma}/csv")
    public ResponseEntity<byte[]> csv(@PathVariable String ma) {
        byte[] body = service.xuatCsv(ma).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("ket-qua-" + ma + ".csv").build().toString())
                .body(body);
    }
}
