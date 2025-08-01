package app.g_agent.policy_service.system;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<String> handleNotFoundException(NoHandlerFoundException ex) {

		Map<String, String> errors = new HashMap<>();
		String path = ex.getRequestURL();
		String errorMessage = ex.getLocalizedMessage();
		ErrorDetails errorDetails = new ErrorDetails(404, errorMessage, path);

		return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(DuplicateContactException.class)
	public ResponseEntity<String> handleDuplicateContactException(DuplicateContactException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("name", "Error");
		error.put("message", ex.getMessage());
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
		ErrorDetails errorDetails = new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"An unexpected error occurred. " + ex.getLocalizedMessage(),
				request.getDescription(false).replace("uri=", ""));
		logger.info("Error caught ==========> " + ex.getMessage());
		//return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
	}
}

class ErrorDetails {
	private int status;
	private String message;
	private String path;

	public ErrorDetails(int status, String message, String path) {
		this.status = status;
		this.message = message;
		this.path = path;
	}

	public ErrorDetails(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
