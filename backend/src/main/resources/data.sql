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
