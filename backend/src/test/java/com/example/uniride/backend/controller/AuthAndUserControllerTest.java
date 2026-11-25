package com.example.uniride.backend.controller;

import com.example.uniride.backend.config.SecurityConfig;
import com.example.uniride.backend.config.TimeConfig;
import com.example.uniride.backend.dto.AuthResponseDTO;
import com.example.uniride.backend.dto.MessageResponseDTO;
import com.example.uniride.backend.dto.UserResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.PreferredMode;
import com.example.uniride.backend.security.ApiErrorSecurityHandler;
import com.example.uniride.backend.security.CurrentUser;
import com.example.uniride.backend.security.JwtAuthenticationFilter;
import com.example.uniride.backend.security.JwtProperties;
import com.example.uniride.backend.security.JwtService;
import com.example.uniride.backend.service.AuthService;
import com.example.uniride.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class, AppLinkController.class})
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        ApiErrorSecurityHandler.class, CurrentUser.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "app.jwt.expiration=30d",
})
class AuthAndUserControllerTest {

    private static final UUID USER_ID = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    private String bearer() {
        return "Bearer " + jwtService.issue(USER_ID).accessToken();
    }

    @Test
    void registroValidoDevuelve201YMensaje() throws Exception {
        when(authService.register(any()))
                .thenReturn(new MessageResponseDTO("Te enviamos un correo para activar la cuenta."));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "lfernandez@uade.edu.ar",
                                  "password": "Secreta123",
                                  "fullName": "Lucía Fernández",
                                  "career": "Ingeniería en Informática",
                                  "phone": "+5491155550000",
                                  "preferredMode": "AMBOS",
                                  "vehicle": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Te enviamos un correo para activar la cuenta."));
    }

    @Test
    void registroConCorreoDeFormatoInvalidoDevuelve400ConFieldsEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "no-es-un-correo",
                                  "password": "Secreta123",
                                  "fullName": "Lucía Fernández",
                                  "career": "Ingeniería",
                                  "phone": "+5491155550000",
                                  "preferredMode": "PASAJERO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void registroConCamposVaciosDevuelve400ValidationError() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": "",
                                  "fullName": "",
                                  "career": "Ingeniería",
                                  "phone": "+5491155550000",
                                  "preferredMode": "PASAJERO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginExitosoDevuelve200ConTokenYUsuario() throws Exception {
        UserResponseDTO user = new UserResponseDTO(
                USER_ID, "Lucía Fernández", "lfernandez@uade.edu.ar",
                "Ingeniería en Informática", "+5491155550000",
                PreferredMode.AMBOS, 4.8, 12, null
        );
        when(authService.login(any()))
                .thenReturn(new AuthResponseDTO("jwt-123", Instant.parse("2026-12-21T00:40:12Z"), user));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lfernandez@uade.edu.ar\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-123"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-20T21:40:12-03:00"))
                .andExpect(jsonPath("$.user.email").value("lfernandez@uade.edu.ar"));
    }

    @Test
    void loginConCuentaNoActivadaDevuelve401AccountNotActivated() throws Exception {
        when(authService.login(any())).thenThrow(new ApiException(ErrorCode.ACCOUNT_NOT_ACTIVATED));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lfernandez@uade.edu.ar\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVATED"));
    }

    @Test
    void perfilSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void perfilConTokenDevuelve200() throws Exception {
        UserResponseDTO user = new UserResponseDTO(
                USER_ID, "Lucía Fernández", "lfernandez@uade.edu.ar",
                "Ingeniería en Informática", "+5491155550000",
                PreferredMode.AMBOS, 4.8, 12, null
        );
        when(userService.getProfile(USER_ID)).thenReturn(user);

        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.vehicle").isEmpty());
    }

    @Test
    void guardarDeviceTokenDevuelve204() throws Exception {
        mvc.perform(put("/api/v1/users/me/device-token")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"token-fcm-1\"}"))
                .andExpect(status().isNoContent());
        verify(userService).updateDeviceToken(USER_ID, "token-fcm-1");
    }

    @Test
    void borrarDeviceTokenDevuelve204() throws Exception {
        mvc.perform(delete("/api/v1/users/me/device-token").header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        verify(userService).deleteDeviceToken(USER_ID);
    }

    @Test
    void enlaceIntermedioDeActivacionEsPublicoYGeneraDeepLink() throws Exception {
        mvc.perform(get("/api/v1/links/activate").param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("uniride://activate?token=abc123")))
                .andExpect(content().string(containsString("Abrir UniRide")));
    }

    @Test
    void enlaceIntermedioSinTokenDevuelve400() throws Exception {
        mvc.perform(get("/api/v1/links/reset-password"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
