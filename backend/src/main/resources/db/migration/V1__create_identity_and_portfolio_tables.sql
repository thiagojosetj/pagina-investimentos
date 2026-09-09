CREATE TABLE app_user (
    id UUID NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT ck_app_user_display_name_not_blank
        CHECK (display_name = btrim(display_name) AND char_length(display_name) BETWEEN 1 AND 120),
    CONSTRAINT ck_app_user_version_non_negative CHECK (version >= 0),
    CONSTRAINT ck_app_user_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE external_identity (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_verified BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_external_identity PRIMARY KEY (id),
    CONSTRAINT fk_external_identity_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT uq_external_identity_provider_subject UNIQUE (provider, subject),
    CONSTRAINT uq_external_identity_user_provider UNIQUE (user_id, provider),
    CONSTRAINT ck_external_identity_provider_format
        CHECK (provider ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_external_identity_subject_not_blank
        CHECK (subject = btrim(subject) AND char_length(subject) BETWEEN 1 AND 255),
    CONSTRAINT ck_external_identity_email_not_blank
        CHECK (email = btrim(email) AND char_length(email) BETWEEN 1 AND 320),
    CONSTRAINT ck_external_identity_version_non_negative CHECK (version >= 0),
    CONSTRAINT ck_external_identity_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE portfolio (
    id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    base_currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_portfolio PRIMARY KEY (id),
    CONSTRAINT fk_portfolio_owner
        FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_portfolio_name_not_blank
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 1 AND 100),
    CONSTRAINT ck_portfolio_base_currency_brl CHECK (base_currency = 'BRL'),
    CONSTRAINT ck_portfolio_version_non_negative CHECK (version >= 0),
    CONSTRAINT ck_portfolio_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX ix_portfolio_owner_user_id ON portfolio (owner_user_id);
CREATE UNIQUE INDEX uq_portfolio_owner_name_ci
    ON portfolio (owner_user_id, lower(name));

CREATE TABLE allocation_class (
    id UUID NOT NULL,
    portfolio_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    display_order SMALLINT NOT NULL,
    target_percentage NUMERIC(7, 4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_allocation_class PRIMARY KEY (id),
    CONSTRAINT fk_allocation_class_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE RESTRICT,
    CONSTRAINT uq_allocation_class_portfolio_order UNIQUE (portfolio_id, display_order),
    CONSTRAINT ck_allocation_class_name_not_blank
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 1 AND 80),
    CONSTRAINT ck_allocation_class_display_order_non_negative CHECK (display_order >= 0),
    CONSTRAINT ck_allocation_class_target_percentage
        CHECK (target_percentage BETWEEN 0.0000 AND 100.0000),
    CONSTRAINT ck_allocation_class_version_non_negative CHECK (version >= 0),
    CONSTRAINT ck_allocation_class_timestamps CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uq_allocation_class_portfolio_name_ci
    ON allocation_class (portfolio_id, lower(name));
