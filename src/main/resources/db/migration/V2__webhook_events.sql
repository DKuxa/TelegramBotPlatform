-- Migration for adding webhook_event table
-- Stores audit of all incoming webhooks from Radarr, Sonarr, TrueNAS

CREATE TABLE webhook_event (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    event_type VARCHAR(100),
    payload TEXT,
    processed_successfully BOOLEAN NOT NULL DEFAULT false,
    error_message TEXT,
    routed_to_bot VARCHAR(100),
    target_channel_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query optimization
CREATE INDEX idx_webhook_event_source ON webhook_event(source);
CREATE INDEX idx_webhook_event_created_at ON webhook_event(created_at);
CREATE INDEX idx_webhook_event_processed ON webhook_event(processed_successfully);
CREATE INDEX idx_webhook_event_source_date ON webhook_event(source, created_at);

-- Table comments
COMMENT ON TABLE webhook_event IS 'Audit of incoming webhook events from external systems (Radarr, Sonarr, TrueNAS)';
COMMENT ON COLUMN webhook_event.source IS 'Webhook source: radarr, sonarr, truenas';
COMMENT ON COLUMN webhook_event.event_type IS 'Event type: Grab, Download, CRITICAL, etc.';
COMMENT ON COLUMN webhook_event.payload IS 'Raw JSON payload for debugging';
COMMENT ON COLUMN webhook_event.routed_to_bot IS 'Name of the bot that handled the request';
COMMENT ON COLUMN webhook_event.target_channel_id IS 'Target channel/chat ID';
