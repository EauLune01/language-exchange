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
        item.append(number, localizedEl('span', 'question-text', { lang, text: question.content }));
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

/** 주제의 질문 시트를 엽니다. topic = { id, names: [{ lang, text }, { lang, text }] } (A, B 순서) */
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
