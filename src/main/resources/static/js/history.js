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

    function createRound(round) {
        const wrap = document.createElement('span');
        wrap.className = 'round';

        const label = document.createElement('span');
        label.className = 'round-label';
        label.append(textEl('span', '', '회차', 'ko'), textEl('span', '', '回目', 'ja'));

        wrap.append(textEl('span', 'round-no', String(round)), label);
        return wrap;
    }

    function createThisWeekPill() {
        const pill = document.createElement('span');
        pill.className = 'pill pill--now';
        pill.append(textEl('span', '', '이번 주', 'ko'), document.createTextNode(' / '), textEl('span', '', '今週', 'ja'));
        return pill;
    }

    function createDate(usedDate) {
        const date = document.createElement('span');
        date.className = 'history-date';
        date.append(
            textEl('span', '', formatDateKo(usedDate), 'ko'),
            document.createTextNode(' / '),
            textEl('span', '', formatDateJa(usedDate), 'ja')
        );
        return date;
    }

    function createCard(entry) {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = isThisWeek(entry.usedDate) ? 'history-card is-current' : 'history-card';

        const top = document.createElement('span');
        top.className = 'history-top';
        top.append(createRound(entry.round));
        if (isThisWeek(entry.usedDate)) {
            top.append(createThisWeekPill());
        }

        const names = document.createElement('span');
        names.className = 'history-names';
        names.append(
            textEl('span', 'topic-name topic-name--ko', entry.nameKo, 'ko'),
            textEl('span', 'topic-name topic-name--ja', entry.nameJa, 'ja')
        );

        button.append(top, names, createDate(entry.usedDate));
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
                showMessage(els.message, MESSAGES.emptyHistory, 'info');
            }
        } catch (error) {
            showMessage(els.message, errorMessage(error));
            state.hasNext = true; // 실패하면 '더 보기' 버튼이 다시 시도 버튼 역할을 합니다.
        } finally {
            state.loading = false;
            els.loadMore.disabled = false;
            els.loadMore.hidden = !state.hasNext;
        }
    }

    els.loadMore.addEventListener('click', loadHistory);

    loadHistory();
})();