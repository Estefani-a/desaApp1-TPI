package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Each RF adds the queries it needs (filters of RF03, row lock of RF04…). */
public interface TripRepository extends JpaRepository<Trip, UUID>, JpaSpecificationExecutor<Trip> {

    /**
     * Reads the trip with {@code SELECT ... FOR UPDATE}: two transactions that change the
     * seats of the same trip run one after the other (RF04, CONVENCIONES §10.2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") UUID id);

    /**
     * "Mis viajes" (RF02, docs/API.md §5.5): the trips the user drives or has a reservation on, in
     * any status of the reservation, with the driver and the campus in the same query. A trip in
     * a {@code closed} status only comes if it left on {@code closedSince} or later.
     */
    @EntityGraph(attributePaths = {"driver", "campus"})
    @Query("""
            select t from Trip t
            where (t.driver.id = :userId
                   or exists (select r.id from Reservation r where r.trip = t and r.passenger.id = :userId))
              and (t.status not in :closed or t.schedule.departureDate >= :closedSince)
            order by t.schedule.departureDate, t.schedule.departureTime, t.id
            """)
    List<Trip> findMine(@Param("userId") UUID userId,
                        @Param("closed") Collection<TripStatus> closed,
                        @Param("closedSince") LocalDate closedSince);

    /**
     * Trip list of RF03 ({@link TripSpecifications}): one {@code SELECT} with the driver and the
     * campus joined, because every {@code TripResponseDTO} needs them. Unlike
     * {@code findAll(spec, Pageable)} it does not run a {@code COUNT}.
     */
    default List<Trip> findTrips(Specification<Trip> spec, Sort sort, Limit limit) {
        return findBy(spec, query -> {
            var sorted = query.sortBy(sort).project("driver", "campus");
            return limit.isLimited() ? sorted.limit(limit.max()).all() : sorted.all();
        });
    }
}
