package com.example.uniride.backend.controller;

import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import com.example.uniride.backend.dto.CampusResponseDTO;
import com.example.uniride.backend.security.ApiErrorSecurityHandler;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.security.JwtAuthenticationFilter;
import com.example.uniride.backend.security.JwtProperties;
import com.example.uniride.backend.security.JwtService;
import com.example.uniride.backend.service.CampusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /campuses}: public and with the format of docs/API.md §5.2. */
@WebMvcTest(CampusController.class)
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class CampusControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CampusService campusService;

    @Test
    void lasSedesSonPublicasYTraenSusPuntosDeEncuentro() throws Exception {
        when(campusService.list()).thenReturn(List.of(
                new CampusResponseDTO("MONSERRAT", "Sede Monserrat", List.of("Salida Lima 717", "Salida Lima 757"))));

        mvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MONSERRAT"))
                .andExpect(jsonPath("$[0].name").value("Sede Monserrat"))
                .andExpect(jsonPath("$[0].meetingPoints[1]").value("Salida Lima 757"));
    }
}
