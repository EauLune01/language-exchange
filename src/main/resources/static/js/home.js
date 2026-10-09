/* 메인 페이지: 주제 뽑기
 *  - 뽑기 버튼을 누를 때마다 아직 안 쓴 주제 중 하나가 뽑혀요. (주에 한 번 제한은 없어요)
 *  - 뽑으면 헤더의 진행바도 한 칸 올라가요.
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
    };

    let weeklyTopic = null;

    function fillTopic(topic) {
        setLocalizedText(els.nameA, topic.names[0]);
        setLocalizedText(els.nameB, topic.names[1]);
        els.card.classList.remove('is-empty');
        els.card.removeAttribute('data-i18n-aria-label');
        els.card.removeAttribute('aria-label');
        els.card.disabled = false;
        els.hint.hidden = false;
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
            // 사용 처리는 서버에서 비동기로 되므로, 진행바는 다시 읽지 않고 여기서 하나 올립니다.
            if (roomInfo) {
                roomInfo.studiedCount += 1;
                applyProgress();
            }
        } catch (error) {
            showApiError(els.message, error); // 남은 주제가 없으면 error.NO_AVAILABLE_TOPIC
        }
        els.drawButton.disabled = false;
    }

    els.drawButton.addEventListener('click', drawTopic);

    els.card.addEventListener('click', () => {
        if (weeklyTopic) {
            openQuestions({ ...weeklyTopic, used: true }); // 방금 뽑은 주제 = 사용한 주제
        }
    });

    // 언어 목록(LANGUAGES), 사전, 방 정보가 준비된 뒤에 뽑을 수 있게 합니다.
    Promise.all([i18nReady, roomReady]).then(() => {
        els.drawButton.hidden = false;
    });
})();
