/* 전체 주제 페이지: 주제 목록 (Slice 페이징) */
(function () {
    'use strict';

    const PAGE_SIZE = 20;

    const els = {
        list: $('topic-list'),
        message: $('list-message'),
        loadMore: $('load-more'),
    };

    const state = {
        nextPage: 0,
        hasNext: false,
        loading: false,
    };

    function createPill(usedDate) {
        const pill = document.createElement('span');
        pill.className = usedDate ? 'pill pill--used' : 'pill pill--new';

        const ko = usedDate ? `${formatShortDate(usedDate)} 사용` : '새 주제';
        const ja = usedDate ? '使用済み' : '新テーマ';
        pill.append(textEl('span', '', ko, 'ko'), document.createTextNode(' / '), textEl('span', '', ja, 'ja'));
        return pill;
    }

    function createRow(topic) {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = topic.usedDate ? 'topic-row is-used' : 'topic-row is-new';
        button.append(
            textEl('span', 'topic-name topic-name--ko', topic.nameKo, 'ko'),
            textEl('span', 'topic-name topic-name--ja', topic.nameJa, 'ja'),
            createPill(topic.usedDate)
        );
        button.addEventListener('click', () => openQuestions(topic));
        item.append(button);
        return item;
    }

    async function loadTopics() {
        if (state.loading) {
            return;
        }

        state.loading = true;
        els.loadMore.disabled = true;
        hideMessage(els.message);

        try {
            const data = await apiGet(`/api/topics?page=${state.nextPage}&size=${PAGE_SIZE}`);
            data.content.forEach((topic) => els.list.append(createRow(topic)));
            state.nextPage = data.page + 1;
            state.hasNext = data.hasNext;

            if (els.list.children.length === 0) {
                showMessage(els.message, MESSAGES.emptyTopics, 'info');
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

    els.loadMore.addEventListener('click', loadTopics);

    loadTopics();
})();