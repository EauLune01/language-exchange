package language.exchange.global.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** 방 로그인 상태를 서버 세션에 넣고 빼는 곳 */
@Component
public class RoomSessionManager {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public void login(RoomPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        // 세션 고정 공격 방지: 로그인할 때 세션 아이디를 새로 발급
        request.getSession();
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, null);
    }
}
