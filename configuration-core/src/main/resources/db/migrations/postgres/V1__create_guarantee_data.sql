CREATE TABLE IF NOT EXISTS guarantee_data (
    id BIGSERIAL PRIMARY KEY,
    signature BYTEA,
    requestValue VARCHAR(255),
    requestType VARCHAR(255),
    createdAt TIMESTAMP(6),
    polledAt TIMESTAMP(6),
    isSent BOOLEAN
);