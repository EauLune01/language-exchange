package language.exchange.room.service;

import language.exchange.global.exception.BusinessException;
import language.exchange.global.exception.ErrorCode;
import language.exchange.room.domain.Language;
import language.exchange.room.domain.Room;
import language.exchange.room.dto.command.RoomCreateCommand;
import language.exchange.room.dto.command.RoomMemberCommand;
import language.exchange.room.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final RoomService roomService = new RoomService(roomRepository, passwordEncoder);

    private RoomCreateCommand command(Language languageA, Language languageB) {
        return command(languageA, languageB, "JP");
    }

    private RoomCreateCommand command(Language languageA, Language languageB, String nationalityB) {
        return RoomCreateCommand.of("our-room", "password123",
                RoomMemberCommand.of("민수", "KR", languageA),
                RoomMemberCommand.of("ゆい", nationalityB, languageB));
    }

    @Test
    @DisplayName("방을 만들면 비밀번호는 BCrypt 해시로만 저장된다")
    void createRoomStoresPasswordHash() {
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomService.createRoom(command(Language.KO, Language.JA));

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getValue().getLanguageA()).isEqualTo(Language.KO);
        assertThat(saved.getValue().getLanguageB()).isEqualTo(Language.JA);
    }

    @Test
    @DisplayName("이미 있는 방 아이디로는 만들 수 없다")
    void createRoomRejectsDuplicateLoginId() {
        when(roomRepository.existsByLoginId("our-room")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(command(Language.KO, Language.JA)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ROOM_ID);
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("국적이 ISO 국가 코드가 아니면 만들 수 없다")
    void createRoomRejectsInvalidNationality() {
        for (String nationality : new String[]{"日本", "jp", "XX", "JPN"}) {
            assertThatThrownBy(() -> roomService.createRoom(command(Language.KO, Language.JA, nationality)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_NATIONALITY);
        }
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("두 사람의 언어가 같으면 만들 수 없다")
    void createRoomRejectsSameLanguage() {
        assertThatThrownBy(() -> roomService.createRoom(command(Language.KO, Language.KO)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SAME_LANGUAGE);
        verify(roomRepository, never()).save(any());
    }
}
