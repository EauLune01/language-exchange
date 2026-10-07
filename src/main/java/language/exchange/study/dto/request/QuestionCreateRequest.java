package language.exchange.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCreateRequest {

    @NotBlank(message = "한국어 질문은 필수입니다.")
    private String contentKo;

    @NotBlank(message = "일본어 질문은 필수입니다.")
    private String contentJa;
}