package language.exchange.study.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionListResult {
    private Long topicId;
    private List<QuestionResult> questions;
}