package language.exchange.global.exception;

import language.exchange.global.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationExceptions(MethodArgumentNotValidException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다.", getErrorFields(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다.");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: {}", errorCode);
        return buildErrorResponse(errorCode.getStatus(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message) {
        ApiResponse<Void> response = ApiResponse.fail(status.value(), message);
        return ResponseEntity.status(status).body(response);
    }

    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(HttpStatus status, String message, T data) {
        ApiResponse<T> response = ApiResponse.fail(status.value(), message, data);
        return ResponseEntity.status(status).body(response);
    }

    private static List<String> getErrorFields(MethodArgumentNotValidException e) {
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
    }
}