package com.ai.vidya.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Standard paginated response wrapper.
 *
 * {
 *   "content":       [...],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 143,
 *   "totalPages":    8,
 *   "first":         true,
 *   "last":          false
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private final List<T> content;
    private final int     page;
    private final int     size;
    private final long    totalElements;
    private final int     totalPages;
    private final boolean first;
    private final boolean last;
}
