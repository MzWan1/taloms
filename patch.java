    public static <T> org.springframework.data.domain.Page<T> paginateList(java.util.List<T> list, Integer page, Integer pageSize) {
        int safeSize = pageSize != null ? Math.min(pageSize, 10) : 10; // Max 10 rows per page
        Pageable pageable = toPageable(page, safeSize, 10, Sort.unsorted());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, list.size());
        }
        return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
    }
