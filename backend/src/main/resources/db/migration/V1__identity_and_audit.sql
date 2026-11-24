CREATE TABLE users (
  id               BIGSERIAL PRIMARY KEY,
  email            VARCHAR(254) NOT NULL,
  password_hash    VARCHAR(100) NOT NULL,
  role             VARCHAR(20)  NOT NULL DEFAULT 'PARTICIPANT'
                   CHECK (role IN ('PARTICIPANT','MENTOR','FACILITATOR','ADMIN')),
  status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','ACTIVE','DISABLED')),
  failed_attempts  INT          NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
  locked_until     TIMESTAMPTZ,
  last_login_at    TIMESTAMPTZ,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE INDEX ix_users_role_status ON users (role, status);

CREATE TABLE profiles (
  user_id       BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  first_name    VARCHAR(80)  NOT NULL,
  last_name     VARCHAR(80)  NOT NULL,
  phone         VARCHAR(30),
  country       VARCHAR(60)  NOT NULL,
  age           SMALLINT CHECK (age BETWEEN 16 AND 120),
  photo_file_id BIGINT,
  bio           VARCHAR(1000),
  skills        VARCHAR(500),
  interests     VARCHAR(500),
  education     VARCHAR(200),
  organization  VARCHAR(200),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked_at  TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE one_time_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type        VARCHAR(20) NOT NULL CHECK (type IN ('ACTIVATION','PASSWORD_RESET')),
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  used_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_one_time_tokens_user ON one_time_tokens (user_id, type);

CREATE TABLE audit_logs (
  id           BIGSERIAL PRIMARY KEY,
  actor_id     BIGINT,
  actor_email  VARCHAR(254),
  action       VARCHAR(60) NOT NULL,
  entity_type  VARCHAR(60),
  entity_id    VARCHAR(60),
  metadata     JSONB,
  ip_address   VARCHAR(45),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_created ON audit_logs (created_at DESC);
CREATE INDEX ix_audit_actor ON audit_logs (actor_id);
CREATE INDEX ix_audit_action ON audit_logs (action);
