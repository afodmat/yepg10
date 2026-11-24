CREATE TABLE achievements (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(40) NOT NULL UNIQUE,
  name        VARCHAR(80) NOT NULL,
  description VARCHAR(300) NOT NULL,
  icon        VARCHAR(20) NOT NULL DEFAULT 'trophy',
  criteria    VARCHAR(300) NOT NULL,
  active      BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE user_achievements (
  id             BIGSERIAL PRIMARY KEY,
  user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  achievement_id BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
  awarded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, achievement_id)
);
CREATE SEQUENCE certificate_seq START 1;
CREATE TABLE certificates (
  id                 BIGSERIAL PRIMARY KEY,
  user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  activity_id        BIGINT NOT NULL REFERENCES learning_activities(id) ON DELETE CASCADE,
  certificate_number VARCHAR(30) NOT NULL UNIQUE,
  verification_code  VARCHAR(20) NOT NULL UNIQUE,
  issuer             VARCHAR(120) NOT NULL DEFAULT 'Youth Empowerment Project',
  status             VARCHAR(10) NOT NULL DEFAULT 'VALID' CHECK (status IN ('VALID','REVOKED')),
  issued_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, activity_id)
);
CREATE TABLE notifications (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       VARCHAR(30) NOT NULL,
  title      VARCHAR(160) NOT NULL,
  body       VARCHAR(500),
  link       VARCHAR(100),
  read_at    TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user ON notifications (user_id, created_at DESC);
CREATE TABLE notification_preferences (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type    VARCHAR(30) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (user_id, type)
);
CREATE TABLE files (
  id            BIGSERIAL PRIMARY KEY,
  owner_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  purpose       VARCHAR(20) NOT NULL CHECK (purpose IN ('PROFILE_PHOTO','PROJECT_DOC','PROJECT_IMAGE','LEARNING_RESOURCE')),
  original_name VARCHAR(150) NOT NULL,
  stored_name   VARCHAR(80) NOT NULL UNIQUE,
  content_type  VARCHAR(100) NOT NULL,
  size_bytes    BIGINT NOT NULL CHECK (size_bytes > 0),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_files_owner ON files (owner_id);
CREATE TABLE project_attachments (
  project_id BIGINT NOT NULL REFERENCES impact_projects(id) ON DELETE CASCADE,
  file_id    BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
  PRIMARY KEY (project_id, file_id)
);
CREATE TABLE platform_settings (
  setting_key   VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(500) NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_photo FOREIGN KEY (photo_file_id) REFERENCES files(id) ON DELETE SET NULL;

INSERT INTO achievements (code, name, description, icon, criteria) VALUES
 ('DIGITAL_PIONEER','Digital Pioneer','Completed your first learning activity','spark','Complete 1 learning activity'),
 ('LEARNING_CHAMPION','Learning Champion','Completed three learning activities','trophy','Complete 3 learning activities'),
 ('VIRTUAL_EXCHANGE_PARTICIPANT','Virtual Exchange Participant','Registered for a Virtual Exchange session','globe','Register for 1 Virtual Exchange session'),
 ('TEAM_BUILDER','Team Builder','Created or joined a team','team','Be a member of a team'),
 ('THOUGHTFUL_CONTRIBUTOR','Thoughtful Contributor','Shared three project updates or comments','message','Post 3 project updates or comments'),
 ('PROJECT_INNOVATOR','Project Innovator','Led an Impact Project','leaf','Lead an Impact Project');

INSERT INTO platform_settings (setting_key, setting_value) VALUES
 ('platform.name','Youth Empowerment Project'),
 ('registration.open','true'),
 ('team.max_members','6'),
 ('certificate.issuer','Youth Empowerment Project');
