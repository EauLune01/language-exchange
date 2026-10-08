package language.exchange.room.dto.response;

import language.exchange.room.domain.Language;
import language.exchange.room.dto.result.RoomMemberResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomMemberResponse {

    private String name;
    private String nationality;
    private Language language;

    public static RoomMemberResponse from(RoomMemberResult result) {
        return RoomMemberResponse.builder()
                .name(result.getName())
                .nationality(result.getNationality())
                .language(result.getLanguage())
                .build();
    }
}
