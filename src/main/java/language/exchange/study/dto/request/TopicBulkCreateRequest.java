package language.exchange.study.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import language.exchange.study.dto.command.TopicBulkCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicBulkCreateRequest {

    @Valid
    @NotEmpty(message = "최소 1개 이상의 주제를 입력해주세요.")
    private List<TopicCreateRequest> topics;

    public TopicBulkCreateCommand toCommand() {
        return TopicBulkCreateCommand.builder()
                .topics(topics.stream().map(TopicCreateRequest::toCommand).toList())
                .build();
    }
}
