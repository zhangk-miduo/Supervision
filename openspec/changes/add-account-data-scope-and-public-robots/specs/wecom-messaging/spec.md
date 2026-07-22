# WeCom Messaging Specification Delta

## ADDED Requirements

### Requirement: Private and public robot visibility

Every group webhook SHALL have an owning account and a private-or-public visibility state, with private as the default.

#### Scenario: Create a robot without choosing visibility

- **WHEN** an account creates a robot without explicitly making it public
- **THEN** the robot SHALL be private
- **AND** no other normal account SHALL see or select it

#### Scenario: Owner publishes a robot

- **GIVEN** an account owns an enabled valid robot
- **WHEN** the owner marks it public
- **THEN** other accounts SHALL be able to see its non-sensitive selectable summary
- **AND** SHALL be able to select it for task delivery

#### Scenario: Public state is scoped to a webhook

- **GIVEN** a logical group contains multiple message-push webhooks
- **WHEN** the owner publishes one webhook
- **THEN** only that webhook SHALL become publicly usable
- **AND** other webhooks in the logical group SHALL retain their own visibility

### Requirement: Public use without configuration control

Publishing a robot SHALL grant delivery use but SHALL NOT grant configuration read or mutation permissions to non-owners.

#### Scenario: Non-owner uses a public robot

- **GIVEN** another account owns a public enabled robot
- **WHEN** the current account saves or executes a task targeting it
- **THEN** the target SHALL pass usability validation
- **AND** delivery SHALL be attributed to the current task and its owner

#### Scenario: Non-owner attempts to modify a public robot

- **GIVEN** a robot is public but owned by another account
- **WHEN** the current account attempts to edit, disable, delete, change visibility, or run a robot configuration test
- **THEN** the operation SHALL be rejected
- **AND** administrator read visibility SHALL NOT change this result

#### Scenario: Non-owner views a public robot

- **WHEN** a non-owner requests a public robot through a permitted public view or selector
- **THEN** the response SHALL contain only its stable identifier, group and push names, public state, status, and creator presentation fields
- **AND** SHALL NOT contain a webhook URL, ciphertext, key, masked secret placeholder, internal remark, or mutable configuration

### Requirement: Runtime public-robot authorization

The system SHALL validate robot ownership or public availability when a task is saved, when a task-level test is sent, and immediately before every actual delivery.

#### Scenario: Save a task using another account's private robot

- **WHEN** an account submits another account's private webhook identifier
- **THEN** the task change SHALL be rejected as an unavailable target
- **AND** the system SHALL NOT reveal the private robot's configuration or owner-only detail

#### Scenario: Public access is revoked after task save

- **GIVEN** a task owned by one account references another account's public robot
- **WHEN** the robot owner makes it private before a later execution
- **THEN** the later execution SHALL NOT send to that webhook
- **AND** SHALL record a stable redacted failure indicating that sharing was revoked
- **AND** SHALL NOT substitute another robot

#### Scenario: Public robot is disabled after task save

- **GIVEN** another account's task references a public robot
- **WHEN** the owner disables the robot
- **THEN** subsequent test and scheduled deliveries SHALL stop
- **AND** SHALL create an auditable unavailable-target result

### Requirement: Sharing-impact warning

Before a robot owner revokes public availability or disables a robot, the system SHALL report the number of enabled tasks owned by other accounts that reference it and SHALL require explicit confirmation in the user interface.

#### Scenario: Revoke a robot used by other accounts

- **GIVEN** a public robot is referenced by enabled tasks owned by other accounts
- **WHEN** its owner begins making it private or disabling it
- **THEN** the interface SHALL show the affected external-task count
- **AND** SHALL explain that their future deliveries will stop
- **AND** SHALL require confirmation before submitting the change

### Requirement: Shared robot audit and rate protection

Public robot use SHALL retain per-webhook delivery limits and SHALL be attributable to the account that owns the calling task.

#### Scenario: Multiple accounts use one public robot

- **WHEN** tasks from multiple accounts send through the same public webhook
- **THEN** all sends SHALL share that webhook's configured rate limit
- **AND** each execution and delivery SHALL identify the calling task owner without exposing credentials

## MODIFIED Requirements

### Requirement: Target validation and delivery consistency

The system SHALL persist a stable webhook identifier and SHALL validate that it resolves to the same enabled encrypted webhook configuration that is either owned by the calling account or currently public during task save, task-level test, and delivery.

#### Scenario: Save a task with an unavailable or unauthorized target

- **WHEN** a creator submits a missing, disabled, invalid, privately owned by another account, or no-longer-public webhook identifier
- **THEN** the API SHALL reject the task change with an actionable non-sensitive validation error
- **AND** SHALL NOT substitute another target

#### Scenario: Target authorization changes after task save

- **GIVEN** a task references a webhook that was valid and usable when saved
- **WHEN** the configuration is disabled or its public access is revoked before execution
- **THEN** the delivery SHALL fail without calling another webhook
- **AND** SHALL create an auditable failure record identifying the unavailable target without leaking credentials
