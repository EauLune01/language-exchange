/* 학습 기록 페이지: 1회차 → n회차 (서버가 오래된 순으로 내려줘요) */
(function () {
    'use strict';

    const PAGE_SIZE = 20;

    const els = {
        list: $('history-list'),
        message: $('list-message'),
        loadMore: $('load-more'),
    };

    const state = {
        nextPage: 0,
        hasNext: false,
        loading: false,
    };

    /**
     * 회차 표시: 큰 숫자 + 작은 글자. 숫자가 앞에 오는지 뒤에 오는지는 언어마다 달라서
     * 사전의 'history.round' 문구('{round}회차', 'Round {round}')에서 {round} 앞뒤 글자를 나눠 씁니다.
     */
    function fillRound(wrap) {
        const [before, after] = t('history.round').split('{round}').map((part) => part.trim());
        const parts = [];
        if (before) {
            parts.push(createElement('span', 'round-label'));
            parts[parts.length - 1].textContent = before;
        }
        parts.push(createElement('span', 'round-no'));
        parts[parts.length - 1].textContent = wrap.dataset.round;
        if (after) {
            parts.push(createElement('span', 'round-label'));
            parts[parts.length - 1].textContent = after;
        }
        wrap.replaceChildren(...parts);
    }

    function createRound(round) {
        const wrap = createElement('span', 'round', { 'data-round': String(round) });
        fillRound(wrap);
        return wrap;
    }

    function createCard(entry) {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = isThisWeek(entry.usedDate) ? 'history-card is-current' : 'history-card';

        const top = createElement('span', 'history-top');
        top.append(createRound(entry.round));
        if (isThisWeek(entry.usedDate)) {
            top.append(i18nEl('span', 'pill pill--now', 'history.thisWeek'));
        }

        const names = createElement('span', 'history-names');
        names.append(
            localizedEl('span', 'topic-name topic-name--a', entry.names[0]),
            localizedEl('span', 'topic-name topic-name--b', entry.names[1])
        );

        const date = i18nEl('span', 'history-date', 'common.date', { date: { date: entry.usedDate, style: 'long' } });

        button.append(top, names, date);
        button.addEventListener('click', () => openQuestions(entry));
        item.append(button);
        return item;
    }

    async function loadHistory() {
        if (state.loading) {
            return;
        }

        state.loading = true;
        els.loadMore.disabled = true;
        hideMessage(els.message);

        try {
            const data = await apiGet(`/api/topics/history?page=${state.nextPage}&size=${PAGE_SIZE}`);
            data.content.forEach((entry) => els.list.append(createCard(entry)));
            state.nextPage = data.page + 1;
            state.hasNext = data.hasNext;

            if (els.list.children.length === 0) {
                showI18nMessage(els.message, 'history.empty', null, 'info');
            }
        } catch (error) {
            showApiError(els.message, error);
            state.hasNext = true; // 실패하면 '더 보기' 버튼이 다시 시도 버튼 역할을 합니다.
        } finally {
            state.loading = false;
            els.loadMore.disabled = false;
            els.loadMore.hidden = !state.hasNext;
        }
    }

    els.loadMore.addEventListener('click', loadHistory);

    // 화면 언어를 바꾸면 회차 글자의 위치가 달라질 수 있어서 다시 그립니다.
    document.addEventListener('i18n:change', () => els.list.querySelectorAll('.round').forEach(fillRound));

    i18nReady.then(loadHistory); // 언어 목록(LANGUAGES)과 사전이 준비된 뒤에 시작합니다.
})();
