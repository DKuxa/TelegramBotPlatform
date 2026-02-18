# Telegram Gateway Service

A Spring Boot microservice that acts as the single entry point between Telegram and your downstream bot workers.

## How it works

```
Telegram  ──long-poll──►  GatewayBot  ──►  telegram.updates  (RabbitMQ)
Telegram  ◄──send msg──   ReplyListener ◄──  telegram.replies (RabbitMQ)
```

1. `GatewayBot` receives every incoming `Update` via long-polling and publishes it as a `BotRequest` to the `telegram.updates` queue.
2. Worker services (separate microservices) consume from `telegram.updates`, apply business logic, and publish a `BotResponse` to `telegram.replies`.
3. `ReplyListener` consumes from `telegram.replies` and sends the reply back to the user via the Telegram API.

## RabbitMQ queues

| Queue | Purpose |
|---|---|
| `telegram.updates` | Outbound — gateway publishes every received `Update` here |
| `telegram.replies` | Inbound — gateway reads replies from here and sends them to Telegram |
| `telegram.replies.dlq` | Dead-letter queue — captures poison messages from `telegram.replies` |

## Configuration

All configuration is via environment variables.

| Variable | Default | Description |
|---|---|---|
| `GATEWAY_BOT_TOKEN` | *(required)* | Telegram bot token from @BotFather |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USER` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `RABBITMQ_VHOST` | `/` | RabbitMQ virtual host |
| `SERVER_PORT` | `8080` | HTTP server port |

## Running locally

**1. Start RabbitMQ**

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:4-management
```

**2. Run the service**

```bash
GATEWAY_BOT_TOKEN=<your_token> ./mvnw spring-boot:run
```

**3. Verify**

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Queues appear in the RabbitMQ management UI at `http://localhost:15672` (guest/guest).

## Running with Docker

```bash
docker build -t telegram-gateway .
docker run -e GATEWAY_BOT_TOKEN=<token> \
           -e RABBITMQ_HOST=<host> \
           -p 8080:8080 \
           telegram-gateway
```

## Message contracts

### BotRequest (published to `telegram.updates`)
```json
{
  "botToken": "123:ABC...",
  "update": { /* Telegram Update object */ }
}
```

### BotResponse (consumed from `telegram.replies`)
```json
{
  "botToken": "123:ABC...",
  "chatId": 123456789,
  "text": "Hello!"
}
```

## Tech stack

- Java 21, Spring Boot 3.5
- Spring AMQP (RabbitMQ)
- Telegram Bots SDK 9.2.1 (long-polling)
- Spring Boot Actuator
