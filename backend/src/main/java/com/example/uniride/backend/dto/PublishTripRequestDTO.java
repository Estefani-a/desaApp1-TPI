package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.Direction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body of {@code PUT /trips/{tripId}} (docs/API.md §5.4). Only the rules that need nothing else
 * are here; the ones about now, the campus and the driver's vehicle are in the service.
 */
public record PublishTripRequestDTO(
        @NotNull(message = "El sentido es obligatorio.")
        Direction direction,

        @NotBlank(message = "La sede es obligatoria.")
        String campus,

        @NotBlank(message = "El punto de encuentro es obligatorio.")
        String meetingPoint,

        @NotBlank(message = "El barrio de destino es obligatorio.")
        @Size(max = 100, message = "Máximo 100 caracteres.")
        String destinationNeighborhood,

        @Size(max = 150, message = "Máximo 150 caracteres.")
        String destinationReference,

        @NotNull(message = "La fecha de salida es obligatoria.")
        LocalDate departureDate,

        @NotNull(message = "La hora de salida es obligatoria.")
        LocalTime departureTime,

        LocalTime estimatedArrivalTime,

        @NotNull(message = "Las plazas son obligatorias.")
        @Min(value = 1, message = "Debe estar entre 1 y 4.")
        @Max(value = 4, message = "Debe estar entre 1 y 4.")
        Integer totalSeats,

        @NotNull(message = "El aporte es obligatorio.")
        @PositiveOrZero(message = "No puede ser negativo.")
        Integer contributionPerSeat
) {
}
