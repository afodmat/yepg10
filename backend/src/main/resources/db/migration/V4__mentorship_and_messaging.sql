CREATE TABLE mentor_profiles (
  user_id      BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  expertise    VARCHAR(300) NOT NULL,
  availability VARCHAR(200),
  bio          VARCHAR(1000),
  max_mentees  SMALLINT NOT NULL DEFAULT 10 CHECK (max_mentees BETWEEN 1 AND 100),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE mentorship_assignments (
  id             BIGSERIAL PRIMARY KEY,
  mentor_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  participant_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
  team_id        BIGINT REFERENCES teams(id) ON DELETE CASCADE,
  status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ENDED')),
  assigned_by    BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((participant_id IS NOT NULL) <> (team_id IS NOT NULL))
);
CREATE INDEX ix_assign_mentor ON mentorship_assignments (mentor_id, status);
CREATE UNIQUE INDEX uq_assign_participant ON mentorship_assignments (mentor_id, participant_id) WHERE status = 'ACTIVE' AND participant_id IS NOT NULL;
CREATE UNIQUE INDEX uq_assign_team ON mentorship_assignments (mentor_id, team_id) WHERE status = 'ACTIVE' AND team_id IS NOT NULL;
CREATE TABLE mentorship_requests (
  id             BIGSERIAL PRIMARY KEY,
  participant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  mentor_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message        VARCHAR(1000),
  status         VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED')),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_mreq_mentor ON mentorship_requests (mentor_id, status);

CREATE TABLE conversations (
  id         BIGSERIAL PRIMARY KEY,
  type       VARCHAR(10) NOT NULL CHECK (type IN ('DIRECT','TEAM')),
  team_id    BIGINT UNIQUE REFERENCES teams(id) ON DELETE CASCADE,
  direct_key VARCHAR(40) UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE conversation_participants (
  conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  last_read_at    TIMESTAMPTZ,
  PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX ix_conv_part_user ON conversation_participants (user_id);
CREATE TABLE messages (
  id              BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id       BIGINT NOT NULL REFERENCES users(id),
  body            VARCHAR(2000) NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_messages_conv ON messages (conversation_id, id DESC);
