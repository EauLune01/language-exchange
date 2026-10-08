/* 전체 주제 페이지: 아직 안 쓴 주제 → 사용한 주제 (서버가 정렬해서 내려줘요) */
(function () {
    'use strict';

    const PAGE_SIZE = 20;

    const GROUP_LABELS = {
        new: { ko: '아직 안 쓴 주제', ja: 'まだ使っていないテーマ' },
        used: { ko: '사용한 주제', ja: '使ったテーマ' },
    };

    const els = {
        list: $('topic-list'),
        message: $('list-message'),
        loadMore: $('load-more'),
    };

    const state = {
        nextPage: 0,
        hasNext: false,
        loading: false,
        lastGroup: null,
    };

    function createGroupHeading(group) {
        const item = document.createElement('li');
        item.className = 'group-heading';
        item.append(
            textEl('span', 'group-heading-ko', GROUP_LABELS[group].ko, 'ko'),
            textEl('span', 'group-heading-ja', GROUP_LABELS[group].ja, 'ja')
        );
        return item;
    }

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
            localizedEl('span', 'topic-name topic-name--ko', topic.names[0]),
            localizedEl('span', 'topic-name topic-name--ja', topic.names[1]),
            createPill(topic.usedDate)
        );
        button.addEventListener('click', () => openQuestions(topic));
        item.append(button);
        return item;
    }

    function appendTopic(topic) {
        const group = topic.usedDate ? 'used' : 'new';
        if (group !== state.lastGroup) {
            els.list.append(createGroupHeading(group));
            state.lastGroup = group;
        }
        els.list.append(createRow(topic));
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
            data.content.forEach(appendTopic);
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

    i18nReady.then(loadTopics); // 언어 목록(LANGUAGES)이 준비된 뒤에 주제를 그립니다.
})();