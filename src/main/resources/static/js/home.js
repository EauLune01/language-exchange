/* 메인 페이지: 주제 뽑기
 *  - 뽑기 버튼을 누를 때마다 아직 안 쓴 주제 중 하나가 뽑혀요. (주에 한 번 제한은 없어요)
 *  - 뽑으면 헤더의 진행바도 한 칸 올라가요.
 *  - 펫: 방의 두 언어 조합에 맞는 동물(pet.js)을 보여줘요. 목표 횟수를 다 채우면 100레벨이고, 뽑아서 레벨이 오르면 창으로 알려 줍니다.
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
        petSection: $('pet-section'),
        petEmoji: $('pet-emoji'),
        petName: $('pet-name'),
        petLevel: $('pet-level'),
        petTrack: $('pet-track'),
        petBar: $('pet-bar'),
        congrats: $('pet-congrats-modal'),
        congratsEmoji: $('congrats-emoji'),
        congratsMessage: $('congrats-message'),
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

    /** 축하는 방마다 이 기기에서 한 번만 보여줍니다. */
    function claimCongrats() {
        const key = `exchange:pet-congrats:${roomInfo.loginId}`;
        try {
            if (localStorage.getItem(key)) {
                return false;
            }
            localStorage.setItem(key, '1');
        } catch (storageError) {
            /* 저장이 막힌 브라우저에서는 열 때마다 축하합니다. */
        }
        return true;
    }

    function openPetDialog(pet, key, params) {
        els.congratsEmoji.textContent = pet.emoji;
        i18nSet(els.congratsMessage, key, params);
        if (!els.congrats.open) {
            els.congrats.showModal();
        }
    }

    /**
     * 펫과 레벨(0~100, 목표 횟수를 다 채우면 100)을 roomInfo 의 지금 값으로 그립니다. 아직 한 번도 안 뽑았으면 숨겨 둡니다.
     * previousCount 는 방금 주제를 뽑았을 때만 넘겨요: 뽑기 전 학습 횟수와 비교해 레벨이 올랐으면 알려 줍니다.
     */
    function renderPet(previousCount) {
        if (!roomInfo || roomInfo.studiedCount < 1) {
            return;
        }
        const [langA, langB] = roomInfo.members.map((member) => member.language);
        const pet = getPet(langA, langB);
        const name = getPetName(langA, langB, i18nLang);
        const level = getPetLevel(roomInfo.studiedCount, roomInfo.goal);

        els.petEmoji.textContent = pet.emoji;
        els.petName.textContent = name;
        i18nSet(els.petLevel, 'stats.petLevel', { level });
        els.petBar.style.setProperty('--fill', `${level}%`);
        els.petTrack.setAttribute('aria-valuenow', level);
        els.petSection.hidden = false;

        const from = previousCount === undefined ? level : getPetLevel(previousCount, roomInfo.goal);
        if (level > from) {
            els.petEmoji.classList.remove('is-level-up');
            void els.petEmoji.offsetWidth; // 오를 때마다 폴짝 뛰는 움직임이 다시 돌도록 한 번 그리게 합니다.
            els.petEmoji.classList.add('is-level-up');
        }

        // particle 은 한국어 사전만 쓰는 조사예요: 이름에 받침이 있으면 "이", 없으면 "가".
        const congratsParams = { name, particle: hasBatchim(name) ? '이' : '가' };
        if (level > from && level < PET_MAX_LEVEL) {
            openPetDialog(pet, 'stats.levelUp', { name, from, to: level });
        } else if (level >= PET_MAX_LEVEL && claimCongrats()) {
            // 방금 100레벨이 됐거나, 이미 100레벨인데 이 기기에서 아직 축하한 적이 없을 때
            openPetDialog(pet, 'stats.congrats', congratsParams);
        }
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
                renderPet(roomInfo.studiedCount - 1);
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
        renderPet();
    });

    // 펫 이름은 화면 언어를 따라갑니다.
    document.addEventListener('i18n:change', () => renderPet());
})();
