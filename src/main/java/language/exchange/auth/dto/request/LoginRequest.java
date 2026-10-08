package language.exchange.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import language.exchange.auth.dto.command.LoginCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginRequest {

    @NotBlank(message = "방 아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    public LoginCommand toCommand() {
        return LoginCommand.of(loginId, password);
    }
}
