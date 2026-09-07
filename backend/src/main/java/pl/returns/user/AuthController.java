package pl.returns.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.returns.common.ApiException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public AuthController(
			AuthenticationManager authenticationManager,
			AppUserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/csrf")
	public CsrfDto csrf(HttpServletRequest request) {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		return new CsrfDto(token != null ? token.getToken() : null, token != null ? token.getHeaderName() : "X-XSRF-TOKEN");
	}

	@PostMapping("/login")
	public UserDto login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		httpRequest.getSession(true);
		return new UserDto(authentication.getName());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		logoutHandler.logout(request, response, authentication);
	}

	@GetMapping("/me")
	public UserDto me(Authentication authentication) {
		if (authentication == null) {
			throw ApiException.unauthorized("Wymagane logowanie");
		}
		return new UserDto(authentication.getName());
	}

	@PutMapping("/profile")
	@Transactional
	public UserDto updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
		AppUser user = userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> ApiException.notFound("Nie znaleziono użytkownika"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("Aktualne hasło jest nieprawidłowe");
		}
		if (!user.getUsername().equals(request.username()) && userRepository.findByUsername(request.username()).isPresent()) {
			throw ApiException.conflict("Taki login już istnieje");
		}
		user.setUsername(request.username());
		if (request.newPassword() != null && !request.newPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		}
		return new UserDto(user.getUsername());
	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record UserDto(String username) {
	}

	public record CsrfDto(String token, String headerName) {
	}

	public record ProfileUpdateRequest(
			@NotBlank @Size(min = 3, max = 100) String username,
			@NotBlank String currentPassword,
			@Size(min = 4, max = 200) String newPassword) {
	}
}
