CREATE TABLE app_user (
                          id BIGSERIAL PRIMARY KEY,
                          chat_id BIGINT UNIQUE NOT NULL,
                          username VARCHAR(255),
                          state VARCHAR(50) DEFAULT 'START',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appuser_chat_id ON app_user(chat_id);

CREATE TABLE action_log (
                            id BIGSERIAL PRIMARY KEY,
                            bot_name VARCHAR(100) NOT NULL,
                            chat_id BIGINT NOT NULL,
                            message_text TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_actionlog_created_at ON action_log(created_at);
CREATE INDEX idx_actionlog_chat_id ON action_log(chat_id);

CREATE TABLE error_log (
                           id BIGSERIAL PRIMARY KEY,
                           bot_name VARCHAR(100),
                           error_message TEXT NOT NULL,
                           stack_trace TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_errorlog_created_at ON error_log(created_at);