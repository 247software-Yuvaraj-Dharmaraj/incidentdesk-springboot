-- IncidentDesk schema (mirrors the original Prisma schema, idiomatic snake_case).

CREATE TABLE users (
    id            VARCHAR(30)  PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'REPORTER' CHECK (role IN ('ADMIN', 'REPORTER')),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE incidents (
    id          VARCHAR(30)  PRIMARY KEY,
    title       VARCHAR(140) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'INCIDENT' CHECK (type IN ('INCIDENT', 'REQUEST', 'MAINTENANCE')),
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'   CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN'     CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    description TEXT,
    due_date    TIMESTAMP,
    resolved_at TIMESTAMP,
    reporter_id VARCHAR(30)  NOT NULL REFERENCES users (id),
    assignee_id VARCHAR(30)  REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_incidents_reporter ON incidents (reporter_id);
CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_created_at ON incidents (created_at);

CREATE TABLE audit_logs (
    id          VARCHAR(30) PRIMARY KEY,
    incident_id VARCHAR(30) NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    actor_id    VARCHAR(30) NOT NULL REFERENCES users (id),
    field       VARCHAR(50) NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_incident ON audit_logs (incident_id);

CREATE TABLE comments (
    id          VARCHAR(30) PRIMARY KEY,
    incident_id VARCHAR(30) NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    author_id   VARCHAR(30) NOT NULL REFERENCES users (id),
    body        TEXT        NOT NULL,
    internal    BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_incident ON comments (incident_id);
