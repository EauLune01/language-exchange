package language.exchange.study.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import language.exchange.study.dto.command.TopicCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicCreateRequest {

    // 방의 두 언어로 쓴 주제명: [{lang:"KO", text:"공원"}, {lang:"JA", text:"公園"}]
    @Valid
    @NotNull(message = "주제명은 필수입니다.")
    @Size(min = 2, max = 2, message = "주제명은 방의 두 언어로 적어야 합니다.")
    private List<@NotNull LocalizedTextRequest> names;

    @Valid
    @NotNull(message = "질문은 필수입니다.")
    @Size(min = 3, max = 3, message = "질문은 정확히 3개여야 합니다.")
    private List<@NotNull QuestionCreateRequest> questions;

    public TopicCreateCommand toCommand() {
        return TopicCreateCommand.of(
                names.stream().map(LocalizedTextRequest::toCommand).toList(),
                questions.stream().map(QuestionCreateRequest::toCommand).toList());
    }
}
