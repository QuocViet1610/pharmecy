package com.medilink.checkup.repo;

import com.medilink.checkup.domain.BanKham;
import com.medilink.checkup.domain.HangMuc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BanKhamRepository extends JpaRepository<BanKham, Long> {

    List<BanKham> findByDotKhamIdOrderByHangMucAscTenAsc(Long dotKhamId);

    List<BanKham> findByDotKhamIdAndHangMuc(Long dotKhamId, HangMuc hangMuc);
}
