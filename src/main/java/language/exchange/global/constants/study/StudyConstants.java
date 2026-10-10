package language.exchange.global.constants.study;


public interface StudyConstants {

    int QUESTION_COUNT = 3;

    // 통계 화면의 월별 그래프가 보여주는 개월 수 (이번 달 포함)
    int STATS_MONTHS = 6;

    // 기본 추천 주제 (classpath). 일괄 등록 요청과 같은 모양이고, names/contents 에 언어를 2개보다 많이 적어도 된다
    String DEFAULT_TOPICS_PATH = "/default-topics.json";
}