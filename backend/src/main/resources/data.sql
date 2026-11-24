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
    ('BELGRANO', 1, 'Salida Echeverría'),
    ('RECOLETA', 0, 'Salida Libertad 1340')
-- Si un punto cambia de nombre, la base que ya existía se actualiza sola al arrancar.
ON CONFLICT (campus_code, position) DO UPDATE SET meeting_point = EXCLUDED.meeting_point;

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
