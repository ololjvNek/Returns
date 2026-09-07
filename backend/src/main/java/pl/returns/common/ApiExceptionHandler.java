package pl.returns.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ErrorBody> handleApi(ApiException ex) {
		return ResponseEntity.status(ex.status()).body(new ErrorBody(ex.getMessage()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ErrorBody> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErrorBody("Nieprawidłowy login lub hasło"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.orElse("Nieprawidłowe dane");
		return ResponseEntity.badRequest().body(new ErrorBody(message));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorBody> handleOther(Exception ex) {
		log.error("Nieoczekiwany błąd", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorBody("Wystąpił nieoczekiwany błąd serwera"));
	}
}
