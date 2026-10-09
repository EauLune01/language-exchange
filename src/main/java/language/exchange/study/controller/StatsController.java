package language.exchange.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import language.exchange.global.config.security.RoomPrincipal;
import language.exchange.global.dto.response.ApiResponse;
import language.exchange.study.dto.response.StatsResponse;
import language.exchange.study.dto.result.StatsResult;
import language.exchange.study.service.StatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stats", description = "방의 학습 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsQueryService statsQueryService;

    @Operation(summary = "방의 학습 통계 조회", description = "로그인한 방의 등록한 주제 수(totalTopics), 사용한 주제 수 = 총 회차(usedTopics), 이번 달 횟수(thisMonthCount), 한 주도 거르지 않고 이어온 주 수(weekStreak: 월~일 기준, 이번 주에 아직 안 했으면 지난주까지), 처음 주제를 뽑은 날(firstUsedDate, 없으면 null), 최근 6개월의 월별 횟수(monthly: 오래된 달 → 이번 달, 안 한 달은 0)를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<StatsResponse>> getStats(
            @AuthenticationPrincipal RoomPrincipal principal) {
        StatsResult result = statsQueryService.getStats(principal.getRoomId());
        return ResponseEntity.ok(ApiResponse.success(200, "통계 조회 성공", StatsResponse.from(result)));
    }
}
