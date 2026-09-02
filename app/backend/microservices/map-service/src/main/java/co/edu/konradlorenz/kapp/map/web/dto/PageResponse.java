package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The {@code PageResponse} schema, and the only paginated collection in this API.
 *
 * <p>Hand-rolled rather than returning Spring Data's {@code Page}: that class serialises a
 * large envelope of {@code pageable}, {@code sort} and {@code numberOfElements} fields that
 * are not in the contract, and its JSON shape is explicitly not guaranteed across versions.
 *
 * @param totalPages 0 when nothing matched, per the spec
 * @param last       true for an empty result, which is a normal answer rather than an error
 */
@Schema(name = "PageResponse")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1);
    }
}
