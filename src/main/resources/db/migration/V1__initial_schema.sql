CREATE TABLE school_domains (
    id BIGINT NOT NULL AUTO_INCREMENT,
    domain VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_school_domain UNIQUE (domain)
) ENGINE=InnoDB;

CREATE TABLE stacks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stack_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE users (
    admin BIT NOT NULL,
    grade INTEGER,
    rating_avg DOUBLE NOT NULL,
    rating_count INTEGER NOT NULL,
    verified BIT NOT NULL,
    deleted_at DATETIME(6),
    nickname_changed_at DATETIME(6),
    school_domain_id BIGINT,
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    bio VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    field VARCHAR(255),
    major VARCHAR(255),
    nick_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    profile_url VARCHAR(255),
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255),
    verified_email VARCHAR(255),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_nickname UNIQUE (nick_name),
    CONSTRAINT uk_user_verified_email UNIQUE (verified_email),
    CONSTRAINT fk_user_school_domain FOREIGN KEY (school_domain_id) REFERENCES school_domains (id)
) ENGINE=InnoDB;

CREATE TABLE user_stacks (
    stack_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_user_stack_stack FOREIGN KEY (stack_id) REFERENCES stacks (id),
    CONSTRAINT fk_user_stack_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_user UNIQUE (user_id),
    CONSTRAINT uk_refresh_token UNIQUE (token)
) ENGINE=InnoDB;

CREATE TABLE email_verifications (
    expired_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    code VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    type ENUM ('SIGNUP', 'UNIVERSITY', 'WITHDRAW') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_email_type UNIQUE (email, type),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE job_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    category_code ENUM ('DESIGN', 'DEVELOPMENT', 'MARKETING', 'PLANNING') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_job_category_code UNIQUE (category_code)
) ENGINE=InnoDB;

CREATE TABLE job_role (
    is_custom BIT NOT NULL,
    category_id BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_job_role_category FOREIGN KEY (category_id) REFERENCES job_category (id)
) ENGINE=InnoDB;

CREATE TABLE projects (
    view_count INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    creator_id BIGINT,
    deleted_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    recruitment_deadline DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    communication_tool VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    meeting_schedule VARCHAR(255),
    period VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    collaboration_type ENUM ('BOTH', 'OFFLINE', 'ONLINE') NOT NULL,
    goal ENUM ('COMPETITION', 'PORTFOLIO', 'PRODUCTION') NOT NULL,
    status ENUM ('COMPLETED', 'IN_PROGRESS', 'RECRUITING', 'RECRUITMENT_CLOSED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_creator FOREIGN KEY (creator_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_recruitment_deadline ON projects (recruitment_deadline);
CREATE INDEX idx_project_status_deadline ON projects (status, deleted_at, recruitment_deadline);

CREATE TABLE project_recruitments (
    applicant_count INTEGER NOT NULL,
    recruitment_count INTEGER NOT NULL,
    deleted_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_role_id BIGINT,
    project_id BIGINT,
    preferred VARCHAR(255) NOT NULL,
    qualification VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recruitment_job_role FOREIGN KEY (job_role_id) REFERENCES job_role (id),
    CONSTRAINT fk_recruitment_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB;

CREATE TABLE project_questions (
    order_index INTEGER NOT NULL,
    required BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT,
    label VARCHAR(255) NOT NULL,
    options VARCHAR(255),
    type ENUM ('MULTI_CHOICE', 'SINGLE_CHOICE', 'TEXT') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_question_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB;

CREATE TABLE project_applications (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    recruitment_id BIGINT,
    user_id BIGINT,
    letter VARCHAR(255) NOT NULL,
    status ENUM ('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_application_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_application_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_application_recruitment FOREIGN KEY (recruitment_id) REFERENCES project_recruitments (id),
    CONSTRAINT fk_project_application_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE application_answers (
    application_id BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT,
    answer_text VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_application_answer_application FOREIGN KEY (application_id) REFERENCES project_applications (id),
    CONSTRAINT fk_application_answer_question FOREIGN KEY (question_id) REFERENCES project_questions (id)
) ENGINE=InnoDB;

CREATE TABLE project_members (
    completed_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    joined_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    recruitment_id BIGINT,
    user_id BIGINT NOT NULL,
    role ENUM ('MEMBER', 'OWNER') NOT NULL,
    status ENUM ('ACTIVE', 'COMPLETED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_member_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_member_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_member_recruitment FOREIGN KEY (recruitment_id) REFERENCES project_recruitments (id),
    CONSTRAINT fk_project_member_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE member_reviews (
    contribution INTEGER NOT NULL,
    participation INTEGER NOT NULL,
    responsibility INTEGER NOT NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    hidden_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    comment VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT chk_review_contribution CHECK (contribution BETWEEN 1 AND 5),
    CONSTRAINT chk_review_participation CHECK (participation BETWEEN 1 AND 5),
    CONSTRAINT chk_review_responsibility CHECK (responsibility BETWEEN 1 AND 5),
    CONSTRAINT uk_member_review_project_author_target UNIQUE (project_id, author_id, target_id),
    CONSTRAINT fk_member_review_author FOREIGN KEY (author_id) REFERENCES users (user_id),
    CONSTRAINT fk_member_review_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_member_review_target FOREIGN KEY (target_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_member_review_target_created
    ON member_reviews (target_id, created_at);

CREATE INDEX idx_member_review_author_created
    ON member_reviews (author_id, created_at);

CREATE TABLE posts (
    comment_count INTEGER NOT NULL,
    like_count INTEGER NOT NULL,
    view_count INTEGER NOT NULL,
    author_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    content TEXT NOT NULL,
    title VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_posts_active_created
    ON posts (deleted_at, created_at, id);
CREATE INDEX idx_posts_author_active_created
    ON posts (author_id, deleted_at, created_at, id);

CREATE TABLE post_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    viewer_key VARCHAR(80) NOT NULL,
    viewed_hour DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_view_hourly UNIQUE (post_id, viewer_key, viewed_hour),
    CONSTRAINT fk_post_view_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB;

CREATE TABLE comments (
    depth INTEGER NOT NULL,
    author_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT,
    post_id BIGINT,
    content TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users (user_id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments (id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB;

CREATE INDEX idx_comments_post_created
    ON comments (post_id, created_at, id);
CREATE INDEX idx_comments_author_active_post
    ON comments (author_id, deleted_at, post_id);
CREATE INDEX idx_comments_parent
    ON comments (parent_id);

CREATE TABLE post_likes (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_like_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_like_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_post_likes_post
    ON post_likes (post_id);

CREATE TABLE post_saves (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_save_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_post_save_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_save_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_post_saves_post
    ON post_saves (post_id);

CREATE TABLE notifications (
    is_read BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    ref_id BIGINT,
    user_id BIGINT NOT NULL,
    content TEXT,
    target_url VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    type ENUM (
        'APPLICATION_APPROVED',
        'APPLICATION_RECEIVED',
        'APPLICATION_REJECTED',
        'COMMENT_REPLY',
        'DEADLINE_REACHED',
        'POST_COMMENT'
    ) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE device_tokens (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_token UNIQUE (token),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_device_token_user ON device_tokens (user_id);

CREATE TABLE project_chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_chat_message_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_chat_message_sender FOREIGN KEY (sender_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_project_chat_message_project_id
    ON project_chat_messages (project_id, id);

CREATE TABLE project_chat_reads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_chat_read_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_chat_read_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_chat_read_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250),
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250),
    NEXT_FIRE_TIME BIGINT,
    PREV_FIRE_TIME BIGINT,
    PRIORITY INTEGER,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT,
    CALENDAR_NAME VARCHAR(190),
    MISFIRE_INSTR SMALLINT,
    JOB_DATA BLOB,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_trigger_job FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_simple_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_cron_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512),
    STR_PROP_2 VARCHAR(512),
    STR_PROP_3 VARCHAR(512),
    INT_PROP_1 INTEGER,
    INT_PROP_2 INTEGER,
    LONG_PROP_1 BIGINT,
    LONG_PROP_2 BIGINT,
    DEC_PROP_1 NUMERIC(13,4),
    DEC_PROP_2 NUMERIC(13,4),
    BOOL_PROP_1 VARCHAR(1),
    BOOL_PROP_2 VARCHAR(1),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_simprop_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA BLOB,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_blob_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(190),
    JOB_GROUP VARCHAR(190),
    IS_NONCONCURRENT VARCHAR(1),
    REQUESTS_RECOVERY VARCHAR(1),
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
