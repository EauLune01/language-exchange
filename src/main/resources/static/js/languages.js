'use strict';

/*
 * 지원 언어 메타데이터 — 언어 정보는 여기 한 곳에서만 관리합니다.
 *  - 키: 서버 Language enum 이름
 *  - name: 그 언어로 쓴 이름, tag: HTML lang 속성 값, dir: 글 방향
 * 언어를 추가할 때는 여기에 한 줄을 더합니다. 다른 코드에서 언어 코드로 분기하지 않아요.
 */
const LANGUAGES = {
    KO: { name: '한국어', tag: 'ko', dir: 'ltr' },
    JA: { name: '日本語', tag: 'ja', dir: 'ltr' },
    EN: { name: 'English', tag: 'en', dir: 'ltr' },
    ZH: { name: '中文', tag: 'zh-Hans', dir: 'ltr' },
    ES: { name: 'Español', tag: 'es', dir: 'ltr' },
    FR: { name: 'Français', tag: 'fr', dir: 'ltr' },
    AR: { name: 'العربية', tag: 'ar', dir: 'rtl' },
    VI: { name: 'Tiếng Việt', tag: 'vi', dir: 'ltr' },
    TH: { name: 'ไทย', tag: 'th', dir: 'ltr' },
    IT: { name: 'Italiano', tag: 'it', dir: 'ltr' },
};
