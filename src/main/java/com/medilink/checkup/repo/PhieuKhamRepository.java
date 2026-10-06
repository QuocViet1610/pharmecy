package com.medilink.checkup.repo;

import com.medilink.checkup.domain.PhieuKham;
import com.medilink.checkup.domain.TrangThaiPhieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PhieuKhamRepository extends JpaRepository<PhieuKham, Long> {

    Optional<PhieuKham> findBySoPhieu(String soPhieu);

    @Query("""
            select p from PhieuKham p
              join fetch p.hocSinh hs
            where p.dotKham.id = :dotKhamId
              and (lower(hs.maDinhDanh) = lower(:ma) or lower(p.soPhieu) = lower(:ma))
            """)
    Optional<PhieuKham> traCuu(Long dotKhamId, String ma);

    @Query("""
            select p from PhieuKham p
              join fetch p.hocSinh
            where p.dotKham.id = :dotKhamId
            order by p.soPhieu
            """)
    List<PhieuKham> findByDotKham(Long dotKhamId);

    /**
     * Tìm học sinh tại bàn khám: khớp chính xác mã định danh/số phiếu (quét QR),
     * hoặc khớp một phần tên — cả khi người dùng không gõ dấu.
     */
    @Query("""
            select p from PhieuKham p
              join fetch p.hocSinh hs
            where p.dotKham.id = :dotKhamId
              and (
                    lower(hs.maDinhDanh) = :q
                 or lower(p.soPhieu) = :q
                 or lower(hs.hoTen) like concat('%', :q, '%')
                 or hs.hoTenTimKiem like concat('%', :qKhongDau, '%')
              )
              and (:lop is null or lower(hs.lop) = :lop)
            order by hs.lop, hs.hoTen
            """)
    List<PhieuKham> tim(Long dotKhamId, String q, String qKhongDau, String lop);

    long countByDotKhamId(Long dotKhamId);

    long countByDotKhamIdAndTrangThai(Long dotKhamId, TrangThaiPhieu trangThai);

    boolean existsByDotKhamIdAndHocSinhId(Long dotKhamId, Long hocSinhId);
}
