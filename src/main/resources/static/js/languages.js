'use strict';

/*
 * 지원 언어 메타데이터 — 언어 정보는 여기 한 곳에서만 관리합니다.
 *  - 키: 서버 Language enum 이름
 *  - name: 그 언어로 쓴 이름, tag: HTML lang 속성 값, dir: 글 방향
 *  - ui: 화면 문구 사전(js/i18n/<키 소문자>.js)이 있어서 화면 언어로 고를 수 있는지
 * 언어를 추가할 때는 여기에 한 줄, 화면 언어로도 쓰려면 사전 파일 하나를 더합니다.
 * 다른 코드에서 언어 코드로 분기하지 않아요.
 */
const LANGUAGES = {
    KO: { name: '한국어', tag: 'ko', dir: 'ltr', ui: true },
    JA: { name: '日本語', tag: 'ja', dir: 'ltr', ui: true },
    EN: { name: 'English', tag: 'en', dir: 'ltr', ui: true },
    ZH: { name: '中文', tag: 'zh-Hans', dir: 'ltr', ui: true },
    ES: { name: 'Español', tag: 'es', dir: 'ltr', ui: true },
    FR: { name: 'Français', tag: 'fr', dir: 'ltr', ui: true },
    AR: { name: 'العربية', tag: 'ar', dir: 'rtl', ui: true },
    VI: { name: 'Tiếng Việt', tag: 'vi', dir: 'ltr', ui: true },
    TH: { name: 'ไทย', tag: 'th', dir: 'ltr', ui: true },
    IT: { name: 'Italiano', tag: 'it', dir: 'ltr', ui: true },
};
