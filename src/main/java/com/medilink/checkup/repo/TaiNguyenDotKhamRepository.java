package com.medilink.checkup.repo;

import com.medilink.checkup.domain.TaiNguyenDotKham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaiNguyenDotKhamRepository extends JpaRepository<TaiNguyenDotKham, Long> {

    List<TaiNguyenDotKham> findByDotKhamIdOrderByLoaiAscTenAsc(Long dotKhamId);

    void deleteByDotKhamId(Long dotKhamId);
}
