'use strict';

/*
 * 지원 언어 메타데이터 — 언어 정보는 여기 한 곳에서만 관리합니다.
 *  - 키: 서버 Language enum 이름
 *  - name: 그 언어로 쓴 이름, tag: HTML lang 속성 값, dir: 글 방향
 *  - ui: 화면 문구 사전(js/i18n/<키 소문자>.js)이 있어서 화면 언어로 고를 수 있는지
 *  - motif: 그 언어의 문화를 담은 장식 그림(SVG) 경로, accent: 그 그림에 어울리는 보조 색 (배경 장식에 씁니다)
 * 언어를 추가할 때는 여기에 한 줄과 모티프 SVG 하나, 화면 언어로도 쓰려면 사전 파일 하나를 더합니다.
 * 다른 코드에서 언어 코드로 분기하지 않아요.
 *
 * 모티프: KO 무궁화, JA 벚꽃, EN 튜더 로즈, ZH 모란, ES 카네이션, FR 아이리스,
 *        AR 재스민과 여덟 꼭지 별 문양, VI 연꽃, TH 라차프르욱(황금비 나무 꽃), IT 흰 백합
 */
const LANGUAGES = {
    KO: { name: '한국어', tag: 'ko', dir: 'ltr', ui: true, motif: 'img/motif/ko.svg', accent: '#7d9ee6' },
    JA: { name: '日本語', tag: 'ja', dir: 'ltr', ui: true, motif: 'img/motif/ja.svg', accent: '#ee8fa0' },
    EN: { name: 'English', tag: 'en', dir: 'ltr', ui: true, motif: 'img/motif/en.svg', accent: '#d9737f' },
    ZH: { name: '中文', tag: 'zh-Hans', dir: 'ltr', ui: true, motif: 'img/motif/zh.svg', accent: '#e0708f' },
    ES: { name: 'Español', tag: 'es', dir: 'ltr', ui: true, motif: 'img/motif/es.svg', accent: '#d95f63' },
    FR: { name: 'Français', tag: 'fr', dir: 'ltr', ui: true, motif: 'img/motif/fr.svg', accent: '#8f7fd4' },
    AR: { name: 'العربية', tag: 'ar', dir: 'rtl', ui: true, motif: 'img/motif/ar.svg', accent: '#5fb3a1' },
    VI: { name: 'Tiếng Việt', tag: 'vi', dir: 'ltr', ui: true, motif: 'img/motif/vi.svg', accent: '#e889ab' },
    TH: { name: 'ไทย', tag: 'th', dir: 'ltr', ui: true, motif: 'img/motif/th.svg', accent: '#e3b93c' },
    IT: { name: 'Italiano', tag: 'it', dir: 'ltr', ui: true, motif: 'img/motif/it.svg', accent: '#a9c48b' },
};
