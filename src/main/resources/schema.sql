-- Drop tables if they exist to support clean local initializations
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS portfolios;
DROP TABLE IF EXISTS traders;

CREATE TABLE traders (
                         id VARCHAR(50) PRIMARY KEY
);

CREATE TABLE portfolios (
                            id BIGSERIAL PRIMARY KEY, -- Using PostgreSQL's auto-incrementing Big Serial
                            trader_id VARCHAR(50) NOT NULL,
                            stock VARCHAR(10) NOT NULL,
                            sector VARCHAR(50) NOT NULL,
                            quantity INT NOT NULL CHECK (quantity >= 0),
                            CONSTRAINT uk_trader_stock UNIQUE (trader_id, stock), -- Maintains unique stock line item per trader
                            CONSTRAINT fk_portfolio_trader FOREIGN KEY (trader_id) REFERENCES traders(id) ON DELETE CASCADE
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY, -- Replaced AUTO_INCREMENT with BIGSERIAL
                        trader_id VARCHAR(50) NOT NULL,
                        stock VARCHAR(10) NOT NULL,
                        sector VARCHAR(50) NOT NULL,
                        quantity INT NOT NULL CHECK (quantity > 0),
                        side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
                        status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED')),
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- Optimized for global timezone support
                        CONSTRAINT fk_order_trader FOREIGN KEY (trader_id) REFERENCES traders(id) ON DELETE CASCADE
);

-- Index for concurrent pending order checks
CREATE INDEX idx_orders_trader_status ON orders(trader_id, status);

INSERT INTO traders (id) VALUES ('T001') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;

INSERT INTO traders (id) VALUES ('T002') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;

INSERT INTO traders (id) VALUES ('T003') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;

INSERT INTO traders (id) VALUES ('T003') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;

INSERT INTO traders (id) VALUES ('T004') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;

INSERT INTO traders (id) VALUES ('T005') ON CONFLICT DO NOTHING;
INSERT INTO traders (id) VALUES ('CONCUR_T01') ON CONFLICT DO NOTHING;