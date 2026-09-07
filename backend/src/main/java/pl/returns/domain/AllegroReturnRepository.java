package pl.returns.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AllegroReturnRepository extends JpaRepository<AllegroReturnEntity, UUID>, JpaSpecificationExecutor<AllegroReturnEntity> {

	List<AllegroReturnEntity> findByAccountAndLocalStateAndStatusNotIn(
			AllegroAccount account, LocalReturnState localState, Collection<String> statuses);

	List<AllegroReturnEntity> findByLocalStateAndApiloOrderNumberIsNull(LocalReturnState localState);
}
