package language.exchange.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import language.exchange.room.domain.Language;
import language.exchange.study.dto.command.LocalizedTextCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 언어가 들어가는 값의 공통 형태: {lang: "KO", text: "공원"} */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalizedTextRequest {

    @NotNull(message = "언어는 필수입니다.")
    private Language lang;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 255, message = "내용은 255자 이하여야 합니다.")
    private String text;

    public LocalizedTextCommand toCommand() {
        return LocalizedTextCommand.of(lang, text);
    }
}
