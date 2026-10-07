package language.exchange.study.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import language.exchange.study.dto.command.QuestionCreateCommand;
import language.exchange.study.dto.command.TopicCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicCreateRequest {

    @NotBlank(message = "한국어 주제명은 필수입니다.")
    private String nameKo;

    @NotBlank(message = "일본어 주제명은 필수입니다.")
    private String nameJa;

    @Valid
    @NotNull(message = "질문은 필수입니다.")
    @Size(min = 3, max = 3, message = "질문은 정확히 3개여야 합니다.")
    private List<QuestionCreateRequest> questions;

    public TopicCreateCommand toCommand() {
        List<QuestionCreateCommand> questionCommands = questions.stream()
                .map(question -> new QuestionCreateCommand(question.getContentKo(), question.getContentJa()))
                .toList();

        return TopicCreateCommand.builder()
                .nameKo(nameKo)
                .nameJa(nameJa)
                .questions(questionCommands)
                .build();
    }
}