-- Datos de demo: se cargan después de data.sql, solo con el perfil "demo"
-- (application-demo.yml). Igual que data.sql, todo INSERT es idempotente.

-- =============================================================================
-- Usuarios de prueba, ya activados (para pedir un token sin pasar por el correo).
-- Contraseña de todos: UniRide2026. Es pública (está en el README): por eso este archivo
-- solo se carga con el perfil "demo", nunca en un despliegue real.
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
-- RF03 · Viajes de ejemplo para el feed. Las fechas son relativas al día en que se crea la
-- base (current_date + n): si la base es vieja y ya pasaron, docker compose down -v las renueva.
-- =============================================================================
INSERT INTO trips (id, driver_id, campus_code, direction, meeting_point, destination_neighborhood,
                   destination_reference, departure_date, departure_time, estimated_arrival_time,
                   total_seats, available_seats, contribution_per_seat,
                   vehicle_brand, vehicle_model, vehicle_color, vehicle_plate,
                   status, created_at, updated_at) VALUES
    ('0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31', '6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10', 'MONSERRAT', 'VUELTA',
     'Salida Lima 717', 'Ramos Mejía', 'Estación de tren', current_date + 1, '22:15', '23:00',
     3, 3, 3500, 'Volkswagen', 'Gol Trend', 'Gris', 'AB123CD', 'ABIERTO', now(), now()),
    ('1a9d3e2f-4c8b-4d6e-9f01-2b3c4d5e6f70', '3b8d1f6a-9c2e-4d7b-8e1a-5f0c3a6d9b24', 'BELGRANO', 'IDA',
     'Salida 11 de Septiembre 1990', 'Palermo', 'Plaza Italia', current_date + 2, '08:00', '08:30',
     2, 2, 2500, 'Fiat', 'Cronos', 'Blanco', 'AE456FG', 'ABIERTO', now(), now()),
    ('2b8e4f1a-5d9c-4e7f-8a12-3c4d5e6f7081', '6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10', 'RECOLETA', 'VUELTA',
     'Salida Libertad 1340', 'Caballito', 'Parque Rivadavia', current_date + 2, '15:30', '16:10',
     2, 2, 2000, 'Volkswagen', 'Gol Trend', 'Gris', 'AB123CD', 'ABIERTO', now(), now()),
    ('3c7f5a2b-6e0d-4f8a-9b23-4d5e6f708192', '3b8d1f6a-9c2e-4d7b-8e1a-5f0c3a6d9b24', 'MONSERRAT', 'IDA',
     'Salida Chile 1142', 'Avellaneda', NULL, current_date + 3, '07:15', '07:50',
     3, 3, 0, 'Fiat', 'Cronos', 'Blanco', 'AE456FG', 'ABIERTO', now(), now())
ON CONFLICT DO NOTHING;
