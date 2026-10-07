package language.exchange.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import language.exchange.global.dto.response.ApiResponse;
import language.exchange.global.dto.response.SliceResponse;
import language.exchange.study.domain.Language;
import language.exchange.study.dto.request.TopicBulkCreateRequest;
import language.exchange.study.dto.request.TopicCreateRequest;
import language.exchange.study.dto.response.QuestionListResponse;
import language.exchange.study.dto.response.TopicResponse;
import language.exchange.study.dto.response.TopicSummaryResponse;
import language.exchange.study.dto.result.QuestionListResult;
import language.exchange.study.dto.result.TopicResult;
import language.exchange.study.dto.result.TopicSummaryResult;
import language.exchange.study.service.TopicQueryService;
import language.exchange.study.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Topic", description = "언어교환 주제·질문 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final TopicQueryService topicQueryService;

    @Operation(summary = "주제 1개 + 질문 3개 등록 요청", description = "주제명(한/일)과 질문 3개(한/일)를 큐에 등록하고 즉시 202를 응답합니다. 실제 저장은 비동기로 처리됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "등록 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "질문 개수 오류 또는 필수값 누락 (INVALID_QUESTION_COUNT)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTopic(
            @Valid @RequestBody TopicCreateRequest request) {
        topicService.requestTopicCreation(request.toCommand());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(202, "주제 등록 요청이 접수되었습니다."));
    }

    @Operation(summary = "주제 여러 개 일괄 등록 요청", description = "주제 여러 개를 큐에 등록하고 즉시 202를 응답합니다. 각 주제마다 질문 3개씩 필요하며, 실제 저장은 주제별로 비동기 처리됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "등록 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "질문 개수 오류 또는 필수값 누락 (INVALID_QUESTION_COUNT)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> createTopics(
            @Valid @RequestBody TopicBulkCreateRequest request) {
        topicService.requestTopicBulkCreation(request.toCommand());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(202, "주제 일괄 등록 요청이 접수되었습니다."));
    }

    @Operation(summary = "이번 주 주제 조회", description = "사용되지 않은 주제 중 랜덤으로 1개를 선택하여 한국어/일본어 병기로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용 가능한 주제 없음 (NO_AVAILABLE_TOPIC)")
    })
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<TopicResponse>> getWeeklyTopic() {
        TopicResult result = topicQueryService.getWeeklyTopic();
        return ResponseEntity.ok(
                ApiResponse.success(200, "주제 조회 성공", TopicResponse.from(result)));
    }

    @Operation(summary = "질문 3개 조회", description = "지정한 주제의 질문 3개를 조회합니다. lang 파라미터로 한국어(KO) 또는 일본어(JA)를 선택할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주제를 찾을 수 없음 (TOPIC_NOT_FOUND)")
    })
    @GetMapping("/{topicId}/questions")
    public ResponseEntity<ApiResponse<QuestionListResponse>> getQuestions(
            @PathVariable Long topicId,
            @Parameter(description = "KO(한국어) 또는 JA(일본어)", example = "KO")
            @RequestParam(defaultValue = "KO") Language lang) {
        QuestionListResult result = topicQueryService.getQuestions(topicId);
        return ResponseEntity.ok(
                ApiResponse.success(200, "질문 조회 성공", QuestionListResponse.of(result, lang)));
    }

    @Operation(summary = "전체 주제 목록 조회", description = "사용 여부와 관계없이 전체 주제를 Slice 형태로 조회합니다. usedDate가 null이면 아직 사용하지 않은 주제입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<SliceResponse<TopicSummaryResponse>>> getTopics(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<TopicSummaryResult> result = topicQueryService.getTopics(pageable);
        SliceResponse<TopicSummaryResponse> response =
                SliceResponse.from(result.map(TopicSummaryResponse::from));
        return ResponseEntity.ok(ApiResponse.success(200, "주제 목록 조회 성공", response));
    }
}