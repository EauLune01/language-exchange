'use strict';

/*
 * 펫: 방의 두 언어 조합마다 동물이 하나씩 있어요 (10개 언어 → 45가지 조합).
 *  - 키: 두 언어 코드를 알파벳 순으로 이은 것. KO·JA 방이든 JA·KO 방이든 같은 동물이 나옵니다.
 *  - name: 화면 언어별 이름 (키는 서버 Language enum 이름)
 *  - 동물은 조합마다 서로 달라요 (이모지도 겹치지 않게 골랐습니다).
 *  - 레벨은 따로 저장하지 않고 방의 학습 횟수와 목표 횟수로 계산합니다: 목표를 다 채우면 100레벨.
 *    목표 25회면 한 번에 4레벨, 50회면 2레벨, 75회면 1~2레벨, 100회면 1레벨씩 올라요.
 * index.html 에서 common.js 다음에 불러옵니다.
 */
const PET_MAX_LEVEL = 100;

const PET_MAP = {
    'AR-EN': { emoji: '🐪', name: { KO: '낙타', JA: 'ラクダ', EN: 'Camel', ZH: '骆驼', ES: 'Camello', FR: 'Chameau', AR: 'جمل', VI: 'Lạc đà', TH: 'อูฐ', IT: 'Cammello' } },
    'AR-ES': { emoji: '🦂', name: { KO: '전갈', JA: 'サソリ', EN: 'Scorpion', ZH: '蝎子', ES: 'Escorpión', FR: 'Scorpion', AR: 'عقرب', VI: 'Bọ cạp', TH: 'แมงป่อง', IT: 'Scorpione' } },
    'AR-FR': { emoji: '🐊', name: { KO: '악어', JA: 'ワニ', EN: 'Crocodile', ZH: '鳄鱼', ES: 'Cocodrilo', FR: 'Crocodile', AR: 'تمساح', VI: 'Cá sấu', TH: 'จระเข้', IT: 'Coccodrillo' } },
    'AR-IT': { emoji: '🦁', name: { KO: '사자', JA: 'ライオン', EN: 'Lion', ZH: '狮子', ES: 'León', FR: 'Lion', AR: 'أسد', VI: 'Sư tử', TH: 'สิงโต', IT: 'Leone' } },
    'AR-JA': { emoji: '🐫', name: { KO: '쌍봉낙타', JA: 'フタコブラクダ', EN: 'Bactrian Camel', ZH: '双峰骆驼', ES: 'Camello bactriano', FR: 'Chameau de Bactriane', AR: 'جمل بختري', VI: 'Lạc đà hai bướu', TH: 'อูฐสองหนอก', IT: 'Cammello battriano' } },
    'AR-KO': { emoji: '🦅', name: { KO: '독수리', JA: 'ワシ', EN: 'Eagle', ZH: '鹰', ES: 'Águila', FR: 'Aigle', AR: 'نسر', VI: 'Đại bàng', TH: 'นกอินทรี', IT: 'Aquila' } },
    'AR-TH': { emoji: '🦛', name: { KO: '하마', JA: 'カバ', EN: 'Hippo', ZH: '河马', ES: 'Hipopótamo', FR: 'Hippopotame', AR: 'فرس النهر', VI: 'Hà mã', TH: 'ฮิปโป', IT: 'Ippopotamo' } },
    'AR-VI': { emoji: '🦜', name: { KO: '앵무새', JA: 'オウム', EN: 'Parrot', ZH: '鹦鹉', ES: 'Loro', FR: 'Perroquet', AR: 'ببغاء', VI: 'Vẹt', TH: 'นกแก้ว', IT: 'Pappagallo' } },
    'AR-ZH': { emoji: '🐍', name: { KO: '뱀', JA: 'ヘビ', EN: 'Snake', ZH: '蛇', ES: 'Serpiente', FR: 'Serpent', AR: 'ثعبان', VI: 'Rắn', TH: 'งู', IT: 'Serpente' } },
    'EN-ES': { emoji: '🦙', name: { KO: '라마', JA: 'ラマ', EN: 'Llama', ZH: '大羊驼', ES: 'Llama', FR: 'Lama', AR: 'لاما', VI: 'Lạc đà không bướu', TH: 'ลามะ', IT: 'Lama' } },
    'EN-FR': { emoji: '🦏', name: { KO: '코뿔소', JA: 'サイ', EN: 'Rhinoceros', ZH: '犀牛', ES: 'Rinoceronte', FR: 'Rhinocéros', AR: 'وحيد القرن', VI: 'Tê giác', TH: 'แรด', IT: 'Rinoceronte' } },
    'EN-IT': { emoji: '🐺', name: { KO: '늑대', JA: 'オオカミ', EN: 'Wolf', ZH: '狼', ES: 'Lobo', FR: 'Loup', AR: 'ذئب', VI: 'Sói', TH: 'หมาป่า', IT: 'Lupo' } },
    'EN-JA': { emoji: '🐼', name: { KO: '팬더', JA: 'パンダ', EN: 'Panda', ZH: '熊猫', ES: 'Panda', FR: 'Panda', AR: 'باندا', VI: 'Gấu trúc', TH: 'แพนด้า', IT: 'Panda' } },
    'EN-KO': { emoji: '🐯', name: { KO: '호랑이', JA: 'トラ', EN: 'Tiger', ZH: '虎', ES: 'Tigre', FR: 'Tigre', AR: 'نمر', VI: 'Hổ', TH: 'เสือ', IT: 'Tigre' } },
    'EN-TH': { emoji: '🐬', name: { KO: '돌고래', JA: 'イルカ', EN: 'Dolphin', ZH: '海豚', ES: 'Delfín', FR: 'Dauphin', AR: 'دلفين', VI: 'Cá heo', TH: 'โลมา', IT: 'Delfino' } },
    'EN-VI': { emoji: '🦋', name: { KO: '나비', JA: 'チョウ', EN: 'Butterfly', ZH: '蝴蝶', ES: 'Mariposa', FR: 'Papillon', AR: 'فراشة', VI: 'Bướm', TH: 'ผีเสื้อ', IT: 'Farfalla' } },
    'EN-ZH': { emoji: '🐕', name: { KO: '강아지', JA: 'イヌ', EN: 'Dog', ZH: '狗', ES: 'Perro', FR: 'Chien', AR: 'كلب', VI: 'Chó', TH: 'สุนัข', IT: 'Cane' } },
    'ES-FR': { emoji: '🐎', name: { KO: '말', JA: '馬', EN: 'Horse', ZH: '马', ES: 'Caballo', FR: 'Cheval', AR: 'حصان', VI: 'Ngựa', TH: 'ม้า', IT: 'Cavallo' } },
    'ES-IT': { emoji: '🐂', name: { KO: '황소', JA: '牛', EN: 'Bull', ZH: '公牛', ES: 'Toro', FR: 'Taureau', AR: 'ثور', VI: 'Bò đực', TH: 'วัวกระทิง', IT: 'Toro' } },
    'ES-JA': { emoji: '🦌', name: { KO: '사슴', JA: '鹿', EN: 'Deer', ZH: '鹿', ES: 'Ciervo', FR: 'Cerf', AR: 'غزال', VI: 'Hươu', TH: 'กวาง', IT: 'Cervo' } },
    'ES-KO': { emoji: '🦩', name: { KO: '홍학', JA: 'フラミンゴ', EN: 'Flamingo', ZH: '火烈鸟', ES: 'Flamenco', FR: 'Flamant rose', AR: 'طائر الفلامنغو', VI: 'Hồng hạc', TH: 'นกฟลามิงโก', IT: 'Fenicottero' } },
    'ES-TH': { emoji: '🦎', name: { KO: '이구아나', JA: 'イグアナ', EN: 'Iguana', ZH: '鬣蜥', ES: 'Iguana', FR: 'Iguane', AR: 'إيغوانا', VI: 'Kỳ nhông', TH: 'อิกัวนา', IT: 'Iguana' } },
    'ES-VI': { emoji: '🐒', name: { KO: '원숭이', JA: 'サル', EN: 'Monkey', ZH: '猴子', ES: 'Mono', FR: 'Singe', AR: 'قرد', VI: 'Khỉ', TH: 'ลิง', IT: 'Scimmia' } },
    'ES-ZH': { emoji: '🐇', name: { KO: '토끼', JA: 'ウサギ', EN: 'Rabbit', ZH: '兔子', ES: 'Conejo', FR: 'Lapin', AR: 'أرنب', VI: 'Thỏ', TH: 'กระต่าย', IT: 'Coniglio' } },
    'FR-IT': { emoji: '🐓', name: { KO: '수탉', JA: '雄鶏', EN: 'Rooster', ZH: '公鸡', ES: 'Gallo', FR: 'Coq', AR: 'ديك', VI: 'Gà trống', TH: 'ไก่ตัวผู้', IT: 'Gallo' } },
    'FR-JA': { emoji: '🦢', name: { KO: '백조', JA: '白鳥', EN: 'Swan', ZH: '天鹅', ES: 'Cisne', FR: 'Cygne', AR: 'بجعة', VI: 'Thiên nga', TH: 'หงส์', IT: 'Cigno' } },
    'FR-KO': { emoji: '🐸', name: { KO: '개구리', JA: 'カエル', EN: 'Frog', ZH: '青蛙', ES: 'Rana', FR: 'Grenouille', AR: 'ضفدع', VI: 'Ếch', TH: 'กบ', IT: 'Rana' } },
    'FR-TH': { emoji: '🦒', name: { KO: '기린', JA: 'キリン', EN: 'Giraffe', ZH: '长颈鹿', ES: 'Jirafa', FR: 'Girafe', AR: 'زرافة', VI: 'Hươu cao cổ', TH: 'ยีราฟ', IT: 'Giraffa' } },
    'FR-VI': { emoji: '🦉', name: { KO: '올빼미', JA: 'フクロウ', EN: 'Owl', ZH: '猫头鹰', ES: 'Búho', FR: 'Hibou', AR: 'بومة', VI: 'Cú mèo', TH: 'นกฮูก', IT: 'Gufo' } },
    'FR-ZH': { emoji: '🦔', name: { KO: '고슴도치', JA: 'ハリネズミ', EN: 'Hedgehog', ZH: '刺猬', ES: 'Erizo', FR: 'Hérisson', AR: 'قنفذ', VI: 'Nhím', TH: 'เม่นแคระ', IT: 'Riccio' } },
    'IT-JA': { emoji: '🦭', name: { KO: '물개', JA: 'アザラシ', EN: 'Seal', ZH: '海豹', ES: 'Foca', FR: 'Phoque', AR: 'فقمة', VI: 'Hải cẩu', TH: 'แมวน้ำ', IT: 'Foca' } },
    'IT-KO': { emoji: '🦝', name: { KO: '너구리', JA: 'タヌキ', EN: 'Raccoon', ZH: '浣熊', ES: 'Mapache', FR: 'Raton laveur', AR: 'راكون', VI: 'Gấu mèo', TH: 'แรคคูน', IT: 'Procione' } },
    'IT-TH': { emoji: '🐋', name: { KO: '고래', JA: 'クジラ', EN: 'Whale', ZH: '鲸鱼', ES: 'Ballena', FR: 'Baleine', AR: 'حوت', VI: 'Cá voi', TH: 'วาฬ', IT: 'Balena' } },
    'IT-VI': { emoji: '🦆', name: { KO: '오리', JA: 'アヒル', EN: 'Duck', ZH: '鸭子', ES: 'Pato', FR: 'Canard', AR: 'بطة', VI: 'Vịt', TH: 'เป็ด', IT: 'Anatra' } },
    'IT-ZH': { emoji: '🦦', name: { KO: '수달', JA: 'カワウソ', EN: 'Otter', ZH: '水獭', ES: 'Nutria', FR: 'Loutre', AR: 'قضاعة', VI: 'Rái cá', TH: 'นาก', IT: 'Lontra' } },
    'JA-KO': { emoji: '🦊', name: { KO: '여우', JA: 'キツネ', EN: 'Fox', ZH: '狐狸', ES: 'Zorro', FR: 'Renard', AR: 'ثعلب', VI: 'Cáo', TH: 'จิ้งจอก', IT: 'Volpe' } },
    'JA-TH': { emoji: '🐟', name: { KO: '잉어', JA: '鯉', EN: 'Koi', ZH: '锦鲤', ES: 'Carpa koi', FR: 'Carpe koï', AR: 'سمكة كوي', VI: 'Cá koi', TH: 'ปลาคาร์ฟ', IT: 'Carpa koi' } },
    'JA-VI': { emoji: '🐢', name: { KO: '거북이', JA: 'カメ', EN: 'Turtle', ZH: '乌龟', ES: 'Tortuga', FR: 'Tortue', AR: 'سلحفاة', VI: 'Rùa', TH: 'เต่า', IT: 'Tartaruga' } },
    'JA-ZH': { emoji: '🐈', name: { KO: '고양이', JA: 'ネコ', EN: 'Cat', ZH: '猫', ES: 'Gato', FR: 'Chat', AR: 'قطة', VI: 'Mèo', TH: 'แมว', IT: 'Gatto' } },
    'KO-TH': { emoji: '🐆', name: { KO: '치타', JA: 'チーター', EN: 'Cheetah', ZH: '猎豹', ES: 'Guepardo', FR: 'Guépard', AR: 'فهد', VI: 'Báo cheetah', TH: 'เสือชีตาห์', IT: 'Ghepardo' } },
    'KO-VI': { emoji: '🐃', name: { KO: '물소', JA: 'スイギュウ', EN: 'Water Buffalo', ZH: '水牛', ES: 'Búfalo de agua', FR: 'Buffle d’eau', AR: 'جاموس', VI: 'Trâu', TH: 'ควาย', IT: 'Bufalo d’acqua' } },
    'KO-ZH': { emoji: '🐻', name: { KO: '반달곰', JA: 'ツキノワグマ', EN: 'Moon Bear', ZH: '月熊', ES: 'Oso luna', FR: 'Ours à collier', AR: 'دب القمر', VI: 'Gấu ngựa', TH: 'หมีคอขาว', IT: 'Orso lunare' } },
    'TH-VI': { emoji: '🦚', name: { KO: '공작', JA: 'クジャク', EN: 'Peacock', ZH: '孔雀', ES: 'Pavo real', FR: 'Paon', AR: 'طاووس', VI: 'Công', TH: 'นกยูง', IT: 'Pavone' } },
    'TH-ZH': { emoji: '🐘', name: { KO: '코끼리', JA: 'ゾウ', EN: 'Elephant', ZH: '象', ES: 'Elefante', FR: 'Éléphant', AR: 'فيل', VI: 'Voi', TH: 'ช้าง', IT: 'Elefante' } },
    'VI-ZH': { emoji: '🐉', name: { KO: '용', JA: '龍', EN: 'Dragon', ZH: '龙', ES: 'Dragón', FR: 'Dragon', AR: 'تنين', VI: 'Rồng', TH: 'มังกร', IT: 'Drago' } },
};

/** 학습 횟수와 목표 횟수로 펫 레벨(0~100)을 계산합니다. 헤더 진행바의 퍼센트와 같은 값이에요. */
function getPetLevel(studiedCount, goal) {
    return Math.min(PET_MAX_LEVEL, Math.floor(studiedCount * PET_MAX_LEVEL / goal));
}

/** 방의 두 언어 코드로 펫 { emoji, name } 을 찾습니다. 순서는 상관없어요. */
function getPet(langA, langB) {
    return PET_MAP[[langA, langB].sort().join('-')] || { emoji: '🐾', name: {} };
}

/** 화면 언어(uiLang)로 쓴 펫 이름. 없으면 영어 이름 */
function getPetName(langA, langB, uiLang) {
    const pet = getPet(langA, langB);
    return pet.name[uiLang] || pet.name.EN || '';
}
