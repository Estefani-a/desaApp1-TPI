package com.example.uniride.backend.controller;

import com.example.uniride.backend.dto.CampusResponseDTO;
import com.example.uniride.backend.service.CampusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Campuses (docs/API.md §5.2). Public: the app needs them before the login. */
@RestController
@RequestMapping("/api/v1/campuses")
@Tag(name = "Sedes")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    @Operation(summary = "Sedes y sus puntos de encuentro")
    public List<CampusResponseDTO> list() {
        return campusService.list();
    }
}
