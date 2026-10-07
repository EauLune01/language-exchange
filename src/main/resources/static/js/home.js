/* 메인 페이지: 이번 주 주제 뽑기 */
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
    };

    let weeklyTopic = null;

    function revealTopic(topic) {
        return new Promise((resolve) => {
            const fill = () => {
                els.ko.textContent = topic.nameKo;
                els.ja.textContent = topic.nameJa;
                els.card.classList.remove('is-empty');
                els.card.removeAttribute('aria-label');
                els.card.disabled = false;
                els.hint.hidden = false;
                els.drawButton.hidden = true;
                weeklyTopic = topic;
            };

            if (prefersReducedMotion()) {
                fill();
                resolve();
                return;
            }

            els.card.classList.add('is-flipping');
            window.setTimeout(() => {
                fill();
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
            await revealTopic(topic);
        } catch (error) {
            showMessage(els.message, errorMessage(error, 'noTopic'));
            els.drawButton.disabled = false;
        }
    }

    els.drawButton.addEventListener('click', drawTopic);

    els.card.addEventListener('click', () => {
        if (weeklyTopic) {
            openQuestions(weeklyTopic);
        }
    });
})();