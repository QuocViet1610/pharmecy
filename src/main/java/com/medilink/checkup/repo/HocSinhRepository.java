package com.medilink.checkup.repo;

import com.medilink.checkup.domain.HocSinh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HocSinhRepository extends JpaRepository<HocSinh, Long> {

    Optional<HocSinh> findByTruongIdAndMaDinhDanh(Long truongId, String maDinhDanh);

    List<HocSinh> findByTruongIdOrderByLopAscHoTenAsc(Long truongId);

    List<HocSinh> findByTruongIdAndLopInOrderByLopAscHoTenAsc(Long truongId, List<String> lop);
}
