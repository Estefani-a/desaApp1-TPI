package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Vehicle DTO shared by registration, profile update and UserResponseDTO (docs/API.md §3.2). */
public record VehicleDTO(
        @NotBlank(message = "La marca es obligatoria.")
        @Size(max = 50, message = "Máximo 50 caracteres.")
        String brand,

        @NotBlank(message = "El modelo es obligatorio.")
        @Size(max = 50, message = "Máximo 50 caracteres.")
        String model,

        @NotBlank(message = "El color es obligatorio.")
        @Size(max = 30, message = "Máximo 30 caracteres.")
        String color,

        @NotBlank(message = "La patente es obligatoria.")
        @Size(max = 10, message = "Máximo 10 caracteres.")
        String plate
) {
    public static VehicleDTO from(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleDTO(
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getPlate()
        );
    }

    public Vehicle toEntity() {
        return new Vehicle(brand.trim(), model.trim(), color.trim(), plate.trim().toUpperCase());
    }
}
