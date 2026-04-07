-- Dark mode preference for users
ALTER TABLE users ADD COLUMN dark_mode BOOLEAN NOT NULL DEFAULT FALSE;

-- Employee self-service contact fields
ALTER TABLE employees ADD COLUMN phone VARCHAR(30);
ALTER TABLE employees ADD COLUMN address VARCHAR(500);
ALTER TABLE employees ADD COLUMN emergency_contact_name VARCHAR(100);
ALTER TABLE employees ADD COLUMN emergency_contact_phone VARCHAR(30);
