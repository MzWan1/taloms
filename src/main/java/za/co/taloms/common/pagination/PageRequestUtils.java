package za.co.taloms.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import za.co.taloms.common.ApplicationConstants;

/**
 * Centralised, clamped page-parameter parsing for list endpoints.
 *
 * Contract for every paginated TALOMS list API:
 *   ?page=1&page_size=20
 *
 * Uses the existing {@link ApplicationConstants}:
 *   default page size : 20 (DEFAULT_PAGE_SIZE)
 *   maximum page size : 100 (MAX_PAGE_SIZE)
 *   minimum page size : 1
 *
 * A client can NEVER request an unbounded result set: anything above the
 * maximum is clamped (not rejected), so existing clients sending large sizes
 * still get correct, bounded responses. Page numbers below one are clamped
 * to one. Page numbers beyond the data simply return an empty page.
 */
public final class PageRequestUtils {

    private PageRequestUtils() {}

    /**
     * @param page     1-based page number (defaults to 1)
     * @param pageSize requested page size (defaults to {@code defaultSize})
     */
    public static Pageable toPageable(Integer page, Integer pageSize, int defaultSize, Sort sort) {
        int safePage = page == null ? 0 : Math.max(0, page - 1);
        int safeSize = pageSize == null ? defaultSize
                : Math.max(1, Math.min(pageSize, ApplicationConstants.MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, sort);
    }


    public static <T> org.springframework.data.domain.Page<T> paginateList(java.util.List<T> list, Integer page, Integer pageSize) {
        int safeSize = pageSize != null ? Math.min(pageSize, 10) : 10;
        Pageable pageable = toPageable(page, safeSize, 10, Sort.unsorted());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, list.size());
        }
        return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    public static Pageable toPageable(Integer page, Integer pageSize) {
        return toPageable(page, pageSize, ApplicationConstants.DEFAULT_PAGE_SIZE, Sort.unsorted());
    }
}

