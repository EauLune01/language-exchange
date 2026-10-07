'use strict';

/*
 * 두 페이지(index.html, topics.html)가 함께 쓰는 공통 코드
 *  - 서버 통신, 안내 메시지
 *  - 질문 시트 (한국어 / 일본어 토글)
 *
 * 같은 서버(Spring Boot의 static 폴더)에서 열 때는 API_BASE를 빈 문자열로 두세요.
 * 프론트 파일을 따로 열 때만 'http://localhost:8080' 으로 바꾸고, 서버에 CORS 설정을 추가해야 해요.
 */
const API_BASE = '';
const LANG_STORAGE_KEY = 'exchange:lang';

const MESSAGES = {
    noTopic: {
        ko: '남은 주제가 없어요. 새 주제를 등록해 주세요.',
        ja: '残りのテーマがありません。新しいテーマを登録してください。',
    },
    notFound: {
        ko: '주제를 찾을 수 없어요.',
        ja: 'テーマが見つかりません。',
    },
    network: {
        ko: '서버에 연결할 수 없어요. 서버가 켜져 있는지 확인해 주세요.',
        ja: 'サーバーに接続できません。サーバーが起動しているか確認してください。',
    },
    generic: {
        ko: '문제가 생겼어요. 잠시 후 다시 시도해 주세요.',
        ja: '問題が発生しました。しばらくしてからもう一度お試しください。',
    },
    emptyTopics: {
        ko: '아직 등록된 주제가 없어요.',
        ja: 'まだテーマが登録されていません。',
    },
    noQuestions: {
        ko: '이 주제에는 질문이 없어요.',
        ja: 'このテーマには質問がありません。',
    },
    loading: {
        ko: '불러오는 중이에요…',
        ja: '読み込み中…',
    },
    retry: {
        ko: '다시 시도',
        ja: 'もう一度',
    },
};

class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

const $ = (id) => document.getElementById(id);

/* ---------- 공통 도구 ---------- */

function readSavedLang() {
    try {
        return localStorage.getItem(LANG_STORAGE_KEY) === 'JA' ? 'JA' : 'KO';
    } catch (storageError) {
        return 'KO';
    }
}

function saveLang(lang) {
    try {
        localStorage.setItem(LANG_STORAGE_KEY, lang);
    } catch (storageError) {
        /* 저장이 막힌 브라우저에서는 그냥 넘어갑니다. */
    }
}

function prefersReducedMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function textEl(tag, className, text, lang) {
    const el = document.createElement(tag);
    if (className) {
        el.className = className;
    }
    if (lang) {
        el.lang = lang;
    }
    el.textContent = text;
    return el;
}

function bilingualLines(message) {
    return [textEl('span', '', message.ko, 'ko'), textEl('span', '', message.ja, 'ja')];
}

function showMessage(target, message, tone) {
    target.className = tone === 'info' ? 'message message--info' : 'message';
    target.replaceChildren(...bilingualLines(message));
    target.hidden = false;
}

function hideMessage(target) {
    target.hidden = true;
    target.replaceChildren();
}

function errorMessage(error, notFoundKey) {
    if (error instanceof ApiError) {
        if (error.status === 0) {
            return MESSAGES.network;
        }
        if (error.status === 404 && notFoundKey) {
            return MESSAGES[notFoundKey];
        }
    }
    return MESSAGES.generic;
}

function formatShortDate(isoDate) {
    const [, month, day] = isoDate.split('-');
    return `${Number(month)}/${Number(day)}`;
}

async function apiGet(path) {
    let response;
    try {
        response = await fetch(API_BASE + path, { headers: { Accept: 'application/json' } });
    } catch (networkError) {
        throw new ApiError(0, 'network');
    }

    let body = null;
    try {
        body = await response.json();
    } catch (parseError) {
        /* 본문이 JSON이 아니면 아래에서 오류로 처리합니다. */
    }

    if (!response.ok || !body || body.success !== true) {
        throw new ApiError(response.status, body && body.message ? body.message : 'request failed');
    }
    return body.data;
}

/* ---------- 질문 시트 (한국어 / 일본어 토글) ---------- */

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