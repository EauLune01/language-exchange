'use strict';

/*
 * 질문 시트 (방의 두 언어 토글)
 * index.html, topics.html, history.html 에서 common.js 다음에 불러옵니다.
 */
const sheetEls = {
    dialog: $('questions-dialog'),
    titleKo: $('dialog-ko'),
    titleJa: $('dialog-ja'),
    close: $('dialog-close'),
    langToggle: $('lang-toggle'),
    questionArea: $('question-area'),
};

const sheetState = {
    topic: null,
    lang: readSavedLang(),
    token: 0,
};

const questionCache = new Map();
const SLOTS = ['a', 'b'];

function syncToggle() {
    sheetEls.langToggle.querySelectorAll('button[data-lang]').forEach((button) => {
        button.setAttribute('aria-pressed', String(button.dataset.lang === sheetState.lang));
    });
}

function renderQuestionStatus(message) {
    const status = document.createElement('p');
    status.className = 'question-status';
    status.append(...bilingualLines(message));
    sheetEls.questionArea.replaceChildren(status);
}

function renderQuestionError(message) {
    const box = document.createElement('div');
    box.className = 'message';
    box.append(...bilingualLines(message));

    const retry = document.createElement('button');
    retry.type = 'button';
    retry.className = 'secondary';
    retry.append(
        textEl('span', '', MESSAGES.retry.ko, 'ko'),
        document.createTextNode(' / '),
        textEl('span', '', MESSAGES.retry.ja, 'ja')
    );
    retry.addEventListener('click', loadQuestions);

    sheetEls.questionArea.replaceChildren(box, retry);
}

/** 토글 두 칸을 이 주제의 두 언어로 맞춥니다. 색은 언어가 아니라 자리(A = 첫 번째, B = 두 번째)에 붙어요. */
function configureToggle(names) {
    const buttons = sheetEls.langToggle.querySelectorAll('button');
    names.forEach((name, index) => {
        const meta = LANGUAGES[name.lang];
        const button = buttons[index];
        button.dataset.lang = name.lang;
        button.dataset.slot = SLOTS[index];
        button.lang = meta.tag;
        button.textContent = meta.name;
    });
}

function slotOf(lang) {
    const button = sheetEls.langToggle.querySelector(`button[data-lang="${lang}"]`);
    return button ? button.dataset.slot : SLOTS[0];
}

function renderQuestions(data, lang) {
    if (!data.questions || data.questions.length === 0) {
        renderQuestionStatus(MESSAGES.noQuestions);
        return;
    }

    const list = document.createElement('ol');
    list.className = 'question-list';
    list.dataset.slot = slotOf(lang);

    data.questions.forEach((question) => {
        const item = document.createElement('li');
        item.className = 'question-item';
        item.append(
            textEl('span', 'question-no', String(question.sequence)),
            localizedEl('span', 'question-text', { lang, text: question.content })
        );
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

    renderQuestionStatus(MESSAGES.loading);

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
        renderQuestionError(errorMessage(error, 'notFound'));
    }
}

/** 주제의 질문 시트를 엽니다. topic = { id, names: [{ lang, text }, { lang, text }] } (A, B 순서) */
function openQuestions(topic) {
    sheetState.topic = topic;
    setLocalizedText(sheetEls.titleKo, topic.names[0]);
    setLocalizedText(sheetEls.titleJa, topic.names[1]);
    configureToggle(topic.names);
    // 마지막으로 고른 언어가 이 방의 언어가 아니면 첫 번째 언어로 시작합니다.
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