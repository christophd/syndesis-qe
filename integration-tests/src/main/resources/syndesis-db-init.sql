CREATE DATABASE syndesis;
CREATE USER syndesis WITH PASSWORD 'secret';
GRANT ALL PRIVILEGES ON DATABASE syndesis to syndesis;

CREATE TABLE IF NOT EXISTS contact (first_name VARCHAR, last_name VARCHAR, company VARCHAR, lead_source VARCHAR, create_date DATE);
CREATE TABLE IF NOT EXISTS todo (id SERIAL PRIMARY KEY, task VARCHAR, completed INTEGER);