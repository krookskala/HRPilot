-- V9__user_password_nullable.sql
-- Allow NULL password_hash for invited users who haven't set a password yet.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
