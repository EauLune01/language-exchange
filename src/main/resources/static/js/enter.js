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
        petPreview: $('pet-preview'),
        petPreviewEmoji: $('pet-preview-emoji'),
        petPreviewName: $('pet-preview-name'),
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

    /**
     * config = {
     *   path, payload(fields),
     *   validate?(fields) → 사전 키 또는 null,
     *   errorKeys?: { 서버 errorCode: 이 화면에서 대신 보여줄 사전 키 }
     * }
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
                showI18nMessage(message, 'common.emptyFields');
                firstInvalid.focus();
                return;
            }
            const invalidKey = config.validate ? config.validate(form.elements) : null;
            if (invalidKey) {
                showI18nMessage(message, invalidKey);
                return;
            }

            submitButton.disabled = true;
            try {
                await apiPost(config.path, config.payload(form.elements));
                window.location.replace('index.html');
            } catch (error) {
                showApiError(message, error, config.errorKeys);
                submitButton.disabled = false;
            }
        });
    }

    /** 배우고 싶은 언어 목록: 화면 언어와 상관없이 각 언어를 그 언어의 이름으로 보여줍니다. */
    function fillLanguageSelects() {
        const fields = els.createForm.elements;
        [fields.learningLanguageA, fields.learningLanguageB].forEach((select) => {
            const placeholder = new Option(t('enter.create.chooseLanguage'), ''); // 기본값 없이 직접 고르게 합니다.
            placeholder.dataset.i18n = 'enter.create.chooseLanguage';
            select.append(placeholder);
            Object.entries(LANGUAGES).forEach(([code, meta]) => {
                const option = new Option(meta.name, code);
                option.lang = meta.tag;
                select.append(option);
            });
            select.addEventListener('change', syncCreateButton);
            select.addEventListener('change', syncPetPreview);
        });
        syncCreateButton();
    }

    /** 서로 다른 두 언어를 고르면 그 조합의 펫(pet.js)을 미리 보여줍니다. 이름은 화면 언어를 따라가요. */
    function syncPetPreview() {
        const fields = els.createForm.elements;
        const langA = fields.learningLanguageA.value;
        const langB = fields.learningLanguageB.value;
        els.petPreview.hidden = !langA || !langB || langA === langB;
        if (!els.petPreview.hidden) {
            els.petPreviewEmoji.textContent = getPet(langA, langB).emoji;
            els.petPreviewName.textContent = getPetName(langA, langB, i18nLang);
        }
    }

    /** 두 사람 모두 배우고 싶은 언어를 고르기 전에는 방 만들기 버튼을 누를 수 없어요. */
    function syncCreateButton() {
        const fields = els.createForm.elements;
        els.createForm.querySelector('button[type="submit"]').disabled =
            !(fields.learningLanguageA.value && fields.learningLanguageB.value);
    }

    /** 국적 목록: 나라 이름을 지금 화면 언어로 만들고 그 언어의 순서로 정렬합니다. 화면 언어가 바뀌면 다시 불러요. */
    function fillNationalitySelects() {
        const locale = i18nLocale();
        const regionNames = new Intl.DisplayNames([locale], { type: 'region' });
        const collator = new Intl.Collator(locale);
        const countries = COUNTRY_CODES
            .map((code) => ({ code, name: regionNames.of(code) }))
            .sort((a, b) => collator.compare(a.name, b.name));

        const fields = els.createForm.elements;
        [fields.nationalityA, fields.nationalityB].forEach((select) => {
            const selected = select.value;
            const placeholder = new Option(t('enter.create.choose'), ''); // 고르지 않으면 빈 칸으로 처리돼요.
            placeholder.dataset.i18n = 'enter.create.choose';
            select.replaceChildren(placeholder, ...countries.map((country) => new Option(country.name, country.code)));
            select.value = selected;
        });
    }

    bindForm(els.loginForm, {
        path: '/api/auth/login',
        payload: (fields) => ({
            loginId: fields.loginId.value.trim(),
            password: fields.password.value,
        }),
    });

    bindForm(els.createForm, {
        path: '/api/rooms',
        // 서로 상대의 언어를 배우는 교환이라 두 사람이 같은 언어를 고를 수 없어요. (서버도 같은 규칙으로 막습니다)
        validate: (fields) => (fields.learningLanguageA.value === fields.learningLanguageB.value
            ? 'error.SAME_LANGUAGE'
            : null),
        payload: (fields) => ({
            loginId: fields.loginId.value.trim(),
            password: fields.password.value,
            members: [
                {
                    name: fields.nameA.value.trim(),
                    nationality: fields.nationalityA.value,
                    learningLanguage: fields.learningLanguageA.value,
                },
                {
                    name: fields.nameB.value.trim(),
                    nationality: fields.nationalityB.value,
                    learningLanguage: fields.learningLanguageB.value,
                },
            ],
            goal: Number(fields.goal.value),
            useDefaultTopics: fields.useDefaultTopics.checked,
        }),
        // 이 화면에서 형식 오류는 방 아이디·비밀번호 규칙을 어긴 경우라서 규칙을 알려 줍니다.
        errorKeys: { INVALID_INPUT: 'enter.error.invalidRoomInput' },
    });

    els.modeToggle.addEventListener('click', (event) => {
        const button = event.target.closest('button[data-mode]');
        if (button) {
            setMode(button.dataset.mode);
        }
    });

    // 언어 목록과 화면 언어가 준비된 뒤에 선택 칸을 채웁니다.
    i18nReady.then(() => {
        fillLanguageSelects();
        fillNationalitySelects();
        document.addEventListener('i18n:change', fillNationalitySelects);
        document.addEventListener('i18n:change', syncPetPreview);
    });

    // 이미 로그인한 상태면 바로 홈으로 보냅니다.
    apiGet('/api/rooms')
        .then(() => window.location.replace('index.html'))
        .catch(() => { /* 로그인 전이면 이 화면에 그대로 있습니다. */ });
})();
