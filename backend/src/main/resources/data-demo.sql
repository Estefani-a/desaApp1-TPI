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
