package language.exchange.room.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import language.exchange.room.dto.command.RoomCreateCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomCreateRequest {

    @NotNull(message = "방 아이디는 필수입니다.")
    @Pattern(regexp = "^[a-z0-9_-]{4,20}$", message = "방 아이디는 영문 소문자·숫자·-·_ 4~20자여야 합니다.")
    private String loginId;

    // BCrypt는 72바이트까지만 처리하므로 ASCII로 제한
    @NotNull(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^[\\x21-\\x7E]{8,64}$", message = "비밀번호는 영문·숫자·기호 8~64자여야 합니다.")
    private String password;

    // [첫 번째 사람(A), 두 번째 사람(B)]
    @Valid
    @NotNull(message = "두 사람의 정보는 필수입니다.")
    @Size(min = 2, max = 2, message = "두 사람의 정보를 입력해야 합니다.")
    private List<@NotNull RoomMemberRequest> members;

    // 25/50/75/100 중 하나인지는 서비스에서 확인
    @NotNull(message = "목표 횟수는 필수입니다.")
    private Integer goal;

    // true 면 방을 만든 직후 기본 추천 주제를 등록한다 (생략하면 false)
    private boolean useDefaultTopics;

    public RoomCreateCommand toCommand() {
        return RoomCreateCommand.of(loginId, password, members.get(0).toCommand(), members.get(1).toCommand(), goal);
    }
}
