package com.medilink.checkup.service;

import com.medilink.checkup.domain.PhanLoaiSucKhoe;
import com.medilink.checkup.domain.PhieuKham;
import com.medilink.checkup.web.ApiException;
import com.medilink.checkup.web.dto.KhamDtos.KetLuanRequest;
import com.medilink.checkup.web.dto.KhamDtos.TienDoPhieu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Bàn kết luận. Điểm đau #6 — học sinh bỏ sót hạng mục mà vẫn được kết luận:
 * ở đây là chốt cứng, thiếu hạng mục bắt buộc thì không kết luận được và API trả về
 * đúng danh sách bàn còn thiếu để hướng dẫn học sinh quay lại.
 */
@Service
public class KetLuanService {

    private final KhamService khamService;

    public KetLuanService(KhamService khamService) {
        this.khamService = khamService;
    }

    @Transactional
    public TienDoPhieu ketLuan(String maDotKham, KetLuanRequest req) {
        PhieuKham phieu = khamService.timPhieu(maDotKham, req.maNhanDien());
        TienDoPhieu tienDo = khamService.tienDo(phieu);

        if (!tienDo.duHangMuc()) {
            List<String> thieu = tienDo.conThieu().stream().map(h -> h.ten()).toList();
            throw ApiException.conflict("THIEU_HANG_MUC",
                    "Học sinh " + phieu.getHocSinh().getHoTen() + " còn thiếu " + thieu.size()
                            + " bàn: " + String.join(", ", thieu) + " — mời quay lại trước khi kết luận",
                    Map.of("soPhieu", phieu.getSoPhieu(),
                            "hoTen", phieu.getHocSinh().getHoTen(),
                            "lop", phieu.getHocSinh().getLop(),
                            "conThieu", tienDo.conThieu()));
        }

        PhanLoaiSucKhoe phanLoai = req.phanLoaiSucKhoe() != null
                ? req.phanLoaiSucKhoe()
                : tienDo.phanLoaiDeXuat();
        if (phanLoai == null) {
            throw ApiException.badRequest("THIEU_PHAN_LOAI", "Chưa có căn cứ phân loại sức khỏe cho phiếu này");
        }

        phieu.ketLuan(phanLoai, req.ketLuan(), req.nguoiKetLuan());
        return khamService.tienDo(phieu);
    }
}
