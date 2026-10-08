'use strict';

/*
 * 모든 페이지가 함께 쓰는 공통 코드
 *  - 서버 통신(GET/POST), 안내 메시지
 *  - 로그인 확인: 로그인이 안 돼 있으면(401) enter.html로 보내요.
 *  - 공통 헤더: 로고, 메뉴, 화면 언어 선택, 로그아웃을 모든 페이지에 그려요. (HTML 에는 빈 <header> 만 둡니다)
 *  - 화면 언어: languages.js, i18n.js, 사전을 불러와요.
 *
 * 같은 서버(Spring Boot의 static 폴더)에서 열 때는 API_BASE를 빈 문자열로 두세요.
 * 프론트 파일을 따로 열 때만 'http://localhost:8080' 으로 바꾸고, 서버에 CORS 설정을 추가해야 해요.
 */
const API_BASE = '';
const LANG_STORAGE_KEY = 'exchange:lang';
const ENTER_PAGE = 'enter.html';

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

// 질문 시트에서 마지막으로 고른 언어는 방별로 기억합니다. (방 정보를 읽으면 키 뒤에 방 아이디가 붙어요)
let sheetLangStorageKey = LANG_STORAGE_KEY;

/** 질문 시트에서 마지막으로 고른 언어 코드. 없으면 null */
function readSavedLang() {
    try {
        return localStorage.getItem(sheetLangStorageKey);
    } catch (storageError) {
        return null;
    }
}

function saveLang(lang) {
    try {
        localStorage.setItem(sheetLangStorageKey, lang);
    } catch (storageError) {
        /* 저장이 막힌 브라우저에서는 그냥 넘어갑니다. */
    }
}

function prefersReducedMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/*
 * 서버가 주는 주제·질문 내용은 { lang: 'KO', text: '공원' } 형태예요.
 * 내용의 실제 언어를 lang/dir 속성으로 달아 줍니다. (LANGUAGES 를 쓰므로 i18nReady 뒤에 불러야 해요)
 */
function setLocalizedText(el, localized) {
    const meta = LANGUAGES[localized.lang];
    i18nClear(el); // 화면 문구가 들어 있던 자리라면 더 이상 번역하지 않게
    el.textContent = localized.text;
    el.lang = meta.tag;
    el.dir = meta.dir;
}

function localizedEl(tag, className, localized) {
    const el = document.createElement(tag);
    el.className = className;
    setLocalizedText(el, localized);
    return el;
}

function hideMessage(target) {
    target.hidden = true;
    target.replaceChildren();
}

/** 오늘이 속한 주(월~일)에 해당하는 날짜인지 ('2026-10-07' 형태의 문자열을 받아요) */
function isThisWeek(isoDate) {
    const [year, month, day] = isoDate.split('-').map(Number);
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

/**
 * 화면 언어로 메시지를 보여줍니다. 키를 달아 두어서 화면 언어를 바꾸면 같이 바뀌어요.
 * tone 이 'info' 면 차분한 안내, 'success' 면 완료 안내, 없으면 오류 모양이에요.
 */
function showI18nMessage(target, key, params, tone) {
    target.className = tone ? `message message--${tone}` : 'message';
    i18nSet(target, key, params);
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
        i18nClear(target);
        target.textContent = error.message;
        target.hidden = false;
    } else {
        showI18nMessage(target, 'error.generic');
    }
}

/* ---------- 공통 헤더: 모든 페이지의 <header class="site-header"> 빈 자리에 그립니다 ---------- */

// [data-nav 값, 주소, 사전 키]. 메뉴를 추가할 때는 여기에 한 줄만 더합니다.
const NAV_ITEMS = [
    ['home', 'index.html', 'nav.home'],
    ['topics', 'topics.html', 'nav.topics'],
    ['history', 'history.html', 'nav.history'],
    ['register', 'register.html', 'nav.register'],
];

function createElement(tag, className, attributes) {
    const el = document.createElement(tag);
    if (className) {
        el.className = className;
    }
    Object.entries(attributes || {}).forEach(([name, value]) => el.setAttribute(name, value));
    return el;
}

function createLogoutButton() {
    const button = createElement('button', '', { type: 'button', 'data-i18n': 'header.logout' });
    button.addEventListener('click', async () => {
        try {
            await apiPost('/api/auth/logout');
        } catch (error) {
            /* 실패해도 들어가기 화면으로 보냅니다. */
        }
        window.location.replace(ENTER_PAGE);
    });
    return button;
}

/**
 * <header class="site-header" data-nav="home"> 처럼 data-nav 가 있으면 로그인이 필요한 페이지라서
 * 메뉴와 로그아웃까지 그리고, 없으면(들어가기 화면) 로고와 화면 언어 선택만 그립니다.
 * 문구는 data-i18n 키만 달아 두고, 사전이 준비되면 i18nApply 가 채워요.
 */
function renderHeader(header) {
    const currentPage = header.dataset.nav;

    const brand = createElement('a', 'brand', { href: currentPage ? 'index.html' : ENTER_PAGE });
    const logo = createElement('span', 'logo', { 'aria-hidden': 'true' });
    logo.append(createElement('span', 'logo-circle logo-circle--a'), createElement('span', 'logo-circle logo-circle--b'));
    brand.append(logo, createElement('span', 'wordmark', { 'data-i18n': 'app.name' }));

    const languageSelect = createElement('select', 'lang-select', { 'data-i18n-aria-label': 'header.language' });
    languageSelect.addEventListener('change', () => i18nSetLanguage(languageSelect.value, true));

    header.replaceChildren(brand, languageSelect);

    if (currentPage) {
        const nav = createElement('nav', 'site-nav', { 'data-i18n-aria-label': 'nav.label' });
        NAV_ITEMS.forEach(([page, href, key]) => {
            const link = createElement('a', '', { href, 'data-i18n': key });
            if (page === currentPage) {
                link.setAttribute('aria-current', 'page');
            }
            nav.append(link);
        });
        nav.append(createLogoutButton());
        header.append(nav);
    }
    return languageSelect;
}

/*
 * 언어별 모티프(장식 그림)를 CSS 변수로 내려줍니다. CSS 는 --motif-a/b(그림)와 --accent-a/b(보조 색)만 보고,
 * 어떤 언어인지는 몰라요. 그림과 색은 languages.js 의 motif, accent 에서 읽습니다.
 *  - codes 가 둘이면 방의 두 언어(A, B): 두 문화가 만나는 방이라 두 그림을 나란히 씁니다.
 *  - codes 가 하나면 화면 언어: 방에 들어가기 전에는 그림 하나만 씁니다.
 */
function applyMotifs(codes) {
    const root = document.documentElement;
    ['a', 'b'].forEach((slot, index) => {
        const meta = LANGUAGES[codes[index] || codes[0]];
        root.style.setProperty(`--motif-${slot}`, `url("${new URL(meta.motif, document.baseURI).href}")`);
        root.style.setProperty(`--accent-${slot}`, meta.accent);
    });
    root.dataset.motifs = codes.length > 1 ? 'pair' : 'single';
}

/** 방의 두 모티프를 나란히 놓은 장식 (빈 상태 화면 등). 장식이라 화면 낭독기에는 숨깁니다. */
function createMotifPair() {
    const pair = createElement('div', 'motif-pair', { 'aria-hidden': 'true' });
    pair.append(createElement('span', 'motif motif--a'), createElement('span', 'motif motif--b'));
    return pair;
}

/** 헤더 아래 진행바: 목표 횟수(goal) 중 지금까지 학습한 횟수(studiedCount). 이름은 사용자 내용이라 키와 값만 달아 둡니다. */
function renderProgress(header, room) {
    const [memberA, memberB] = room.members;
    const journey = createElement('div', 'journey');
    const title = i18nEl('span', 'journey-title', 'progress.title', { nameA: memberA.name, nameB: memberB.name });
    title.id = 'journey-title';
    journey.append(
        title,
        i18nEl('span', 'journey-count', 'progress.count', { count: room.studiedCount, goal: room.goal }),
        // 목표를 넘겨도 <progress> 가 알아서 100% 로 막아 줍니다.
        createElement('progress', 'journey-bar', { max: room.goal, value: room.studiedCount, 'aria-labelledby': 'journey-title' }));
    header.append(journey);
}

/** 화면 언어 선택 칸: 사전이 있는 언어만, 각 언어의 이름으로 보여줍니다. */
function fillLanguageSelect(select) {
    i18nUiLanguages().forEach((code) => {
        const option = new Option(LANGUAGES[code].name, code);
        option.lang = LANGUAGES[code].tag;
        select.append(option);
    });
    select.value = i18nLang;
}

const siteHeader = document.querySelector('.site-header');
const languageSelect = renderHeader(siteHeader);

/*
 * 화면 언어 준비: 언어 목록 → 엔진 → 사전 순서로 불러온 뒤 문구를 채웁니다.
 * LANGUAGES, t(), i18nSet() 등은 이 약속이 끝난 뒤에 쓸 수 있으므로, 각 페이지는 i18nReady.then(...) 안에서 시작해요.
 */
const i18nReady = (async () => {
    await loadScript('js/languages.js');
    await loadScript('js/i18n.js');
    await i18nInit();
    fillLanguageSelect(languageSelect);
})();

/*
 * 메뉴가 있는 페이지(= 로그인이 필요한 페이지)에서는 열자마자 방 정보를 한 번 읽습니다.
 * roomReady 는 { loginId, members: [{ name, nationality, language, learningLanguage }, …], goal, studiedCount } 로 풀리고,
 * 읽지 못하면 null 이에요. 같은 값을 roomInfo 전역에도 담아 둡니다.
 */
let roomInfo = null;
const roomReady = siteHeader.dataset.nav ? apiGet('/api/rooms').catch(() => null) : Promise.resolve(null);

roomReady.then((room) => {
    roomInfo = room;
    if (room) {
        sheetLangStorageKey = `${LANG_STORAGE_KEY}:${room.loginId}`;
    }
});

// 모티프: 방 안에서는 방의 두 언어, 방에 들어가기 전(메뉴가 없는 화면)에는 화면 언어를 따라갑니다.
if (siteHeader.dataset.nav) {
    Promise.all([i18nReady, roomReady]).then(([, room]) => {
        if (room) {
            applyMotifs(room.members.map((member) => member.language));
            renderProgress(siteHeader, room);
        }
    });
} else {
    i18nReady.then(() => {
        applyMotifs([i18nLang]);
        document.addEventListener('i18n:change', () => applyMotifs([i18nLang]));
    });
}
