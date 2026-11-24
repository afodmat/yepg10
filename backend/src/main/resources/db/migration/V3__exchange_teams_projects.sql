CREATE TABLE exchange_sessions (
  id                  BIGSERIAL PRIMARY KEY,
  title               VARCHAR(160) NOT NULL,
  description         TEXT NOT NULL,
  topic               VARCHAR(100) NOT NULL,
  starts_at           TIMESTAMPTZ NOT NULL,
  duration_minutes    INT NOT NULL DEFAULT 60 CHECK (duration_minutes BETWEEN 15 AND 480),
  facilitator_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  meeting_link        VARCHAR(300),
  status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')),
  discussion_prompts  TEXT,
  resources           TEXT,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_sessions_starts ON exchange_sessions (starts_at);
CREATE TABLE exchange_participants (
  id            BIGSERIAL PRIMARY KEY,
  session_id    BIGINT NOT NULL REFERENCES exchange_sessions(id) ON DELETE CASCADE,
  user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  country       VARCHAR(60) NOT NULL,
  registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (session_id, user_id)
);

CREATE TABLE teams (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(80) NOT NULL,
  description VARCHAR(1000),
  leader_id   BIGINT NOT NULL REFERENCES users(id),
  country     VARCHAR(60),
  institution VARCHAR(160),
  category    VARCHAR(60),
  open        BOOLEAN NOT NULL DEFAULT TRUE,
  status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_teams_name ON teams (lower(name));
CREATE TABLE team_members (
  id          BIGSERIAL PRIMARY KEY,
  team_id     BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  member_role VARCHAR(10) NOT NULL DEFAULT 'MEMBER' CHECK (member_role IN ('LEADER','MEMBER')),
  title       VARCHAR(60),
  joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (team_id, user_id)
);
CREATE INDEX ix_team_members_user ON team_members (user_id);
CREATE TABLE team_invitations (
  id         BIGSERIAL PRIMARY KEY,
  team_id    BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  email      VARCHAR(254) NOT NULL,
  invited_by BIGINT NOT NULL REFERENCES users(id),
  status     VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_invitations_email ON team_invitations (lower(email), status);

CREATE TABLE impact_projects (
  id                BIGSERIAL PRIMARY KEY,
  team_id           BIGINT NOT NULL UNIQUE REFERENCES teams(id) ON DELETE CASCADE,
  name              VARCHAR(120) NOT NULL,
  description       TEXT NOT NULL,
  problem_statement TEXT,
  solution          TEXT,
  category          VARCHAR(40) NOT NULL,
  location          VARCHAR(120),
  leader_id         BIGINT NOT NULL REFERENCES users(id),
  status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT','ACTIVE','SUBMITTED','COMPLETED')),
  progress_pct      SMALLINT NOT NULL DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
  start_date        DATE,
  target_date       DATE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_projects_category ON impact_projects (category, status);
CREATE TABLE project_milestones (
  id           BIGSERIAL PRIMARY KEY,
  project_id   BIGINT NOT NULL REFERENCES impact_projects(id) ON DELETE CASCADE,
  name         VARCHAR(80) NOT NULL,
  position     SMALLINT NOT NULL,
  status       VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','DONE')),
  completed_at TIMESTAMPTZ
);
CREATE INDEX ix_milestones_project ON project_milestones (project_id, position);
CREATE TABLE project_updates (
  id         BIGSERIAL PRIMARY KEY,
  project_id BIGINT NOT NULL REFERENCES impact_projects(id) ON DELETE CASCADE,
  author_id  BIGINT NOT NULL REFERENCES users(id),
  content    VARCHAR(2000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_updates_project ON project_updates (project_id, created_at DESC);
CREATE TABLE project_comments (
  id         BIGSERIAL PRIMARY KEY,
  project_id BIGINT NOT NULL REFERENCES impact_projects(id) ON DELETE CASCADE,
  author_id  BIGINT NOT NULL REFERENCES users(id),
  content    VARCHAR(2000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_comments_project ON project_comments (project_id, created_at DESC);
