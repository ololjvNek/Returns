package pl.returns.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

	private final AppUserRepository repository;
	private final PasswordEncoder passwordEncoder;

	public AdminUserInitializer(AppUserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() == 0) {
			repository.save(new AppUser("admin", passwordEncoder.encode("admin")));
		}
	}
}
