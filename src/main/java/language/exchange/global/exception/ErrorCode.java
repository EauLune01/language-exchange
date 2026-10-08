package language.exchange.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    // topic
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "주제를 찾을 수 없습니다."),
    NO_AVAILABLE_TOPIC(HttpStatus.NOT_FOUND, "사용 가능한 주제가 없습니다."),
    INVALID_QUESTION_COUNT(HttpStatus.BAD_REQUEST, "질문은 정확히 3개여야 합니다."),
    LANGUAGE_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "이 방에서 사용하는 언어가 아닙니다."),
    // auth·room
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "방 아이디 또는 비밀번호가 올바르지 않습니다."),
    ROOM_NOT_FOUND(HttpStatus.UNAUTHORIZED, "방을 찾을 수 없습니다. 다시 로그인해 주세요."),
    DUPLICATE_ROOM_ID(HttpStatus.CONFLICT, "이미 사용 중인 방 아이디입니다."),
    SAME_LANGUAGE(HttpStatus.BAD_REQUEST, "두 사람의 언어는 서로 달라야 합니다."),
    INVALID_NATIONALITY(HttpStatus.BAD_REQUEST, "국적은 ISO 국가 코드(KR, JP 등)여야 합니다.");

    private final HttpStatus status;
    private final String message;
}