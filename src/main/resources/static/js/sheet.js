'use strict';

/*
 * 질문 시트 (한국어 / 일본어 토글)
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

function renderQuestions(data, lang) {
    if (!data.questions || data.questions.length === 0) {
        renderQuestionStatus(MESSAGES.noQuestions);
        return;
    }

    const list = document.createElement('ol');
    list.className = 'question-list';
    list.dataset.lang = lang;

    data.questions.forEach((question) => {
        const item = document.createElement('li');
        item.className = 'question-item';
        item.append(
            textEl('span', 'question-no', String(question.sequence)),
            textEl('span', 'question-text', question.content, lang === 'KO' ? 'ko' : 'ja')
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

/** 주제의 질문 시트를 엽니다. topic = { id, nameKo, nameJa } */
function openQuestions(topic) {
    sheetState.topic = topic;
    sheetEls.titleKo.textContent = topic.nameKo;
    sheetEls.titleJa.textContent = topic.nameJa;
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