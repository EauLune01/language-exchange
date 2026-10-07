package language.exchange.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Topic
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "주제를 찾을 수 없습니다."),
    NO_AVAILABLE_TOPIC(HttpStatus.NOT_FOUND, "사용 가능한 주제가 없습니다."),
    INVALID_QUESTION_COUNT(HttpStatus.BAD_REQUEST, "질문은 정확히 3개여야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}