CREATE TABLE IF NOT EXISTS guarantee_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    signature LONGBLOB,
    requestValue VARCHAR(255),
    requestType VARCHAR(255),
    createdAt DATETIME(6),
    polledAt DATETIME(6),
    isSent BOOLEAN
);