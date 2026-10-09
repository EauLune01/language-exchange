/* 통계 페이지: 방의 언어교환을 숫자와 그래프로 돌아봅니다. (GET /api/stats + 방 정보의 목표 횟수) */
(function () {
    'use strict';

    const BAR_MAX_HEIGHT = 140; // 가장 많이 한 달의 막대 높이(px). CSS 의 --ratio 에 곱해 씁니다.
    const DAY_MS = 24 * 60 * 60 * 1000;

    const els = {
        stats: $('stats'),
        message: $('stats-message'),
    };

    let stats = null; // 화면 언어를 바꾸면 다시 그릴 수 있게 담아 둡니다.

    function formatNumber(value) {
        return new Intl.NumberFormat(i18nLocale()).format(value);
    }

    /** 숫자 타일 하나: 작은 이름표 + 큰 숫자 + (있으면) 보조 설명 */
    function createTile(labelKey, value, sub, className) {
        const tile = createElement('div', className ? `stat-tile ${className}` : 'stat-tile');
        const number = createElement('strong', 'stat-value');
        number.textContent = value === null ? '–' : formatNumber(value);
        tile.append(i18nEl('span', 'stat-label', labelKey), number);
        if (sub) {
            tile.append(i18nEl('span', 'stat-sub', sub.key, sub.params));
        }
        return tile;
    }

    /** 첫 날을 1일째로 센, 처음 주제를 뽑은 날부터 오늘까지의 날 수 */
    function daysSince(isoDate) {
        const [year, month, day] = isoDate.split('-').map(Number);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return Math.max(1, Math.round((today - new Date(year, month - 1, day)) / DAY_MS) + 1);
    }

    function createTiles() {
        const lastMonth = stats.monthly[stats.monthly.length - 2];
        const tiles = createElement('section', 'stat-tiles');
        tiles.append(
            createTile('stats.total', stats.usedTopics,
                roomInfo && { key: 'stats.goal', params: { goal: roomInfo.goal } }, 'stat-tile--hero'),
            createTile('stats.thisMonth', stats.thisMonthCount,
                lastMonth && { key: 'stats.lastMonth', params: { count: lastMonth.count } }),
            createTile('stats.streak', stats.weekStreak, { key: 'stats.streakNote' }),
            createTile('stats.days', stats.firstUsedDate ? daysSince(stats.firstUsedDate) : null,
                stats.firstUsedDate && { key: 'stats.since', params: { date: { date: stats.firstUsedDate, style: 'full' } } })
        );
        return tiles;
    }

    /** 등록한 주제 중 완료한 주제: 비율 하나라서 원그래프 대신 한 줄 막대(미터)로 보여줍니다. */
    function createTopicsPanel() {
        const { totalTopics, usedTopics } = stats;
        const percent = totalTopics ? Math.round(usedTopics / totalTopics * 100) : 0;

        const figure = createElement('p', 'stat-fraction');
        const done = createElement('strong', 'stat-fraction-done');
        done.textContent = formatNumber(usedTopics);
        const total = createElement('span', 'stat-fraction-total');
        total.textContent = ` / ${formatNumber(totalTopics)}`;
        figure.append(done, total);

        const meter = createElement('div', 'stat-meter', {
            role: 'progressbar', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': percent, 'aria-labelledby': 'stat-topics-title',
        });
        const fill = createElement('span', 'stat-meter-fill');
        fill.style.width = `${percent}%`;
        meter.append(fill);

        const title = i18nEl('h2', 'stat-title', 'stats.topics.title');
        title.id = 'stat-topics-title';

        const foot = createElement('p', 'stat-foot');
        foot.append(
            i18nEl('span', '', 'stats.topics.done', { percent }),
            i18nEl('span', '', 'stats.topics.left', { left: totalTopics - usedTopics })
        );

        const panel = createElement('section', 'stat-panel');
        panel.append(title, i18nEl('p', 'stat-note', 'stats.topics.note'), figure, meter, foot);
        return panel;
    }

    /** 최근 6개월의 월별 횟수: 세로 막대. 이번 달만 진한 색으로 강조하고, 값은 막대 위에 숫자로도 적습니다. */
    function createMonthlyPanel() {
        const max = Math.max(1, ...stats.monthly.map((entry) => entry.count));
        const monthName = new Intl.DateTimeFormat(i18nLocale(), { month: 'short' });

        const bars = createElement('ol', 'stat-bars');
        stats.monthly.forEach((entry, index) => {
            const [year, month] = entry.month.split('-').map(Number);
            const label = monthName.format(new Date(year, month - 1, 1));

            const item = createElement('li', index === stats.monthly.length - 1 ? 'stat-bar-item is-current' : 'stat-bar-item');
            item.title = `${label} · ${formatNumber(entry.count)}`;

            // 화면 낭독기가 "10월 3" 순서로 읽도록 달 이름을 먼저 넣고, 보이는 순서는 CSS(order)로 바꿉니다.
            const name = createElement('span', 'stat-bar-month');
            name.textContent = label;
            const value = createElement('span', entry.count ? 'stat-bar-value' : 'stat-bar-value is-zero');
            value.textContent = formatNumber(entry.count);
            const bar = createElement('span', 'stat-bar', { 'aria-hidden': 'true' });
            bar.style.height = `${Math.round(entry.count / max * BAR_MAX_HEIGHT)}px`;

            item.append(name, value, bar);
            bars.append(item);
        });

        const panel = createElement('section', 'stat-panel');
        panel.append(
            i18nEl('h2', 'stat-title', 'stats.monthly.title'),
            i18nEl('p', 'stat-note', 'stats.monthly.note', { months: stats.monthly.length }),
            bars
        );
        return panel;
    }

    function render() {
        const panels = createElement('div', 'stat-panels');
        panels.append(createTopicsPanel(), createMonthlyPanel());
        els.stats.replaceChildren(createTiles(), panels);
    }

    async function loadStats() {
        try {
            stats = await apiGet('/api/stats');
            render();
        } catch (error) {
            showApiError(els.message, error);
        }
    }

    // 숫자·달 이름의 모양이 화면 언어마다 달라서, 언어를 바꾸면 다시 그립니다.
    document.addEventListener('i18n:change', () => {
        if (stats) {
            render();
        }
    });

    // 언어 목록·사전과 방 정보(목표 횟수)가 준비된 뒤에 시작합니다.
    Promise.all([i18nReady, roomReady]).then(loadStats);
})();
