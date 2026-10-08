package language.exchange.study.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import language.exchange.study.dto.command.QuestionCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCreateRequest {

    // 방의 두 언어로 쓴 같은 질문: [{lang:"KO", text:"…"}, {lang:"JA", text:"…"}]
    @Valid
    @NotNull(message = "질문 내용은 필수입니다.")
    @Size(min = 2, max = 2, message = "질문은 방의 두 언어로 적어야 합니다.")
    private List<@NotNull LocalizedTextRequest> contents;

    public QuestionCreateCommand toCommand() {
        return QuestionCreateCommand.from(contents.stream().map(LocalizedTextRequest::toCommand).toList());
    }
}
