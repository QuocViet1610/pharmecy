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

    long countByDotKhamId(Long dotKhamId);

    long countByDotKhamIdAndTrangThai(Long dotKhamId, TrangThaiPhieu trangThai);

    boolean existsByDotKhamIdAndHocSinhId(Long dotKhamId, Long hocSinhId);
}
