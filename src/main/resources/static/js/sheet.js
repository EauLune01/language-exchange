'use strict';

/*
 * 질문 시트 (방의 두 언어 토글)
 * index.html, topics.html, history.html 에서 common.js 다음에 불러옵니다.
 * 색은 언어가 아니라 자리에 붙어요: 첫 번째 언어 = A(파랑), 두 번째 언어 = B(빨강)
 */
const sheetEls = {
    dialog: $('questions-dialog'),
    titleA: $('dialog-a'),
    titleB: $('dialog-b'),
    close: $('dialog-close'),
    langToggle: $('lang-toggle'),
    questionArea: $('question-area'),
};

const sheetState = {
    topic: null,
    lang: null, // 처음 열 때 이 방에서 마지막으로 고른 언어를 읽어 옵니다.
    token: 0,
};

const questionCache = new Map();

// 메모는 질문 × 언어마다 하나예요(한국어 질문의 메모와 일본어 질문의 메모는 별개).
// 적던 내용과 펼친 상태는 언어를 바꿔 목록을 다시 그려도 남도록 따로 기억합니다.
const noteDrafts = new Map(); // "questionId:lang" → 적던 내용 (서버에서 읽어 온 값 포함)
const openNotes = new Set(); // 펼쳐 둔 questionId (언어를 바꾸면 같은 질문의 그 언어 메모가 열려요)

const NOTE_MAX_LENGTH = 2000; // 서버 NoteUpsertRequest 의 @Size 와 같은 값
const NOTE_ICON = '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">'
    + '<path d="M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>';

function toggleButton(lang) {
    return sheetEls.langToggle.querySelector(`button[data-lang="${lang}"]`);
}

function syncToggle() {
    sheetEls.langToggle.querySelectorAll('button[data-lang]').forEach((button) => {
        button.setAttribute('aria-pressed', String(button.dataset.lang === sheetState.lang));
    });
}

/** 토글 두 칸(HTML 의 data-slot="a", "b")에 이 주제의 두 언어를 순서대로 넣습니다. */
function configureToggle(names) {
    const buttons = sheetEls.langToggle.querySelectorAll('button');
    names.forEach((name, index) => {
        const meta = LANGUAGES[name.lang];
        buttons[index].dataset.lang = name.lang;
        buttons[index].lang = meta.tag;
        buttons[index].textContent = meta.name;
    });
}

function renderQuestionStatus(key) {
    sheetEls.questionArea.replaceChildren(i18nEl('p', 'question-status', key));
}

function renderQuestionError(error) {
    const box = createElement('div', 'message', { role: 'alert' });
    showApiError(box, error);

    const retry = i18nEl('button', 'secondary', 'sheet.retry');
    retry.type = 'button';
    retry.addEventListener('click', loadQuestions);

    sheetEls.questionArea.replaceChildren(box, retry);
}

/** 질문 하나의 lang 언어 메모 칸. load() 는 펼칠 때 부르고, 이 화면에서 아직 읽은 적이 없을 때만 서버에서 가져옵니다. */
function createNote(questionId, lang) {
    const noteKey = `${questionId}:${lang}`;
    const notePath = `/api/questions/${questionId}/note?lang=${lang}`;
    const el = createElement('div', 'note');
    el.hidden = true;

    const input = createElement('textarea', 'note-input', {
        rows: 3,
        maxlength: NOTE_MAX_LENGTH,
        dir: 'auto',
        placeholder: t('sheet.note-placeholder'),
        'aria-label': t('sheet.note-placeholder'),
        'data-i18n-placeholder': 'sheet.note-placeholder',
        'data-i18n-aria-label': 'sheet.note-placeholder',
    });
    input.disabled = true;

    const status = createElement('span', 'note-status', { role: 'status' });
    const save = i18nEl('button', 'note-save', 'sheet.note-save');
    save.type = 'button';

    const foot = createElement('div', 'note-foot');
    foot.append(status, save);
    el.append(input, foot);

    function setStatus(key, tone) {
        status.className = tone ? `note-status note-status--${tone}` : 'note-status';
        if (key) {
            i18nSet(status, key);
        } else {
            i18nClear(status);
            status.textContent = '';
        }
    }

    function syncSave() {
        save.disabled = input.disabled || !input.value.trim();
    }

    input.addEventListener('input', () => {
        noteDrafts.set(noteKey, input.value);
        setStatus(null);
        syncSave();
    });

    save.addEventListener('click', async () => {
        save.disabled = true;
        setStatus(null);
        try {
            await apiPut(notePath, { content: input.value });
            setStatus('sheet.note-saved', 'saved');
        } catch (error) {
            setStatus('sheet.note-error', 'error');
        }
        syncSave();
    });

    async function load() {
        if (!noteDrafts.has(noteKey)) {
            setStatus('sheet.loading');
            try {
                const data = await apiGet(notePath);
                // 읽어 오는 사이에 다른 칸(언어를 바꿨다 돌아와 다시 그린 같은 질문)에서 적기 시작했으면 그 내용을 지킵니다.
                if (!noteDrafts.has(noteKey)) {
                    noteDrafts.set(noteKey, data.content);
                }
            } catch (error) {
                // 못 읽은 채로 저장하면 원래 메모를 덮어쓰니까 칸을 잠가 둡니다. 닫았다 다시 열면 다시 읽어요.
                setStatus('error.generic', 'error');
                return;
            }
            setStatus(null);
        }
        input.value = noteDrafts.get(noteKey);
        input.disabled = false;
        syncSave();
    }

    return { el, load };
}

function renderQuestions(data, lang) {
    if (!data.questions || data.questions.length === 0) {
        renderQuestionStatus('sheet.empty');
        return;
    }

    const list = createElement('ol', 'question-list', { 'data-slot': toggleButton(lang).dataset.slot });

    data.questions.forEach((question) => {
        const number = createElement('span', 'question-no');
        number.textContent = String(question.sequence);

        const item = createElement('li', 'question-item');
        const text = localizedEl('span', 'question-text', { lang, text: question.content });

        // 메모는 이미 뽑아서 이야기한(사용한) 주제에만 적을 수 있어요. 아직 안 쓴 주제는 질문만 보여줍니다.
        if (!sheetState.topic.used) {
            const row = createElement('div', 'question-row');
            row.append(number, text);
            item.append(row);
            list.append(item);
            return;
        }

        const icon = createElement('span', 'question-note-icon');
        icon.innerHTML = NOTE_ICON;

        // 질문을 누르면 그 아래 메모 칸이 열리고, 다시 누르면 닫힙니다.
        const toggle = createElement('button', 'question-row question-toggle', { type: 'button', 'aria-expanded': 'false' });
        toggle.append(number, text, icon);

        const note = createNote(question.id, lang);
        const setOpen = (open) => {
            note.el.hidden = !open;
            toggle.setAttribute('aria-expanded', String(open));
            if (open) {
                openNotes.add(question.id);
                note.load();
            } else {
                openNotes.delete(question.id);
            }
        };
        toggle.addEventListener('click', () => setOpen(note.el.hidden));
        if (openNotes.has(question.id)) {
            setOpen(true);
        }

        item.append(toggle, note.el);
        list.append(item);
    });

    sheetEls.questionArea.replaceChildren(list);
}

async function loadQuestions() {
    const topic = sheetState.topic;
    if (!topic) {
        return;
    }

    const lang = sheetState.lang;
    const token = ++sheetState.token;
    const cacheKey = `${topic.id}:${lang}`;

    if (questionCache.has(cacheKey)) {
        renderQuestions(questionCache.get(cacheKey), lang);
        return;
    }

    renderQuestionStatus('sheet.loading');

    try {
        const data = await apiGet(`/api/topics/${topic.id}/questions?lang=${lang}`);
        questionCache.set(cacheKey, data);
        if (token !== sheetState.token) {
            return;
        }
        renderQuestions(data, lang);
    } catch (error) {
        if (token !== sheetState.token) {
            return;
        }
        renderQuestionError(error);
    }
}

/**
 * 주제의 질문 시트를 엽니다. topic = { id, names: [{ lang, text }, { lang, text }], used } (names 는 A, B 순서)
 * used 가 true(이미 뽑아서 사용한 주제)일 때만 질문마다 메모 칸이 붙습니다.
 */
function openQuestions(topic) {
    sheetState.topic = topic;
    setLocalizedText(sheetEls.titleA, topic.names[0]);
    setLocalizedText(sheetEls.titleB, topic.names[1]);
    configureToggle(topic.names);
    if (!sheetState.lang) {
        sheetState.lang = readSavedLang();
    }
    // 고른 적이 없거나 이 방의 언어가 아니면 첫 번째 언어로 시작합니다.
    if (!topic.names.some((name) => name.lang === sheetState.lang)) {
        sheetState.lang = topic.names[0].lang;
    }
    syncToggle();
    sheetEls.dialog.querySelector('.sheet').scrollTop = 0;
    sheetEls.dialog.showModal();
    loadQuestions();
}

sheetEls.langToggle.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-lang]');
    if (!button || button.dataset.lang === sheetState.lang) {
        return;
    }
    sheetState.lang = button.dataset.lang;
    saveLang(sheetState.lang);
    syncToggle();
    loadQuestions();
});

sheetEls.close.addEventListener('click', () => sheetEls.dialog.close());

// 어두운 배경(backdrop)을 누르면 닫기
sheetEls.dialog.addEventListener('click', (event) => {
    if (event.target === sheetEls.dialog) {
        sheetEls.dialog.close();
    }
});
