package com.medilink.checkup.repo;

import com.medilink.checkup.domain.HangMuc;
import com.medilink.checkup.domain.KetQuaKham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KetQuaKhamRepository extends JpaRepository<KetQuaKham, Long> {

    List<KetQuaKham> findByPhieuKhamIdOrderByThoiDiemAsc(Long phieuKhamId);

    Optional<KetQuaKham> findByPhieuKhamIdAndHangMuc(Long phieuKhamId, HangMuc hangMuc);

    @Query("select k.hangMuc, count(k) from KetQuaKham k where k.phieuKham.dotKham.id = :dotKhamId group by k.hangMuc")
    List<Object[]> demTheoHangMuc(Long dotKhamId);

    @Query("select k from KetQuaKham k where k.phieuKham.dotKham.id = :dotKhamId")
    List<KetQuaKham> findByDotKham(Long dotKhamId);
}
