package language.exchange.study.note.repository;

import language.exchange.study.note.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByQuestionIdAndRoomIdAndLang(Long questionId, Long roomId, String lang);
}
