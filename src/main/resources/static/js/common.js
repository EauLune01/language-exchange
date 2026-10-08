'use strict';

/*
 * 모든 페이지가 함께 쓰는 공통 코드
 *  - 서버 통신(GET/POST), 안내 메시지, 날짜 표시
 *  - 로그인 확인: 로그인이 안 돼 있으면(401) enter.html로 보내고, 메뉴에 로그아웃 버튼을 붙여요.
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
    invalidCredentials: {
        ko: '방 아이디 또는 비밀번호가 맞지 않아요.',
        ja: 'ルームIDまたはパスワードが正しくありません。',
    },
    duplicateRoomId: {
        ko: '이미 사용 중인 방 아이디예요. 다른 아이디를 써 주세요.',
        ja: 'このルームIDはすでに使われています。別のIDにしてください。',
    },
    sameLanguage: {
        ko: '두 사람의 모국어는 서로 달라야 해요.',
        ja: 'ふたりの母語は別の言語にしてください。',
    },
    invalidRoomInput: {
        ko: '방 아이디는 영문 소문자·숫자 4~20자, 비밀번호는 영문·숫자·기호 8자 이상으로 적어 주세요.',
        ja: 'ルームIDは英小文字・数字4〜20文字、パスワードは英数字・記号8文字以上で入力してください。',
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
        throw new ApiError(response.status, body && body.message ? body.message : 'request failed');
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

/* ---------- 로그인한 방 ---------- */

function addLogoutButton(nav) {
    const button = document.createElement('button');
    button.type = 'button';
    button.append(textEl('span', '', '로그아웃', 'ko'), textEl('span', '', 'ログアウト', 'ja'));
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

/*
 * 메뉴가 있는 페이지(= 로그인이 필요한 페이지)에서는 열자마자 방 정보를 한 번 읽습니다.
 * roomReady 는 { loginId, members: [{ name, nationality, language }, …] } 로 풀리고, 읽지 못하면 null 이에요.
 */
const siteNav = document.querySelector('.site-nav');
const roomReady = siteNav ? apiGet('/api/rooms').catch(() => null) : Promise.resolve(null);
if (siteNav) {
    addLogoutButton(siteNav);
}
