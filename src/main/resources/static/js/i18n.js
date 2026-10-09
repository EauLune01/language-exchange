'use strict';

/*
 * 화면 문구 엔진 — common.js 가 languages.js 다음에 불러옵니다.
 *
 *  - HTML: <p data-i18n="enter.note"></p> 처럼 키만 쓰고, 문구는 js/i18n/<언어>.js 사전에 둡니다.
 *          속성은 data-i18n-aria-label / data-i18n-placeholder / data-i18n-title 로 씁니다.
 *          문구 안의 {이름} 자리에 넣을 값은 data-i18n-params='{"no": 1}' 로 줍니다.
 *  - JS:   t('register.topicNo', { no: 1 }), 화면에 넣을 때는 i18nSet(el, key, params) / i18nEl(tag, className, key, params)
 *          날짜는 { date: { date: '2026-10-08', style: 'long' } } 처럼 넘기면 화면 언어에 맞게 만들어 줍니다.
 *  - 화면 언어: ① 직접 고른 값(localStorage) → ② 브라우저 선호 언어 중 사전이 있는 첫 언어 → ③ 영어
 *              한 기기에서 한 언어만 보여주고, 방의 언어와는 상관없어요.
 *  - 사전은 지금 언어와 영어(빠진 키 대신 보여줄 언어)만 불러옵니다.
 *  - 언어가 바뀌면 data-i18n 요소는 자동으로 다시 쓰이고, document 에 'i18n:change' 이벤트가 납니다.
 */
const I18N_STORAGE_KEY = 'exchange:ui-lang';
const I18N_FALLBACK = 'EN';
const I18N_ATTRIBUTES = ['aria-label', 'placeholder', 'title'];

// 날짜 모양. 순서와 글자(월/일, 요일)는 Intl.DateTimeFormat 이 화면 언어에 맞게 정해요.
const I18N_DATE_STYLES = {
    short: { month: 'short', day: 'numeric' },
    long: { month: 'long', day: 'numeric' },
    full: { year: 'numeric', month: 'long', day: 'numeric' },
    weekday: { month: 'long', day: 'numeric', weekday: 'short' },
};

const i18nDictionaries = {};
let i18nLang = I18N_FALLBACK;

/** 사전 파일(js/i18n/*.js)이 자기 문구를 등록할 때 부릅니다. */
function i18nRegister(code, dictionary) {
    i18nDictionaries[code] = dictionary;
}

/** 화면 언어로 고를 수 있는 언어 코드들 */
function i18nUiLanguages() {
    return Object.keys(LANGUAGES).filter((code) => LANGUAGES[code].ui);
}

function primarySubtag(tag) {
    return tag.toLowerCase().split('-')[0];
}

function i18nResolveLanguage() {
    const available = i18nUiLanguages();

    let saved = null;
    try {
        saved = localStorage.getItem(I18N_STORAGE_KEY);
    } catch (storageError) {
        /* 저장이 막힌 브라우저에서는 브라우저 언어로 정합니다. */
    }
    if (available.includes(saved)) {
        return saved;
    }

    for (const browserTag of navigator.languages || []) {
        const match = available.find((code) => primarySubtag(LANGUAGES[code].tag) === primarySubtag(browserTag));
        if (match) {
            return match;
        }
    }
    return I18N_FALLBACK;
}

function i18nLookup(key) {
    const current = i18nDictionaries[i18nLang] || {};
    const fallback = i18nDictionaries[I18N_FALLBACK] || {};
    return current[key] ?? fallback[key];
}

function i18nHas(key) {
    return i18nLookup(key) !== undefined;
}

/** 지금 화면 언어의 HTML lang 값 (Intl API 에 넘길 때도 씁니다) */
function i18nLocale() {
    return LANGUAGES[i18nLang].tag;
}

/** '2026-10-08' 을 화면 언어의 날짜로. style 은 I18N_DATE_STYLES 의 이름 */
function i18nFormatDate(isoDate, style) {
    const [year, month, day] = isoDate.split('-').map(Number);
    return new Intl.DateTimeFormat(i18nLocale(), I18N_DATE_STYLES[style] || I18N_DATE_STYLES.long)
        .format(new Date(year, month - 1, day));
}

function i18nParamText(value) {
    return value !== null && typeof value === 'object' ? i18nFormatDate(value.date, value.style) : String(value);
}

/**
 * 키에 해당하는 문구. 지금 언어에 없으면 영어, 영어에도 없으면 키를 그대로 돌려줍니다.
 * params 가 있으면 문구 안의 {이름} 을 값으로 바꿉니다.
 */
function t(key, params) {
    const template = i18nLookup(key) ?? key;
    if (!params) {
        return template;
    }
    return template.replace(/\{(\w+)\}/g, (placeholder, name) => (name in params ? i18nParamText(params[name]) : placeholder));
}

/** 요소에 문구를 넣고 키를 달아 둡니다. 화면 언어를 바꾸면 이 요소도 같이 바뀌어요. */
function i18nSet(el, key, params) {
    el.dataset.i18n = key;
    if (params) {
        el.dataset.i18nParams = JSON.stringify(params);
    } else {
        delete el.dataset.i18nParams;
    }
    el.textContent = t(key, params);
}

function i18nEl(tag, className, key, params) {
    const el = document.createElement(tag);
    if (className) {
        el.className = className;
    }
    i18nSet(el, key, params);
    return el;
}

/** 요소를 더 이상 번역하지 않게 합니다. (사용자 내용이나 서버 문구를 넣기 전에) */
function i18nClear(el) {
    delete el.dataset.i18n;
    delete el.dataset.i18nParams;
}

function i18nLoadDictionary(code) {
    return i18nDictionaries[code] ? Promise.resolve() : loadScript(`js/i18n/${code.toLowerCase()}.js`);
}

/** root 안에서 data-i18n 이 붙은 모든 요소를 지금 화면 언어로 다시 씁니다. (<template> 을 복사한 조각에도 쓸 수 있어요) */
function i18nApply(root) {
    const scope = root || document;
    scope.querySelectorAll('[data-i18n]').forEach((el) => {
        const params = el.dataset.i18nParams ? JSON.parse(el.dataset.i18nParams) : null;
        el.textContent = t(el.dataset.i18n, params);
    });
    I18N_ATTRIBUTES.forEach((attribute) => {
        scope.querySelectorAll(`[data-i18n-${attribute}]`).forEach((el) => {
            el.setAttribute(attribute, t(el.getAttribute(`data-i18n-${attribute}`)));
        });
    });

    const meta = LANGUAGES[i18nLang];
    document.documentElement.lang = meta.tag;
    document.documentElement.dir = meta.dir;
}

/** 화면 언어를 바꿉니다. remember 가 true 면 이 기기에 기억해요. */
async function i18nSetLanguage(code, remember) {
    await i18nLoadDictionary(code);
    i18nLang = code;
    if (remember) {
        try {
            localStorage.setItem(I18N_STORAGE_KEY, code);
        } catch (storageError) {
            /* 저장이 막힌 브라우저에서는 이번 화면에서만 바뀝니다. */
        }
    }
    i18nApply();
    document.dispatchEvent(new CustomEvent('i18n:change'));
}

async function i18nInit() {
    const code = i18nResolveLanguage();
    await i18nLoadDictionary(I18N_FALLBACK);
    try {
        await i18nLoadDictionary(code);
        i18nLang = code;
    } catch (loadError) {
        /* 사전을 못 불러오면 영어로 보여줍니다. */
    }
    i18nApply();
}
