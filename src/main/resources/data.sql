-- Migration: add source column to recommendations (idempotent — run once in Supabase if not already present)
ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS source VARCHAR(8) DEFAULT 'RULE';

INSERT INTO crop_parameters (crop_type, suggested_spacing, growth_cycle_days, optimal_temp_min, optimal_temp_max, humidity_min, humidity_max, ph_min, ph_max, ec_min, ec_max, irrigation_needs, recommended_fertilizer, planting_depth_cm) 
VALUES 
('BANANO', '2.5m x 2.5m', 270, 26.0, 30.0, 75.0, 85.0, 5.5, 7.0, 1.0, 2.0, '20-25 mm/day', 'NPK 15-5-20', 30.0),
('MANGO', '8m x 8m', 150, 24.0, 30.0, 50.0, 70.0, 5.5, 7.5, 0.5, 1.5, '100-150 L/week', 'NPK 10-20-20', 50.0),
('YUCA', '1m x 1m', 300, 25.0, 29.0, 60.0, 80.0, 5.5, 6.5, 0.5, 1.0, '10-15 mm/day', 'NPK 12-12-17', 15.0),
('PLATANO', '3m x 2m', 360, 25.0, 30.0, 70.0, 85.0, 5.5, 7.0, 1.0, 2.0, '20-30 mm/day', 'NPK 14-4-28', 40.0),
('MAIZ', '0.8m x 0.2m', 120, 20.0, 30.0, 50.0, 70.0, 5.8, 7.0, 1.5, 2.5, '5-10 mm/day', 'Urea 46-0-0', 5.0),
('PALMA', '9m x 9m', 1095, 24.0, 32.0, 80.0, 90.0, 4.0, 6.0, 1.0, 2.0, '150-200 L/day', 'NPKMg 12-12-17-2', 45.0)
ON CONFLICT (crop_type) DO NOTHING;
