# WeCom Messaging Specification Delta

## MODIFIED Requirements

### Requirement: Separate delivery channels

The system SHALL distinguish group webhook delivery from WeCom application-message delivery, SHALL expose only recipient modes supported by the selected channel, and SHALL load selectable targets independently so a failure in one recipient source does not hide another available delivery channel.

#### Scenario: Person source fails while group targets are available

- **GIVEN** at least one enabled group webhook has a valid encrypted configuration
- **WHEN** the person-directory request fails while the group-target request succeeds
- **THEN** the task editor SHALL still display the available group target
- **AND** SHALL show the person-directory error only in the affected person-selection area

#### Scenario: Refresh target groups when creating a task

- **GIVEN** an administrator has added and enabled a valid group webhook
- **WHEN** a user opens or reopens the task wizard or enters the message-delivery step
- **THEN** the editor SHALL request the current selectable group targets
- **AND** the user SHALL NOT need to reload the entire application

#### Scenario: Exclude an invalid group target

- **WHEN** a robot is disabled or has no enabled decryptable webhook configuration
- **THEN** it SHALL NOT appear as a selectable target for a new task
- **AND** the API SHALL NOT expose its webhook URL, key, ciphertext, or secret

### Requirement: Target validation and delivery consistency

The system SHALL persist a stable webhook identifier and SHALL validate that it resolves to the same enabled encrypted webhook configuration during task save and delivery.

#### Scenario: Save a task with an unavailable target

- **WHEN** a creator submits a missing, disabled, or invalid webhook identifier
- **THEN** the API SHALL reject the task change with an actionable validation error
- **AND** SHALL NOT substitute another target

#### Scenario: Target is disabled after task save

- **GIVEN** a task references a webhook that was valid when saved
- **WHEN** the configuration is disabled before execution
- **THEN** the delivery SHALL fail without calling another webhook
- **AND** SHALL create an auditable failure record identifying the unavailable target without leaking credentials
