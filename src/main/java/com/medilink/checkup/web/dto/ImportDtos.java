package com.medilink.checkup.web.dto;

import java.util.List;

public final class ImportDtos {

    private ImportDtos() {}

    /** KIEM_TRA = chỉ soi lỗi, không ghi DB. LUU = ghi các dòng hợp lệ, bỏ qua dòng lỗi. */
    public enum CheDoImport { KIEM_TRA, LUU }

    public record DongImport(
            int dong,
            String maDinhDanh,
            String hoTen,
            String lop,
            List<String> loi,
            List<String> canhBao
    ) {
        public boolean hopLe() { return loi.isEmpty(); }
    }

    public record KetQuaImport(
            CheDoImport cheDo,
            int tongDong,
            int soDongHopLe,
            int soDongLoi,
            int soDaLuu,
            List<DongImport> dongLoi,
            List<DongImport> dongCanhBao
    ) {}
}
