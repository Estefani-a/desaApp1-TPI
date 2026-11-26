package com.example.uniride.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Locale;
import java.util.Objects;

/** Vehicle of a driver, stored in the {@code users} table (docs/API.md §3.2). */
@Embeddable
public class Vehicle {

    @Column(name = "vehicle_brand", length = 50)
    private String brand;

    @Column(name = "vehicle_model", length = 50)
    private String model;

    @Column(name = "vehicle_color", length = 30)
    private String color;

    @Column(name = "vehicle_plate", length = 10)
    private String plate;

    protected Vehicle() {
    }

    public Vehicle(String brand, String model, String color, String plate) {
        this.brand = Objects.requireNonNull(brand).trim();
        this.model = Objects.requireNonNull(model).trim();
        this.color = Objects.requireNonNull(color).trim();
        this.plate = Objects.requireNonNull(plate).trim().toUpperCase(Locale.ROOT);
    }

    /** Embeddables cannot be shared between two entities: each one keeps its own copy. */
    public Vehicle copy() {
        return new Vehicle(brand, model, color, plate);
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public String getPlate() {
        return plate;
    }
}
