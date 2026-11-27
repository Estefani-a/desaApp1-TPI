package com.example.uniride.backend.dto;

import com.example.uniride.backend.model.Campus;

import java.util.List;

/** Campus with its predefined meeting points (docs/API.md §5.2). */
public record CampusResponseDTO(
        String code,
        String name,
        List<String> meetingPoints
) {

    public static CampusResponseDTO from(Campus campus) {
        return new CampusResponseDTO(campus.getCode(), campus.getName(), campus.getMeetingPoints());
    }
}
