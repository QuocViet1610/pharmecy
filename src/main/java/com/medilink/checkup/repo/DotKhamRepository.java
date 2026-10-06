package com.medilink.checkup.repo;

import com.medilink.checkup.domain.DotKham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DotKhamRepository extends JpaRepository<DotKham, Long> {

    Optional<DotKham> findByMa(String ma);

    boolean existsByMa(String ma);

    List<DotKham> findAllByOrderByNgayKhamDesc();
}
