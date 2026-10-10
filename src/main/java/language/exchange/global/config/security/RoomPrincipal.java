package language.exchange.global.config.security;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/** 로그인 주체는 사람이 아니라 방입니다. 세션에 저장됩니다. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomPrincipal implements Serializable {

    // 세션이 Redis 에 직렬화돼 있어서, 값을 고정해 두지 않으면 이 클래스를 다시 컴파일한 배포에서 기존 로그인이 풀릴 수 있다
    private static final long serialVersionUID = 1L;

    private final Long roomId;
    private final String loginId;

    public static RoomPrincipal of(Long roomId, String loginId) {
        return new RoomPrincipal(roomId, loginId);
    }
}
