/* 들어가기 페이지: 로그인 / 방 만들기 */
(function () {
    'use strict';

    // ISO 3166-1 alpha-2 국가 코드. 서버가 검증에 쓰는 Java Locale.getISOCountries() 와 같은 목록이에요.
    // 나라 이름은 여기 두지 않고 브라우저(Intl.DisplayNames)가 화면 언어로 만들어 줍니다.
    const COUNTRY_CODES = ('AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ '
        + 'CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR '
        + 'GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP '
        + 'KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ '
        + 'NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW '
        + 'SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ '
        + 'UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW').split(' ');

    const els = {
        modeToggle: $('mode-toggle'),
        loginForm: $('login-form'),
        createForm: $('create-form'),
    };

    function setMode(mode) {
        els.loginForm.hidden = mode !== 'login';
        els.createForm.hidden = mode !== 'create';
        els.modeToggle.querySelectorAll('button[data-mode]').forEach((button) => {
            button.setAttribute('aria-pressed', String(button.dataset.mode === mode));
        });
    }

    /** 비어 있는 칸에 표시를 하고, 첫 번째 빈 칸을 돌려줍니다. 없으면 null */
    function markEmptyFields(form) {
        let firstInvalid = null;
        form.querySelectorAll('.field-input').forEach((input) => {
            if (input.value.trim() === '') {
                input.setAttribute('aria-invalid', 'true');
                firstInvalid = firstInvalid || input;
            } else {
                input.removeAttribute('aria-invalid');
            }
        });
        return firstInvalid;
    }

    function failureMessage(error, messageKeyByStatus) {
        if (error instanceof ApiError) {
            if (error.status === 0) {
                return MESSAGES.network;
            }
            if (messageKeyByStatus[error.status]) {
                return MESSAGES[messageKeyByStatus[error.status]];
            }
        }
        return MESSAGES.generic;
    }

    /**
     * config = { path, payload(fields), validate?(fields) → MESSAGES 키 또는 null, errors: { 상태코드: MESSAGES 키 } }
     * 성공하면 세션 쿠키가 생기므로 홈으로 이동합니다.
     */
    function bindForm(form, config) {
        const message = form.querySelector('.message');
        const submitButton = form.querySelector('button[type="submit"]');

        form.addEventListener('input', (event) => event.target.removeAttribute('aria-invalid'));

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            hideMessage(message);

            const firstInvalid = markEmptyFields(form);
            if (firstInvalid) {
                showMessage(message, MESSAGES.emptyFields);
                firstInvalid.focus();
                return;
            }
            const invalidKey = config.validate ? config.validate(form.elements) : null;
            if (invalidKey) {
                showMessage(message, MESSAGES[invalidKey]);
                return;
            }

            submitButton.disabled = true;
            try {
                await apiPost(config.path, config.payload(form.elements));
                window.location.replace('index.html');
            } catch (error) {
                showMessage(message, failureMessage(error, config.errors));
                submitButton.disabled = false;
            }
        });
    }

    function fillLanguageSelects() {
        const fields = els.createForm.elements;
        [fields.languageA, fields.languageB].forEach((select, index) => {
            Object.entries(LANGUAGES).forEach(([code, meta]) => {
                const option = new Option(meta.name, code);
                option.lang = meta.tag;
                select.append(option);
            });
            select.selectedIndex = index; // 처음부터 서로 다른 언어가 골라져 있게
        });
    }

    function fillNationalitySelects() {
        // 화면 언어 선택은 4단계에서 생겨요. 그때까지는 브라우저 선호 언어를 화면 언어로 봅니다.
        const locales = navigator.languages;
        const regionNames = new Intl.DisplayNames(locales, { type: 'region' });
        const collator = new Intl.Collator(locales);
        const countries = COUNTRY_CODES
            .map((code) => ({ code, name: regionNames.of(code) }))
            .sort((a, b) => collator.compare(a.name, b.name));

        const fields = els.createForm.elements;
        [fields.nationalityA, fields.nationalityB].forEach((select) => {
            select.append(new Option('—', '')); // 고르지 않으면 빈 칸으로 처리돼요.
            countries.forEach((country) => select.append(new Option(country.name, country.code)));
        });
    }

    bindForm(els.loginForm, {
        path: '/api/auth/login',
        payload: (fields) => ({
            loginId: fields.loginId.value.trim(),
            password: fields.password.value,
        }),
        errors: { 401: 'invalidCredentials' },
    });

    bindForm(els.createForm, {
        path: '/api/rooms',
        validate: (fields) => (fields.languageA.value === fields.languageB.value ? 'sameLanguage' : null),
        payload: (fields) => ({
            loginId: fields.loginId.value.trim(),
            password: fields.password.value,
            members: [
                {
                    name: fields.nameA.value.trim(),
                    nationality: fields.nationalityA.value,
                    language: fields.languageA.value,
                },
                {
                    name: fields.nameB.value.trim(),
                    nationality: fields.nationalityB.value,
                    language: fields.languageB.value,
                },
            ],
        }),
        errors: { 400: 'invalidRoomInput', 409: 'duplicateRoomId' },
    });

    els.modeToggle.addEventListener('click', (event) => {
        const button = event.target.closest('button[data-mode]');
        if (button) {
            setMode(button.dataset.mode);
        }
    });

    fillLanguageSelects();
    fillNationalitySelects();

    // 이미 로그인한 상태면 바로 홈으로 보냅니다.
    apiGet('/api/rooms')
        .then(() => window.location.replace('index.html'))
        .catch(() => { /* 로그인 전이면 이 화면에 그대로 있습니다. */ });
})();
