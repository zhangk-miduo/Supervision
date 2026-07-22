# WeCom Messaging Specification Delta

## MODIFIED Requirements

### Requirement: Managed group and push identities

The system SHALL model a logical WeCom group separately from one or more webhook push configurations and SHALL generate legacy-compatible system identifiers without requiring users to enter a RobotId.

#### Scenario: Create a push configuration

- **WHEN** an administrator provides a group name, push name, and valid webhook URL
- **THEN** the system SHALL create the logical group or associate the selected group with the push
- **AND** SHALL generate any required internal system code automatically

#### Scenario: Multiple pushes in one group

- **GIVEN** a logical group already has one push configuration
- **WHEN** an administrator adds another push with a distinct push name
- **THEN** both configurations SHALL remain independently testable and selectable
- **AND** their webhook secrets SHALL remain encrypted and masked

### Requirement: Multi-target group delivery

The system SHALL allow a group-webhook task to select one or more enabled push configurations and SHALL process each target independently.

#### Scenario: Select targets

- **WHEN** a creator opens the target selector
- **THEN** each option SHALL display the logical group name and push name
- **AND** the stable value SHALL be the push configuration identifier

#### Scenario: Select multiple pushes for the same group

- **WHEN** a creator selects more than one push associated with the same logical group
- **THEN** the editor SHALL warn that the group may receive duplicate messages
- **AND** SHALL require explicit confirmation if the selection is retained

#### Scenario: Partial target failure

- **GIVEN** a task execution has three target pushes
- **WHEN** two deliveries succeed and one fails
- **THEN** the execution SHALL be classified as partial success with a 2/3 summary
- **AND** each target SHALL retain its own delivery result

#### Scenario: Retry a partial execution

- **WHEN** retry policy retries a partial execution
- **THEN** it SHALL retry only retryable failed targets
- **AND** SHALL NOT resend a target with a successful delivery for the same target-level idempotency key

### Requirement: Legacy target compatibility

The system SHALL read existing tasks with a single `webhookId` and SHALL preserve legacy RobotId references during a documented compatibility period.

#### Scenario: Read a legacy task

- **WHEN** an existing task contains one `webhookId`
- **THEN** the system SHALL interpret it as a one-element target list
- **AND** editing and saving the task SHALL use the new multi-target representation
