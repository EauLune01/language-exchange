/* 메인 페이지: 이번 주 주제 뽑기
 *  - 이번 주에 이미 뽑은 주제가 있으면 바로 보여주고 뽑기 버튼은 숨겨요.
 *  - 아직 안 뽑았으면 뽑기 버튼을 보여줘요.
 */
(function () {
    'use strict';

    const FLIP_MS = 220;

    const els = {
        card: $('weekly-card'),
        nameA: $('weekly-a'),
        nameB: $('weekly-b'),
        hint: $('weekly-hint'),
        message: $('weekly-message'),
        drawButton: $('draw-button'),
        desc: $('hero-desc'),
    };

    let weeklyTopic = null;

    /** 다음 주 월요일을 '2026-10-12' 형태로 (이번 주 주제를 뽑았다면, 새 주제는 이 날부터 뽑을 수 있어요) */
    function nextMondayIso() {
        const next = new Date();
        const daysUntilMonday = ((8 - next.getDay()) % 7) || 7;
        next.setDate(next.getDate() + daysUntilMonday);
        const pad = (value) => String(value).padStart(2, '0');
        return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}`;
    }

    function fillTopic(topic) {
        setLocalizedText(els.nameA, topic.names[0]);
        setLocalizedText(els.nameB, topic.names[1]);
        els.card.classList.remove('is-empty');
        els.card.removeAttribute('data-i18n-aria-label');
        els.card.removeAttribute('aria-label');
        els.card.disabled = false;
        els.hint.hidden = false;
        els.drawButton.hidden = true;
        i18nSet(els.desc, 'home.hero.drawn', { date: { date: nextMondayIso(), style: 'weekday' } });
        weeklyTopic = topic;
    }

    /** animate=true 면 카드가 뒤집히며 나타나요 (버튼을 눌렀을 때) */
    function revealTopic(topic, animate) {
        return new Promise((resolve) => {
            if (!animate || prefersReducedMotion()) {
                fillTopic(topic);
                resolve();
                return;
            }

            els.card.classList.add('is-flipping');
            window.setTimeout(() => {
                fillTopic(topic);
                els.card.classList.remove('is-flipping');
                resolve();
            }, FLIP_MS);
        });
    }

    async function drawTopic() {
        els.drawButton.disabled = true;
        hideMessage(els.message);

        try {
            const topic = await apiGet('/api/topics/weekly');
            await revealTopic(topic, true);
        } catch (error) {
            showApiError(els.message, error); // 남은 주제가 없으면 error.NO_AVAILABLE_TOPIC
            els.drawButton.disabled = false;
        }
    }

    /** 페이지를 열 때: 이번 주에 이미 뽑은 주제가 있는지 확인 (뽑지는 않아요) */
    async function checkThisWeek() {
        try {
            const topic = await apiGet('/api/topics/this-week');
            if (topic) {
                await revealTopic(topic, false);
                return;
            }
        } catch (error) {
            /* 확인에 실패해도 뽑기 버튼은 쓸 수 있게 합니다. */
        }
        els.drawButton.hidden = false;
    }

    els.drawButton.addEventListener('click', drawTopic);

    els.card.addEventListener('click', () => {
        if (weeklyTopic) {
            openQuestions(weeklyTopic);
        }
    });

    i18nReady.then(checkThisWeek); // 언어 목록(LANGUAGES)과 사전이 준비된 뒤에 시작합니다.
})();
