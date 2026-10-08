'use strict';

/*
 * 모든 페이지가 함께 쓰는 공통 코드
 *  - 서버 통신(GET/POST), 안내 메시지, 날짜 표시
 *  - 로그인 확인: 로그인이 안 돼 있으면(401) enter.html로 보내요.
 *  - 화면 언어: languages.js, i18n.js, 사전을 불러오고 헤더에 화면 언어 선택과 로그아웃 버튼을 넣어요.
 *
 * 같은 서버(Spring Boot의 static 폴더)에서 열 때는 API_BASE를 빈 문자열로 두세요.
 * 프론트 파일을 따로 열 때만 'http://localhost:8080' 으로 바꾸고, 서버에 CORS 설정을 추가해야 해요.
 */
const API_BASE = '';
const LANG_STORAGE_KEY = 'exchange:lang';
const ENTER_PAGE = 'enter.html';

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
    emptyHistory: {
        ko: '아직 학습 기록이 없어요. 이번 주 주제를 뽑아보세요.',
        ja: 'まだ学習の記録がありません。今週のテーマを引いてみましょう。',
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
    emptyFields: {
        ko: '비어 있는 칸이 있어요. 모든 칸을 채워 주세요.',
        ja: '空欄があります。すべての欄を入力してください。',
    },
    invalidInput: {
        ko: '서버가 입력 내용을 받아주지 않았어요. 주제 이름과 질문 3개를 다시 확인해 주세요.',
        ja: 'サーバーが入力内容を受け付けませんでした。テーマ名と質問3つをもう一度確認してください。',
    },
};

class ApiError extends Error {
    constructor(status, message, errorCode) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.errorCode = errorCode || null; // 서버 ErrorCode 이름 (예: DUPLICATE_ROOM_ID)
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

/* ---------- 날짜 표시 ('2026-10-07' 형태의 문자열을 받아요) ---------- */

function splitDate(isoDate) {
    const [year, month, day] = isoDate.split('-').map(Number);
    return { year, month, day };
}

function formatShortDate(isoDate) {
    const { month, day } = splitDate(isoDate);
    return `${month}/${day}`;
}

function formatDateKo(isoDate) {
    const { month, day } = splitDate(isoDate);
    return `${month}월 ${day}일`;
}

function formatDateJa(isoDate) {
    const { month, day } = splitDate(isoDate);
    return `${month}月${day}日`;
}

/** 오늘이 속한 주(월~일)에 해당하는 날짜인지 */
function isThisWeek(isoDate) {
    const { year, month, day } = splitDate(isoDate);
    const target = new Date(year, month - 1, day);

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const daysSinceMonday = (today.getDay() + 6) % 7;

    const monday = new Date(today);
    monday.setDate(today.getDate() - daysSinceMonday);
    const nextMonday = new Date(monday);
    nextMonday.setDate(monday.getDate() + 7);

    return target >= monday && target < nextMonday;
}

async function apiRequest(path, options) {
    let response;
    try {
        response = await fetch(API_BASE + path, options);
    } catch (networkError) {
        throw new ApiError(0, 'network');
    }

    // 로그인이 안 돼 있으면 들어가기 화면으로 보냅니다. (들어가기 화면의 로그인 실패 401은 그 화면이 직접 처리)
    if (response.status === 401 && !window.location.pathname.endsWith(ENTER_PAGE)) {
        window.location.replace(ENTER_PAGE);
        return new Promise(() => {}); // 이동하는 동안 오류 메시지가 깜빡이지 않게 끝나지 않는 약속을 돌려줍니다.
    }

    let body = null;
    try {
        body = await response.json();
    } catch (parseError) {
        /* 본문이 JSON이 아니면 아래에서 오류로 처리합니다. */
    }

    if (!response.ok || !body || body.success !== true) {
        throw new ApiError(response.status, body && body.message ? body.message : '', body && body.errorCode);
    }
    return body.data;
}

function apiGet(path) {
    return apiRequest(path, { headers: { Accept: 'application/json' } });
}

function apiPost(path, payload) {
    return apiRequest(path, {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
}

/* ---------- 화면 언어 · 공통 헤더 · 로그인한 방 ---------- */

function loadScript(src) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = () => reject(new Error(`failed to load ${src}`));
        document.head.append(script);
    });
}

/** 화면 언어로 메시지를 보여줍니다. data-i18n 을 달아 두어서 화면 언어를 바꾸면 같이 바뀌어요. */
function showI18nMessage(target, key) {
    target.className = 'message';
    target.dataset.i18n = key;
    target.textContent = t(key);
    target.hidden = false;
}

/**
 * 서버 오류를 화면 언어로 보여줍니다: error.<errorCode> 키 → 없으면 서버 message → 그것도 없으면 일반 문구.
 * overrides = { 서버 errorCode: 이 화면에서 대신 쓸 사전 키 }
 */
function showApiError(target, error, overrides) {
    const isApiError = error instanceof ApiError;
    if (isApiError && error.status === 0) {
        showI18nMessage(target, 'error.network');
        return;
    }

    const errorCode = isApiError ? error.errorCode : null;
    const key = (overrides && overrides[errorCode]) || `error.${errorCode}`;
    if (errorCode && i18nHas(key)) {
        showI18nMessage(target, key);
    } else if (isApiError && error.message) {
        target.className = 'message';
        delete target.dataset.i18n;
        target.textContent = error.message;
        target.hidden = false;
    } else {
        showI18nMessage(target, 'error.generic');
    }
}

function addLanguageSelect(header, nav) {
    const select = document.createElement('select');
    select.className = 'lang-select';
    select.setAttribute('data-i18n-aria-label', 'header.language');
    i18nUiLanguages().forEach((code) => {
        const option = new Option(LANGUAGES[code].name, code);
        option.lang = LANGUAGES[code].tag;
        select.append(option);
    });
    select.value = i18nLang;
    select.addEventListener('change', () => i18nSetLanguage(select.value, true));

    if (nav) {
        nav.before(select);
    } else {
        header.append(select);
    }
}

function addLogoutButton(nav) {
    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.i18n = 'header.logout';
    button.addEventListener('click', async () => {
        try {
            await apiPost('/api/auth/logout');
        } catch (error) {
            /* 실패해도 들어가기 화면으로 보냅니다. */
        }
        window.location.replace(ENTER_PAGE);
    });
    nav.append(button);
}

const siteHeader = document.querySelector('.site-header');
const siteNav = document.querySelector('.site-nav');

/*
 * 화면 언어 준비: 언어 목록 → 엔진 → 사전 순서로 불러온 뒤, 모든 페이지 헤더에 화면 언어 선택을,
 * 메뉴가 있는 페이지(= 로그인이 필요한 페이지)에는 로그아웃 버튼을 넣습니다.
 * LANGUAGES 와 t() 는 이 약속이 끝난 뒤에 쓸 수 있어요.
 */
const i18nReady = (async () => {
    await loadScript('js/languages.js');
    await loadScript('js/i18n.js');
    await i18nInit();
    if (siteHeader) {
        addLanguageSelect(siteHeader, siteNav);
    }
    if (siteNav) {
        addLogoutButton(siteNav);
    }
    i18nApply();
})();

/*
 * 메뉴가 있는 페이지에서는 열자마자 방 정보를 한 번 읽습니다.
 * roomReady 는 { loginId, members: [{ name, nationality, language }, …] } 로 풀리고, 읽지 못하면 null 이에요.
 */
const roomReady = siteNav ? apiGet('/api/rooms').catch(() => null) : Promise.resolve(null);
