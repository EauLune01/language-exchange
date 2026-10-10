'use strict';

/*
 * 공유 카드: 100레벨 달성·지난달 요약을 이미지 한 장으로 그려서 공유합니다.
 *  - 크기는 4:5 (1080×1350). 인스타그램 피드에 꽉 차고, 스토리에 올려도 잘리지 않아요.
 *  - 라이브러리 없이 Canvas 에 직접 그립니다. 배경은 방의 두 언어 모티프와 보조 색(languages.js)이에요.
 *  - 휴대폰에서는 공유 시트(인스타그램·카카오톡 등)가 열리고, 파일 공유가 안 되는 브라우저에서는 이미지를 내려받습니다.
 * index.html 에서 pet.js 다음에 불러옵니다.
 */
const SHARE_CARD_WIDTH = 1080;
const SHARE_CARD_HEIGHT = 1350;
const SHARE_CARD_MOTIF_SIZE = 400;

async function loadShareImage(src) {
    const image = new Image();
    image.src = src;
    await image.decode();
    return image;
}

/**
 * card = { emoji, headline, sub, note } 를 canvas 에 그립니다. note 는 없어도 돼요.
 * 방 이름·언어는 roomInfo 에서 읽으므로 roomReady, i18nReady 뒤에 불러야 합니다.
 */
async function drawShareCard(canvas, card) {
    const width = SHARE_CARD_WIDTH;
    const height = SHARE_CARD_HEIGHT;
    const [memberA, memberB] = roomInfo.members;
    const metaA = LANGUAGES[memberA.language];
    const metaB = LANGUAGES[memberB.language];
    const rootStyle = getComputedStyle(document.documentElement);
    const ink = rootStyle.getPropertyValue('--ink').trim();
    const muted = rootStyle.getPropertyValue('--muted').trim();
    const fontFamily = getComputedStyle(document.body).fontFamily;

    // 웹 글꼴이 아직 안 왔으면 기기 글꼴로 그려지므로 기다립니다. 모티프는 못 읽어도 카드는 그려요.
    const [motifA, motifB] = await Promise.all([
        loadShareImage(metaA.motif).catch(() => null),
        loadShareImage(metaB.motif).catch(() => null),
        document.fonts.ready,
    ]);

    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');

    const background = ctx.createLinearGradient(0, 0, width, height);
    background.addColorStop(0, metaA.accent);
    background.addColorStop(1, metaB.accent);
    ctx.fillStyle = background;
    ctx.fillRect(0, 0, width, height);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.beginPath();
    ctx.roundRect(56, 56, width - 112, height - 112, 72);
    ctx.fill();

    // 모티프 SVG 에는 크기가 없어서(viewBox 만 있음) 그릴 크기를 꼭 정해 줍니다.
    if (motifA) {
        ctx.drawImage(motifA, -60, -60, SHARE_CARD_MOTIF_SIZE, SHARE_CARD_MOTIF_SIZE);
    }
    if (motifB) {
        ctx.drawImage(motifB, width - SHARE_CARD_MOTIF_SIZE + 60, height - SHARE_CARD_MOTIF_SIZE + 60,
            SHARE_CARD_MOTIF_SIZE, SHARE_CARD_MOTIF_SIZE);
    }

    ctx.textAlign = 'center';
    ctx.direction = document.documentElement.dir || 'ltr';
    // 긴 이름·문구는 줄바꿈 대신 카드 너비에 맞게 좁혀서 한 줄로 그립니다 (fillText 의 maxWidth).
    const line = (text, y, size, color) => {
        ctx.font = `${size}px ${fontFamily}`;
        ctx.fillStyle = color;
        ctx.fillText(text, width / 2, y, width - 240);
    };

    line(t('app.name'), 300, 44, muted);
    line(card.emoji, 640, 280, ink);
    line(card.headline, 820, 92, ink);
    line(card.sub, 910, 52, ink);
    if (card.note) {
        line(card.note, 985, 42, muted);
    }
    line(`${memberA.name} & ${memberB.name}`, 1120, 52, ink);
    line(`${metaA.name} · ${metaB.name}`, 1185, 38, muted);
}

/** 그린 카드를 공유할 PNG 파일로 만듭니다. */
function shareCardFile(canvas) {
    return new Promise((resolve, reject) => {
        canvas.toBlob((blob) => {
            if (blob) {
                resolve(new File([blob], 'language-exchange.png', { type: 'image/png' }));
            } else {
                reject(new Error('failed to export card'));
            }
        }, 'image/png');
    });
}

/**
 * 공유 시트를 엽니다. 파일 공유를 못 하는 브라우저(대부분의 PC)에서는 이미지를 내려받아요.
 * 사파리는 버튼을 누른 직후에만 공유를 허락하므로, 파일은 미리 만들어 두고 누르면 바로 이 함수를 부릅니다.
 */
async function shareCard(file) {
    if (navigator.canShare && navigator.canShare({ files: [file] })) {
        try {
            await navigator.share({ files: [file] });
            return;
        } catch (error) {
            if (error.name === 'AbortError') {
                return; // 공유 시트를 그냥 닫은 경우
            }
        }
    }
    const link = createElement('a', '', { href: URL.createObjectURL(file), download: file.name });
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
