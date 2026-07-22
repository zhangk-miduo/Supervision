# Advanced Scheduling Specification

## ADDED Requirements

### Requirement: Visual recurrence rules

The system SHALL allow a task creator to configure manual, one-time, interval, daily, weekly, monthly, or advanced Cron scheduling without requiring Cron for common recurrence patterns.

#### Scenario: Configure a weekly task

- **GIVEN** an authorized user is editing a task
- **WHEN** the user selects weekly scheduling, chooses Monday and Friday, and selects 09:00
- **THEN** the system SHALL save the semantic weekly rule and register the corresponding Quartz triggers
- **AND** the user SHALL not be required to enter a Cron expression

### Requirement: Schedule preview

The system SHALL validate a schedule in its configured timezone and return at least the next five fire times before the task is saved.

#### Scenario: Preview a monthly schedule

- **GIVEN** a user configured execution on the final day of each month at 18:00 Asia/Shanghai
- **WHEN** the user requests a preview
- **THEN** the system SHALL display the next five valid execution timestamps
- **AND** invalid or impossible rules SHALL return a field-level validation error

### Requirement: Runtime policies

The system SHALL support start and end boundaries, timezone, misfire policy, overlap policy, timeout, and retry policy independently from the recurrence rule.

#### Scenario: Previous execution is still running

- **GIVEN** a task uses the overlap policy `SKIP`
- **AND** its previous execution still holds the task execution lock
- **WHEN** the next trigger fires
- **THEN** the system SHALL record the occurrence as skipped
- **AND** SHALL NOT start a concurrent execution

### Requirement: Durable semantic storage

The system SHALL store the versioned user recurrence rule as the source of truth and treat Cron or Quartz triggers as compiled artifacts.

#### Scenario: Edit an existing visual rule

- **GIVEN** a daily task was created through the visual editor
- **WHEN** the user reopens the task
- **THEN** the editor SHALL reconstruct the original daily configuration from the stored semantic rule
- **AND** SHALL NOT require reverse parsing a Cron string

### Requirement: Legacy schedule migration

The system SHALL migrate existing manual and Cron tasks without changing their effective behavior.

#### Scenario: Restart after migration

- **GIVEN** an enabled legacy Cron task has been migrated
- **WHEN** the application restarts
- **THEN** the task SHALL be restored with the same effective Cron schedule
- **AND** SHALL have exactly one effective execution for each due occurrence