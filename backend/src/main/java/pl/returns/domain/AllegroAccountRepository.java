package pl.returns.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllegroAccountRepository extends JpaRepository<AllegroAccount, Long> {

	List<AllegroAccount> findByEnabledTrue();
}
