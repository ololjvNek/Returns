package pl.returns.user;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository repository;

	public AppUserDetailsService(AppUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = repository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika"));
		return new User(user.getUsername(), user.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
