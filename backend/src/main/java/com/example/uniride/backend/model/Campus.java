package com.example.uniride.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * UADE campus with its predefined meeting points (docs/API.md §5.2). Loaded from
 * {@code data.sql}; the app does not create campuses.
 */
@Entity
@Table(name = "campuses")
public class Campus {

    /** {@code MONSERRAT}, {@code BELGRANO}, {@code RECOLETA}. */
    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "campus_meeting_points", joinColumns = @JoinColumn(name = "campus_code"))
    @OrderColumn(name = "position")
    @Column(name = "meeting_point", nullable = false, length = 100)
    private List<String> meetingPoints = new ArrayList<>();

    protected Campus() {
    }

    public Campus(String code, String name, List<String> meetingPoints) {
        this.code = code;
        this.name = name;
        this.meetingPoints = new ArrayList<>(meetingPoints);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<String> getMeetingPoints() {
        return List.copyOf(meetingPoints);
    }
}
