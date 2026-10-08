package language.exchange.global.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import language.exchange.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"success", "code", "errorCode", "message", "data"})
public class ApiResponse<T> {

    private final boolean success;
    private final int code;

    // 실패 응답에만 담는다 (ErrorCode 이름). 프론트가 화면 언어로 메시지를 고르는 기준
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String errorCode;

    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(true, code, null, message, data);
    }

    public static ApiResponse<Void> success(int code, String message) {
        return new ApiResponse<>(true, code, null, message, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return fail(errorCode, message, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.name(), message, data);
    }
}
