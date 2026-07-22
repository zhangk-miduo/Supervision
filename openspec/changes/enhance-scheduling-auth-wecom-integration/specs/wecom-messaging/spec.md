# WeCom Messaging Specification

## ADDED Requirements

### Requirement: Separate delivery channels

The system SHALL distinguish group webhook delivery from WeCom application-message delivery and SHALL expose only recipient modes supported by the selected channel.

#### Scenario: Select application message

- **WHEN** a task creator selects the application-message channel
- **THEN** the editor SHALL allow members, departments, and supported tags as recipients
- **AND** SHALL NOT present group-wide @all as an application-message feature

#### Scenario: Select group webhook

- **WHEN** a task creator selects a group webhook
- **THEN** the editor SHALL allow no mention, selected-person mention, or @all
- **AND** SHALL warn that organization membership does not prove membership in the target group

### Requirement: Structured message types

The system SHALL use validated structured editors and a canonical message model for text, markdown, markdown_v2, image, news, file, voice, and template_card instead of requiring end users to edit raw WeCom JSON.

#### Scenario: Create a news message

- **GIVEN** the task creator selects a news message
- **WHEN** the creator adds between one and eight valid articles
- **THEN** the system SHALL validate titles, links, descriptions, and images
- **AND** SHALL generate the WeCom payload through the channel adapter

### Requirement: Mention capability enforcement

The system SHALL enforce message-type-specific mention capabilities and persist recipients by stable WeCom userid.

#### Scenario: Mention selected people in text

- **GIVEN** selected active persons have WeCom userids
- **WHEN** a text group message is sent
- **THEN** their userids SHALL be mapped to the supported mention field
- **AND** the delivery record SHALL retain a recipient snapshot

#### Scenario: Select @all

- **WHEN** the creator selects @all
- **THEN** @all SHALL be mutually exclusive with individual mention selection
- **AND** the preview SHALL clearly show that every member of the target group will be reminded

#### Scenario: Unsupported markdown_v2 mention

- **WHEN** a creator selects markdown_v2 and requests an unsupported mention mode
- **THEN** the editor and API SHALL reject or disable that combination before sending

### Requirement: Preview and test delivery

The system SHALL provide a channel-accurate preview and an administrator-authorized test-send action before a task is enabled.

#### Scenario: Test-send a template card

- **GIVEN** a valid webhook and card definition
- **WHEN** the user sends a test
- **THEN** the system SHALL create a test delivery record and show the normalized WeCom result
- **AND** failure SHALL not enable or execute the task

### Requirement: Delivery audit and idempotency

Every send attempt SHALL create or update a delivery record containing channel, target snapshot, message type, idempotency key, task and execution references, result code, status, failure reason, retry count, and timestamps.

#### Scenario: Duplicate execution request

- **GIVEN** a successful delivery already exists for the same idempotency key
- **WHEN** the same task execution attempts the same logical delivery again
- **THEN** the system SHALL NOT send a duplicate message
- **AND** SHALL return the existing delivery result

### Requirement: Rate limiting and media lifecycle

The system SHALL enforce the documented group-webhook rate limit of 20 messages per minute per webhook and SHALL manage file or voice media identifiers without reusing expired identifiers.

#### Scenario: Webhook rate is exhausted

- **WHEN** a new send would exceed the configured webhook rate
- **THEN** the system SHALL defer or reject the send according to policy
- **AND** SHALL record a rate-limited delivery state rather than silently dropping it

### Requirement: Secret protection

The system SHALL encrypt webhook URLs and application secrets at rest, mask them in APIs, and remove them from application logs and error messages.

#### Scenario: WeCom returns an error containing the request URL

- **WHEN** the client records the failure
- **THEN** the persisted and logged error SHALL redact the webhook key and credentials