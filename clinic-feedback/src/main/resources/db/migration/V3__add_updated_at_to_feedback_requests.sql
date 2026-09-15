-- V3: Add updated_at to feedback_requests

ALTER TABLE feedback_requests
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
