/* 메인 페이지: 이번 주 주제 뽑기
 *  - 이번 주에 이미 뽑은 주제가 있으면 바로 보여주고 뽑기 버튼은 숨겨요.
 *  - 아직 안 뽑았으면 뽑기 버튼을 보여줘요.
 */
(function () {
    'use strict';

    const FLIP_MS = 220;

    const els = {
        card: $('weekly-card'),
        ko: $('weekly-ko'),
        ja: $('weekly-ja'),
        hint: $('weekly-hint'),
        message: $('weekly-message'),
        drawButton: $('draw-button'),
        descKo: $('hero-desc-ko'),
        descJa: $('hero-desc-ja'),
    };

    let weeklyTopic = null;

    /** 다음 주 월요일 (이번 주 주제를 뽑았다면, 새 주제는 이 날부터 뽑을 수 있어요) */
    function nextMonday() {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const daysUntilMonday = ((8 - today.getDay()) % 7) || 7;
        const next = new Date(today);
        next.setDate(today.getDate() + daysUntilMonday);
        return next;
    }

    function showDrawnDescription() {
        const next = nextMonday();
        const month = next.getMonth() + 1;
        const day = next.getDate();
        els.descKo.textContent = `이번 주 주제가 정해졌어요. 다음 주제는 ${month}월 ${day}일(월)부터 뽑을 수 있어요.`;
        els.descJa.textContent = `今週のテーマが決まりました。次のテーマは${month}月${day}日(月)から引けます。`;
    }

    function fillTopic(topic) {
        els.ko.textContent = topic.nameKo;
        els.ja.textContent = topic.nameJa;
        els.card.classList.remove('is-empty');
        els.card.removeAttribute('aria-label');
        els.card.disabled = false;
        els.hint.hidden = false;
        els.drawButton.hidden = true;
        showDrawnDescription();
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
            showMessage(els.message, errorMessage(error, 'noTopic'));
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

    checkThisWeek();
})();