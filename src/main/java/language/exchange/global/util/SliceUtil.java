package language.exchange.global.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.List;

public class SliceUtil {

    private SliceUtil() {}

    /** pageSize + 1개를 조회한 결과로 다음 페이지 여부를 판단합니다. (count 쿼리 없음) */
    public static <T> Slice<T> toSlice(List<T> items, Pageable pageable) {
        boolean hasNext = items.size() > pageable.getPageSize();
        List<T> content = hasNext
                ? new ArrayList<>(items.subList(0, pageable.getPageSize()))
                : items;
        return new SliceImpl<>(content, pageable, hasNext);
    }
}