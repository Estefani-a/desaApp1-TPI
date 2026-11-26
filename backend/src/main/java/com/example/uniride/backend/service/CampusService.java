package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.CampusResponseDTO;
import com.example.uniride.backend.repository.CampusRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UADE campuses and their meeting points (docs/API.md §5.2). They come from {@code data.sql}. */
@Service
public class CampusService {

    private final CampusRepository campusRepository;

    public CampusService(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    /** Every campus, sorted by name, with its meeting points in the order of {@code data.sql}. */
    @Transactional(readOnly = true)
    public List<CampusResponseDTO> list() {
        return campusRepository.findAll(Sort.by("name")).stream()
                .map(CampusResponseDTO::from)
                .toList();
    }
}
