package language.exchange.auth.dto.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginCommand {

    private final String loginId;
    private final String password;

    public static LoginCommand of(String loginId, String password) {
        return new LoginCommand(loginId, password);
    }
}
