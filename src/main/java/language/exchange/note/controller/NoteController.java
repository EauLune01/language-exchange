package language.exchange.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import language.exchange.global.config.security.RoomPrincipal;
import language.exchange.global.dto.response.ApiResponse;
import language.exchange.room.domain.Language;
import language.exchange.note.dto.request.NoteUpsertRequest;
import language.exchange.note.dto.response.NoteResponse;
import language.exchange.note.dto.result.NoteResult;
import language.exchange.note.service.NoteQueryService;
import language.exchange.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Note", description = "질문 메모 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questions/{questionId}/note")
public class NoteController {

    private final NoteService noteService;
    private final NoteQueryService noteQueryService;

    @Operation(summary = "질문 메모 저장", description = "로그인한 방이 그 질문에 lang 언어로 적은 메모를 저장합니다. 이미 있으면 내용을 바꾸고, 없으면 새로 만듭니다. 메모는 방 × 질문 × 언어마다 따로 저장됩니다. 이미 뽑아서 사용한 주제의 질문에만 적을 수 있습니다. lang은 필수이며 방의 두 언어 중 하나여야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "빈 내용 (NOTE_CONTENT_BLANK), 방의 언어가 아님 (LANGUAGE_NOT_IN_ROOM), 2000자 초과·lang 누락·잘못된 값 (INVALID_INPUT)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "아직 사용하지 않은 주제의 질문 (TOPIC_NOT_USED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음 또는 다른 방의 질문 (NOT_FOUND)")
    })
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> upsertNote(
            @AuthenticationPrincipal RoomPrincipal principal,
            @PathVariable("questionId") Long questionId,
            @Parameter(description = "방의 두 언어 중 하나 (예: KO, JA)", example = "KO")
            @RequestParam("lang") Language lang,
            @Valid @RequestBody NoteUpsertRequest request) {
        noteService.upsert(request.toCommand(questionId, principal.getRoomId(), lang));
        return ResponseEntity.ok(ApiResponse.success(200, "메모가 저장되었습니다."));
    }

    @Operation(summary = "질문 메모 조회", description = "로그인한 방이 그 질문에 lang 언어로 적은 메모를 조회합니다. 아직 적은 메모가 없으면 content 가 빈 문자열입니다. lang은 필수이며 방의 두 언어 중 하나여야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "방의 언어가 아님 (LANGUAGE_NOT_IN_ROOM), lang 누락·잘못된 값 (INVALID_INPUT)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(
            @AuthenticationPrincipal RoomPrincipal principal,
            @PathVariable("questionId") Long questionId,
            @Parameter(description = "방의 두 언어 중 하나 (예: KO, JA)", example = "KO")
            @RequestParam("lang") Language lang) {
        NoteResult result = noteQueryService.find(questionId, principal.getRoomId(), lang);
        return ResponseEntity.ok(ApiResponse.success(200, "메모 조회 성공", NoteResponse.from(result)));
    }
}
