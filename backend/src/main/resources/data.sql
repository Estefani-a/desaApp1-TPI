-- Datos iniciales de UniRide. Corre en cada arranque (spring.sql.init.mode=always) después de
-- que Hibernate crea las tablas, así que todo INSERT tiene que poder repetirse sin error:
-- siempre con ON CONFLICT DO NOTHING (CONVENCIONES §10.5).
-- Cada RF agrega sus datos al final de su sección.

-- =============================================================================
-- Sedes y puntos de encuentro (docs/API.md §5.2)
-- =============================================================================
INSERT INTO campuses (code, name) VALUES
    ('MONSERRAT', 'Sede Monserrat'),
    ('BELGRANO', 'Sede Belgrano'),
    ('RECOLETA', 'Sede Recoleta')
ON CONFLICT DO NOTHING;

INSERT INTO campus_meeting_points (campus_code, position, meeting_point) VALUES
    ('MONSERRAT', 0, 'Salida Lima 717'),
    ('MONSERRAT', 1, 'Salida Lima 757'),
    ('MONSERRAT', 2, 'Salida Chile 1142'),
    ('MONSERRAT', 3, 'Salida Independencia 1072'),
    ('BELGRANO', 0, 'Salida 11 de Septiembre 1990'),
    ('BELGRANO', 1, 'Esquina Echeverría'),
    ('RECOLETA', 0, 'Salida Libertad 1340')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Usuarios de prueba, ya activados (para pedir un token sin pasar por el correo).
-- Contraseña de todos: UniRide2026
-- =============================================================================
INSERT INTO users (id, email, password_hash, full_name, career, phone, preferred_mode, status,
                   rating_avg, rating_count, vehicle_brand, vehicle_model, vehicle_color, vehicle_plate,
                   created_at, updated_at) VALUES
    ('6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10', 'conductora.demo@uade.edu.ar',
     '$2a$10$QAsotLuwdrPyhyPsDD351uUtPB7924q0HNx4VpMpVs4.QnDPiombe',
     'Lucía Fernández', 'Ingeniería en Informática', '+5491155550000', 'AMBOS', 'ACTIVA',
     4.8, 12, 'Volkswagen', 'Gol Trend', 'Gris', 'AB123CD', now(), now()),
    ('9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58', 'pasajero.demo@uade.edu.ar',
     '$2a$10$QAsotLuwdrPyhyPsDD351uUtPB7924q0HNx4VpMpVs4.QnDPiombe',
     'Martín Gómez', 'Licenciatura en Administración', '+5491155550001', 'PASAJERO', 'ACTIVA',
     4.5, 3, NULL, NULL, NULL, NULL, now(), now()),
    ('3b8d1f6a-9c2e-4d7b-8e1a-5f0c3a6d9b24', 'conductor2.demo@uade.edu.ar',
     '$2a$10$QAsotLuwdrPyhyPsDD351uUtPB7924q0HNx4VpMpVs4.QnDPiombe',
     'Sofía Ruiz', 'Diseño Gráfico', '+5491155550002', 'CONDUCTOR', 'ACTIVA',
     NULL, 0, 'Fiat', 'Cronos', 'Blanco', 'AE456FG', now(), now())
ON CONFLICT DO NOTHING;

-- =============================================================================
-- RF01 · Autenticación y perfil
-- =============================================================================

-- =============================================================================
-- RF02 · Publicación de viajes
-- =============================================================================

-- =============================================================================
-- RF03 · Explorar viajes
-- =============================================================================

-- =============================================================================
-- RF04 · Reservas
-- =============================================================================
-- Un pasajero tiene como máximo una reserva activa por viaje (ALREADY_RESERVED). El service
-- lo controla con el viaje bloqueado; este índice es la red de seguridad en la base.
CREATE UNIQUE INDEX IF NOT EXISTS uk_reservations_active_passenger
    ON reservations (trip_id, passenger_id)
    WHERE status IN ('PENDIENTE', 'ACEPTADA');
