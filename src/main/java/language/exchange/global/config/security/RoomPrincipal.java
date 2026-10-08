package language.exchange.global.config.security;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/** 로그인 주체는 사람이 아니라 방입니다. 세션에 저장됩니다. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomPrincipal implements Serializable {

    private final Long roomId;
    private final String loginId;

    public static RoomPrincipal of(Long roomId, String loginId) {
        return new RoomPrincipal(roomId, loginId);
    }
}
