ALTER TABLE email_verifications
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE project_applications
    MODIFY COLUMN letter VARCHAR(500) NOT NULL;
