package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.Shift;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

/**
 * Conditions of the trip list (RF03, docs/API.md §5.1), one per filter, so the service combines
 * only the ones the request has. They are plain queries: the rules of what is listed live in
 * {@code TripService}.
 */
public final class TripSpecifications {

    private TripSpecifications() {
    }

    public static Specification<Trip> statusIn(Collection<TripStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /** Leaves at [now] or later. [now] is a date and time of the business zone. */
    public static Specification<Trip> notDepartedAt(LocalDateTime now) {
        return (root, query, cb) -> {
            Path<LocalDate> date = departureDate(root);
            Path<LocalTime> time = departureTime(root);
            return cb.or(
                    cb.greaterThan(date, now.toLocalDate()),
                    cb.and(cb.equal(date, now.toLocalDate()), cb.greaterThanOrEqualTo(time, now.toLocalTime())));
        };
    }

    public static Specification<Trip> updatedSince(Instant since) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), since);
    }

    public static Specification<Trip> campus(String campusCode) {
        return (root, query, cb) -> cb.equal(root.get("campus").get("code"), campusCode);
    }

    public static Specification<Trip> direction(Direction direction) {
        return (root, query, cb) -> cb.equal(root.get("route").get("direction"), direction);
    }

    public static Specification<Trip> departureDate(LocalDate date) {
        return (root, query, cb) -> cb.equal(departureDate(root), date);
    }

    /** The slot on any day. NOCHE goes through midnight, so it is "from 18:00 OR before 06:00". */
    public static Specification<Trip> shift(Shift shift) {
        return (root, query, cb) -> {
            Path<LocalTime> time = departureTime(root);
            Predicate fromStart = cb.greaterThanOrEqualTo(time, shift.start());
            Predicate beforeEnd = cb.lessThan(time, shift.end());
            return shift.wrapsMidnight() ? cb.or(fromStart, beforeEnd) : cb.and(fromStart, beforeEnd);
        };
    }

    /**
     * The slot that starts on [date]. For NOCHE it is [date] from 18:00 or the next day before
     * 06:00: the early morning of [date] belongs to the night of the day before.
     */
    public static Specification<Trip> departureDateAndShift(LocalDate date, Shift shift) {
        return (root, query, cb) -> {
            Path<LocalTime> time = departureTime(root);
            if (!shift.wrapsMidnight()) {
                return cb.and(onDate(root, cb, date),
                        cb.greaterThanOrEqualTo(time, shift.start()), cb.lessThan(time, shift.end()));
            }
            return cb.or(
                    cb.and(onDate(root, cb, date), cb.greaterThanOrEqualTo(time, shift.start())),
                    cb.and(onDate(root, cb, date.plusDays(1)), cb.lessThan(time, shift.end())));
        };
    }

    private static Predicate onDate(Root<Trip> root, CriteriaBuilder cb, LocalDate date) {
        return cb.equal(departureDate(root), date);
    }

    private static Path<LocalDate> departureDate(Root<Trip> root) {
        return root.get("schedule").get("departureDate");
    }

    private static Path<LocalTime> departureTime(Root<Trip> root) {
        return root.get("schedule").get("departureTime");
    }
}
