package language.exchange.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import language.exchange.global.config.security.RoomPrincipal;
import language.exchange.global.config.security.RoomSessionManager;
import language.exchange.global.dto.response.ApiResponse;
import language.exchange.room.dto.request.RoomCreateRequest;
import language.exchange.room.dto.response.RoomResponse;
import language.exchange.room.dto.result.RoomResult;
import language.exchange.room.service.RoomQueryService;
import language.exchange.room.service.RoomService;
import language.exchange.study.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "방(공용 계정) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomQueryService roomQueryService;
    private final RoomSessionManager roomSessionManager;
    private final TopicService topicService;

    @Operation(summary = "방 만들기", description = "방 아이디·비밀번호와 두 사람의 이름·국적·배우고 싶은 언어(learningLanguage), 목표 횟수(goal: 25/50/75/100)로 방을 만들고 바로 로그인합니다. members는 [첫 번째 사람, 두 번째 사람] 순서입니다. 서로 상대의 언어를 배우는 교환이라, 한 사람이 쓰는 언어는 상대가 배우고 싶은 언어로 정해지며 두 언어는 서로 달라야 합니다. 국적은 ISO 3166-1 alpha-2 국가 코드(KR, JP 등)입니다. useDefaultTopics 가 true 면 방의 두 언어로 준비된 기본 추천 주제를 함께 등록합니다(비동기). 준비된 주제가 없는 언어 조합이면 아무것도 등록하지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "방 생성 및 로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 오류, 같은 언어 선택 (SAME_LANGUAGE), 잘못된 국가 코드 (INVALID_NATIONALITY)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 방 아이디 (DUPLICATE_ROOM_ID)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createRoom(
            @Valid @RequestBody RoomCreateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Long roomId = roomService.createRoom(request.toCommand());
        if (request.isUseDefaultTopics()) {
            topicService.requestDefaultTopics(roomId);
        }
        roomSessionManager.login(RoomPrincipal.of(roomId, request.getLoginId()), httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "방이 만들어졌습니다."));
    }

    @Operation(summary = "현재 방 정보 조회", description = "로그인한 방의 아이디와 두 사람의 이름·국적·쓰는 언어(language)·배우고 싶은 언어(learningLanguage), 목표 횟수(goal)와 지금까지 학습한 횟수(studiedCount)를 반환합니다. members는 [첫 번째 사람(A), 두 번째 사람(B)] 순서입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(
            @AuthenticationPrincipal RoomPrincipal principal) {
        RoomResult result = roomQueryService.getRoom(principal.getRoomId());
        return ResponseEntity.ok(ApiResponse.success(200, "방 정보 조회 성공", RoomResponse.from(result)));
    }
}
