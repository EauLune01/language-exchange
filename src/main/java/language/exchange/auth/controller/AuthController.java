package language.exchange.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import language.exchange.auth.dto.request.LoginRequest;
import language.exchange.auth.service.AuthService;
import language.exchange.global.config.security.RoomPrincipal;
import language.exchange.global.config.security.RoomSessionManager;
import language.exchange.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "방 로그인·로그아웃 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RoomSessionManager roomSessionManager;

    @Operation(summary = "로그인", description = "방 아이디와 비밀번호로 로그인합니다. 성공하면 세션 쿠키가 발급됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "방 아이디 또는 비밀번호 불일치 (INVALID_CREDENTIALS)")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Long roomId = authService.login(request.toCommand());
        roomSessionManager.login(RoomPrincipal.of(roomId, request.getLoginId()), httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(200, "로그인 성공"));
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 끝냅니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        roomSessionManager.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(200, "로그아웃 성공"));
    }
}
