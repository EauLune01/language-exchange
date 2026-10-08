/* 주제 등록 페이지: 하나씩 / 여러 개씩 */
(function () {
    'use strict';

    const QUESTION_COUNT = 3; // 서버 규칙: 주제 1개당 질문 3개
    const MAX_TOPICS = 20; // 한 번에 등록할 수 있는 주제 수 (화면에서의 제한)
    const SLOTS = ['a', 'b']; // 왼쪽 칸 = 방의 첫 번째 언어(A), 오른쪽 칸 = 두 번째 언어(B)

    const els = {
        form: $('register-form'),
        modeToggle: $('mode-toggle'),
        topicForms: $('topic-forms'),
        template: $('topic-template'),
        addButton: $('add-topic'),
        submitButton: $('submit-button'),
        submitLabel: $('submit-label'),
        message: $('form-message'),
        success: $('form-success'),
    };

    const state = {
        mode: 'single',
        submitting: false,
        languages: null, // 방의 두 언어 코드 [A, B]. 방 정보를 읽은 뒤에 채워져요.
    };

    /* ---------- 입력 칸 도우미 ---------- */

    function allCards() {
        return [...els.topicForms.children];
    }

    /** 지금 화면에서 실제로 등록 대상인 칸들 (하나씩 모드에서는 첫 번째만) */
    function activeCards() {
        const cards = allCards();
        return state.mode === 'single' ? cards.slice(0, 1) : cards;
    }

    function inputsOf(card) {
        return [...card.querySelectorAll('[data-field]')];
    }

    function fieldOf(card, field, questionIndex) {
        const selector = questionIndex === undefined
            ? `[data-field="${field}"]`
            : `[data-field="${field}"][data-question="${questionIndex}"]`;
        return card.querySelector(selector);
    }

    // 질문은 한 줄이라서 줄바꿈은 공백으로 바꾸고 앞뒤 공백을 지웁니다.
    function clean(value) {
        return value.replace(/\s*\n\s*/g, ' ').trim();
    }

    /** 서버에는 방의 두 언어로 { lang, text } 를 보냅니다. */
    function localizedPair(textA, textB) {
        return [
            { lang: state.languages[0], text: clean(textA) },
            { lang: state.languages[1], text: clean(textB) },
        ];
    }

    function buildPayload(card) {
        const questions = [];
        for (let i = 0; i < QUESTION_COUNT; i += 1) {
            questions.push({
                contents: localizedPair(fieldOf(card, 'contentA', i).value, fieldOf(card, 'contentB', i).value),
            });
        }
        return {
            names: localizedPair(fieldOf(card, 'nameA').value, fieldOf(card, 'nameB').value),
            questions,
        };
    }

    /** 칸 위의 언어 이름과 입력 칸의 lang/dir 을 방의 두 언어로 맞춥니다. */
    function applyRoomLanguages(card) {
        if (!state.languages) {
            return;
        }
        SLOTS.forEach((slot, index) => {
            const meta = LANGUAGES[state.languages[index]];
            card.querySelectorAll(`.field-label--${slot}`).forEach((label) => {
                label.textContent = meta.name;
                label.lang = meta.tag;
            });
            card.querySelectorAll(`.field-input--${slot}`).forEach((input) => {
                input.lang = meta.tag;
                input.dir = meta.dir;
            });
        });
    }

    function hasContent(card) {
        return inputsOf(card).some((input) => clean(input.value) !== '');
    }

    function clearCard(card) {
        inputsOf(card).forEach((input) => {
            input.value = '';
            input.removeAttribute('aria-invalid');
        });
    }

    /** 비어 있는 칸에 표시를 하고, 첫 번째 빈 칸을 돌려줍니다. 없으면 null */
    function markEmptyFields(cards) {
        let firstInvalid = null;
        cards.forEach((card) => {
            inputsOf(card).forEach((input) => {
                if (clean(input.value) === '') {
                    input.setAttribute('aria-invalid', 'true');
                    if (!firstInvalid) {
                        firstInvalid = input;
                    }
                } else {
                    input.removeAttribute('aria-invalid');
                }
            });
        });
        return firstInvalid;
    }

    /* ---------- 화면 갱신 ---------- */

    function refresh() {
        const cards = allCards();
        const isBulk = state.mode === 'bulk';

        els.form.classList.toggle('is-bulk', isBulk);

        cards.forEach((card, index) => {
            card.hidden = !isBulk && index > 0;
            i18nSet(card.querySelector('.topic-form-title'), 'register.topicNo', { no: index + 1 });
            card.querySelector('.topic-remove').hidden = cards.length <= 1;
        });

        els.modeToggle.querySelectorAll('button[data-mode]').forEach((button) => {
            button.setAttribute('aria-pressed', String(button.dataset.mode === state.mode));
        });

        els.addButton.hidden = !isBulk;
        els.addButton.disabled = cards.length >= MAX_TOPICS;

        if (isBulk) {
            i18nSet(els.submitLabel, 'register.submit.bulk', { count: activeCards().length });
        } else {
            i18nSet(els.submitLabel, 'register.submit.single');
        }
    }

    function hideMessages() {
        hideMessage(els.message);
        hideMessage(els.success);
    }

    function createCard() {
        const card = els.template.content.firstElementChild.cloneNode(true);
        card.querySelector('.topic-remove').addEventListener('click', () => removeCard(card));
        i18nApply(card); // <template> 에서 복사한 조각은 문서 밖에 있었으므로 문구를 직접 채웁니다.
        applyRoomLanguages(card);
        return card;
    }

    function addCard() {
        if (allCards().length >= MAX_TOPICS) {
            return;
        }
        const card = createCard();
        els.topicForms.append(card);
        refresh();
        fieldOf(card, 'nameA').focus();
        card.scrollIntoView({ behavior: prefersReducedMotion() ? 'auto' : 'smooth', block: 'center' });
    }

    function removeCard(card) {
        if (allCards().length <= 1) {
            return;
        }
        if (hasContent(card) && !window.confirm(t('register.removeConfirm'))) {
            return;
        }
        card.remove();
        refresh();
    }

    function setMode(mode) {
        if (mode === state.mode) {
            return;
        }
        state.mode = mode;
        hideMessages();
        refresh();
    }

    /* ---------- 전송 ---------- */

    function showSuccess(count) {
        const link = i18nEl('a', 'inline-link', 'register.success.link');
        link.href = 'topics.html';

        els.success.replaceChildren(i18nEl('span', '', 'register.success', { count }), link);
        els.success.hidden = false;
        els.success.scrollIntoView({ behavior: prefersReducedMotion() ? 'auto' : 'smooth', block: 'nearest' });
    }

    function resetAfterSuccess() {
        if (state.mode === 'single') {
            clearCard(allCards()[0]);
            return;
        }
        els.topicForms.replaceChildren(createCard());
        refresh();
    }

    async function submit(event) {
        event.preventDefault();
        if (state.submitting) {
            return;
        }
        hideMessages();

        if (!state.languages) {
            showI18nMessage(els.message, 'error.generic'); // 방 정보를 아직 못 읽었어요.
            return;
        }

        const cards = activeCards();
        const firstInvalid = markEmptyFields(cards);
        if (firstInvalid) {
            showI18nMessage(els.message, 'common.emptyFields');
            firstInvalid.focus();
            return;
        }

        state.submitting = true;
        els.submitButton.disabled = true;

        try {
            if (state.mode === 'single') {
                await apiPost('/api/topics', buildPayload(cards[0]));
            } else {
                await apiPost('/api/topics/bulk-create', { topics: cards.map(buildPayload) });
            }
            showSuccess(cards.length);
            resetAfterSuccess();
        } catch (error) {
            // 이 화면에서 형식 오류는 주제 이름·질문을 다시 확인하라고 알려 줍니다.
            showApiError(els.message, error, { INVALID_INPUT: 'register.error.invalid' });
        } finally {
            state.submitting = false;
            els.submitButton.disabled = false;
        }
    }

    /* ---------- 이벤트 연결 ---------- */

    els.modeToggle.addEventListener('click', (event) => {
        const button = event.target.closest('button[data-mode]');
        if (button) {
            setMode(button.dataset.mode);
        }
    });

    els.addButton.addEventListener('click', addCard);
    els.form.addEventListener('submit', submit);

    // 다시 입력하기 시작하면 그 칸의 빨간 표시를 지웁니다.
    els.topicForms.addEventListener('input', (event) => {
        event.target.removeAttribute('aria-invalid');
    });

    // 사전이 준비되면 첫 입력 칸을 만들고, 방 정보를 읽으면 칸 위에 방의 두 언어 이름을 붙입니다.
    i18nReady.then(() => {
        els.topicForms.append(createCard());
        refresh();
    });

    Promise.all([i18nReady, roomReady]).then(([, room]) => {
        if (!room) {
            return;
        }
        state.languages = room.members.map((member) => member.language);
        allCards().forEach(applyRoomLanguages);
    });
})();
