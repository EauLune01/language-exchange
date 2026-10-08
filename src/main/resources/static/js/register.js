/* 주제 등록 페이지: 하나씩 / 여러 개씩 */
(function () {
    'use strict';

    const QUESTION_COUNT = 3; // 서버 규칙: 주제 1개당 질문 3개
    const MAX_TOPICS = 20; // 한 번에 등록할 수 있는 주제 수 (화면에서의 제한)

    const els = {
        form: $('register-form'),
        modeToggle: $('mode-toggle'),
        topicForms: $('topic-forms'),
        template: $('topic-template'),
        addButton: $('add-topic'),
        submitButton: $('submit-button'),
        submitKo: $('submit-ko'),
        submitJa: $('submit-ja'),
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

    /** 왼쪽(Ko) 칸 = 방의 첫 번째 언어(A), 오른쪽(Ja) 칸 = 두 번째 언어(B). 서버에는 { lang, text } 로 보냅니다. */
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
                contents: localizedPair(fieldOf(card, 'contentKo', i).value, fieldOf(card, 'contentJa', i).value),
            });
        }
        return {
            names: localizedPair(fieldOf(card, 'nameKo').value, fieldOf(card, 'nameJa').value),
            questions,
        };
    }

    /** 입력 칸의 언어 이름과 lang/dir 을 방의 두 언어로 맞춥니다. */
    function applyRoomLanguages(card) {
        if (!state.languages) {
            return;
        }
        [['ko', state.languages[0]], ['ja', state.languages[1]]].forEach(([slot, code]) => {
            const meta = LANGUAGES[code];
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
            card.querySelector('.topic-form-no-ko').textContent = `주제 ${index + 1}`;
            card.querySelector('.topic-form-no-ja').textContent = `テーマ${index + 1}`;
            card.querySelector('.topic-remove').hidden = cards.length <= 1;
        });

        els.modeToggle.querySelectorAll('button[data-mode]').forEach((button) => {
            button.setAttribute('aria-pressed', String(button.dataset.mode === state.mode));
        });

        els.addButton.hidden = !isBulk;
        els.addButton.disabled = cards.length >= MAX_TOPICS;

        const count = activeCards().length;
        els.submitKo.textContent = isBulk ? `${count}개 한꺼번에 등록하기` : '등록하기';
        els.submitJa.textContent = isBulk ? `${count}件まとめて登録する` : '登録する';
    }

    function hideMessages() {
        hideMessage(els.message);
        hideMessage(els.success);
    }

    function createCard() {
        const card = els.template.content.firstElementChild.cloneNode(true);
        card.querySelector('.topic-remove').addEventListener('click', () => removeCard(card));
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
        fieldOf(card, 'nameKo').focus();
        card.scrollIntoView({ behavior: prefersReducedMotion() ? 'auto' : 'smooth', block: 'center' });
    }

    function removeCard(card) {
        if (allCards().length <= 1) {
            return;
        }
        if (hasContent(card) && !window.confirm('이 주제를 삭제할까요?\nこのテーマを削除しますか？')) {
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

    function registerErrorMessage(error) {
        if (error instanceof ApiError) {
            if (error.status === 0) {
                return MESSAGES.network;
            }
            if (error.status === 400) {
                return MESSAGES.invalidInput;
            }
        }
        return MESSAGES.generic;
    }

    function showSuccess(count) {
        const link = document.createElement('a');
        link.className = 'inline-link';
        link.href = 'topics.html';
        link.append(textEl('span', '', '전체 주제 보기', 'ko'), document.createTextNode(' / '), textEl('span', '', 'テーマ一覧を見る', 'ja'));

        els.success.replaceChildren(
            textEl('span', '', `${count}개의 주제 등록 요청이 접수됐어요. 잠시 뒤 전체 주제에서 확인할 수 있어요.`, 'ko'),
            textEl('span', '', `${count}件のテーマの登録リクエストを受け付けました。しばらくすると、テーマ一覧で確認できます。`, 'ja'),
            link
        );
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
            showMessage(els.message, MESSAGES.generic); // 방 정보를 아직 못 읽었어요.
            return;
        }

        const cards = activeCards();
        const firstInvalid = markEmptyFields(cards);
        if (firstInvalid) {
            showMessage(els.message, MESSAGES.emptyFields);
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
            showMessage(els.message, registerErrorMessage(error));
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

    els.topicForms.append(createCard());
    refresh();

    Promise.all([i18nReady, roomReady]).then(([, room]) => {
        if (!room) {
            return;
        }
        state.languages = room.members.map((member) => member.language);
        allCards().forEach(applyRoomLanguages);
    });
})();