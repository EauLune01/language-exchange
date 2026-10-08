'use strict';

/*
 * 화면 문구 엔진 — common.js 가 languages.js 다음에 불러옵니다.
 *
 *  - HTML: <p data-i18n="enter.note"></p> 처럼 키만 쓰고, 문구는 js/i18n/<언어>.js 사전에 둡니다.
 *          속성은 data-i18n-aria-label / data-i18n-placeholder / data-i18n-title 로 씁니다.
 *  - JS:   t('enter.note')
 *  - 화면 언어: ① 직접 고른 값(localStorage) → ② 브라우저 선호 언어 중 사전이 있는 첫 언어 → ③ 영어
 *              한 기기에서 한 언어만 보여주고, 방의 언어와는 상관없어요.
 *  - 사전은 지금 언어와 영어(빠진 키 대신 보여줄 언어)만 불러옵니다.
 *  - 언어가 바뀌면 document 에 'i18n:change' 이벤트가 납니다. (JS 로 만든 목록을 다시 그릴 때 사용)
 */
const I18N_STORAGE_KEY = 'exchange:ui-lang';
const I18N_FALLBACK = 'EN';
const I18N_ATTRIBUTES = ['aria-label', 'placeholder', 'title'];

// 아직 data-i18n 으로 옮기지 않은 페이지(한/일 병기)는 <html>의 lang/dir 을 바꾸지 않고,
// common.js 가 넣은 헤더 요소에만 붙입니다. 페이지를 옮기면 자동으로 <html>에 적용돼요.
const I18N_PAGE_TRANSLATED = document.querySelector('[data-i18n]') !== null;

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

/** 키에 해당하는 문구. 지금 언어에 없으면 영어, 영어에도 없으면 키를 그대로 돌려줍니다. */
function t(key) {
    return i18nLookup(key) ?? key;
}

/** 지금 화면 언어의 HTML lang 값 (Intl API 에 넘길 때도 씁니다) */
function i18nLocale() {
    return LANGUAGES[i18nLang].tag;
}

function i18nLoadDictionary(code) {
    return i18nDictionaries[code] ? Promise.resolve() : loadScript(`js/i18n/${code.toLowerCase()}.js`);
}

/** data-i18n 이 붙은 모든 요소를 지금 화면 언어로 다시 씁니다. */
function i18nApply() {
    const translated = document.querySelectorAll('[data-i18n]');
    translated.forEach((el) => {
        el.textContent = t(el.dataset.i18n);
    });
    I18N_ATTRIBUTES.forEach((attribute) => {
        document.querySelectorAll(`[data-i18n-${attribute}]`).forEach((el) => {
            el.setAttribute(attribute, t(el.getAttribute(`data-i18n-${attribute}`)));
        });
    });

    const meta = LANGUAGES[i18nLang];
    const languageHolders = I18N_PAGE_TRANSLATED ? [document.documentElement] : translated;
    languageHolders.forEach((el) => {
        el.lang = meta.tag;
        el.dir = meta.dir;
    });
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
