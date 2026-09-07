package pl.returns.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {

	List<ReturnRecord> findByPeriodOrderByIdAsc(LocalDate period);

	boolean existsByAllegroReturn(AllegroReturnEntity allegroReturn);
}
