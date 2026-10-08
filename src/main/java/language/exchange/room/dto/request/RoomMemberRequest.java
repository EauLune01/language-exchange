package language.exchange.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import language.exchange.room.domain.Language;
import language.exchange.room.dto.command.RoomMemberCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMemberRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    // ISO 3166-1 alpha-2 국가 코드 (KR, JP …). 실제 코드인지는 서비스에서 확인한다.
    @NotBlank(message = "국적은 필수입니다.")
    private String nationality;

    @NotNull(message = "언어는 필수입니다.")
    private Language language;

    public RoomMemberCommand toCommand() {
        return RoomMemberCommand.of(name, nationality, language);
    }
}
