package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.Shift;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Query parameters of {@code GET /trips} (docs/API.md §5.1). Every field is optional
 * ({@code null} means "any") and they combine with AND.
 *
 * @param campus       code of the campus, for example {@code MONSERRAT}
 * @param shift        with {@code date}, the slot that starts that day (NOCHE includes the early
 *                     morning of the next day); alone, the slot on any day
 * @param updatedSince only the trips changed since then, including the ones that were finished
 *                     or cancelled, so the app can remove them from Room
 */
public record TripFiltersRequestDTO(
        String campus,
        Direction direction,
        LocalDate date,
        Shift shift,
        OffsetDateTime updatedSince
) {
}
