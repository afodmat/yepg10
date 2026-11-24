CREATE TABLE learning_activities (
  id             BIGSERIAL PRIMARY KEY,
  title          VARCHAR(160) NOT NULL,
  description    TEXT NOT NULL,
  category       VARCHAR(60)  NOT NULL,
  level          VARCHAR(30)  NOT NULL DEFAULT 'Level 1',
  objectives     TEXT,
  facilitator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  start_date     DATE,
  end_date       DATE,
  duration_hours INT CHECK (duration_hours > 0),
  capacity       INT NOT NULL DEFAULT 200 CHECK (capacity > 0),
  lessons_total  INT NOT NULL DEFAULT 4 CHECK (lessons_total BETWEEN 1 AND 100),
  status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','OPEN','CLOSED','COMPLETED')),
  thumbnail_url  VARCHAR(300),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);
CREATE INDEX ix_activities_status ON learning_activities (status);
CREATE INDEX ix_activities_category ON learning_activities (category);

CREATE TABLE enrollments (
  id                BIGSERIAL PRIMARY KEY,
  user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  activity_id       BIGINT NOT NULL REFERENCES learning_activities(id) ON DELETE CASCADE,
  status            VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED','COMPLETED','CANCELLED')),
  progress_pct      SMALLINT NOT NULL DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
  completed_lessons INT NOT NULL DEFAULT 0 CHECK (completed_lessons >= 0),
  final_score       NUMERIC(5,2),
  enrolled_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at      TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, activity_id)
);
CREATE INDEX ix_enrollments_activity ON enrollments (activity_id, status);

CREATE TABLE assessments (
  id           BIGSERIAL PRIMARY KEY,
  activity_id  BIGINT NOT NULL UNIQUE REFERENCES learning_activities(id) ON DELETE CASCADE,
  title        VARCHAR(160) NOT NULL,
  pass_mark    SMALLINT NOT NULL DEFAULT 60 CHECK (pass_mark BETWEEN 1 AND 100),
  max_attempts SMALLINT NOT NULL DEFAULT 3 CHECK (max_attempts BETWEEN 1 AND 20),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE questions (
  id            BIGSERIAL PRIMARY KEY,
  assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
  text          VARCHAR(500) NOT NULL,
  points        SMALLINT NOT NULL DEFAULT 1 CHECK (points > 0),
  position      SMALLINT NOT NULL
);
CREATE INDEX ix_questions_assessment ON questions (assessment_id, position);
CREATE TABLE question_options (
  id          BIGSERIAL PRIMARY KEY,
  question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  text        VARCHAR(300) NOT NULL,
  correct     BOOLEAN NOT NULL DEFAULT FALSE,
  position    SMALLINT NOT NULL
);
CREATE INDEX ix_options_question ON question_options (question_id, position);
CREATE TABLE assessment_attempts (
  id            BIGSERIAL PRIMARY KEY,
  assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
  user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  score_pct     NUMERIC(5,2) NOT NULL CHECK (score_pct BETWEEN 0 AND 100),
  passed        BOOLEAN NOT NULL,
  submitted_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_attempts_user ON assessment_attempts (assessment_id, user_id);
CREATE TABLE attempt_answers (
  id          BIGSERIAL PRIMARY KEY,
  attempt_id  BIGINT NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
  question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  option_id   BIGINT REFERENCES question_options(id) ON DELETE SET NULL,
  correct     BOOLEAN NOT NULL
);
CREATE INDEX ix_attempt_answers_attempt ON attempt_answers (attempt_id);
