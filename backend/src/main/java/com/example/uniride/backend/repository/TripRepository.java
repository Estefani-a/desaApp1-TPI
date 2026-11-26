package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
