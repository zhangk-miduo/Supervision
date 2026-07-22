# Advanced Scheduling Specification Delta

## ADDED Requirements

### Requirement: National workday scheduling

The system SHALL provide a `WORKDAY` schedule mode based on an approved annual China national workday calendar, including statutory holidays and weekend make-up workdays.

#### Scenario: Statutory holiday on a weekday

- **GIVEN** a date is marked as a holiday in the approved annual calendar
- **WHEN** a workday task reaches its candidate trigger time
- **THEN** the task SHALL NOT execute
- **AND** the skip reason SHALL be recorded as a calendar decision

#### Scenario: Weekend make-up workday

- **GIVEN** a Saturday or Sunday is marked as a workday by the approved annual calendar
- **WHEN** a workday task reaches its configured time
- **THEN** the task SHALL execute normally

#### Scenario: Preview workday schedule

- **WHEN** a user requests the next five times for a workday rule
- **THEN** the preview SHALL use the same calendar decision service as runtime execution
- **AND** SHALL include make-up workdays and exclude holidays

### Requirement: Calendar provenance and coverage

The system SHALL version annual calendar data with source, publication metadata, checksum, import time, and coverage status.

#### Scenario: Calendar year is unavailable

- **GIVEN** the candidate date belongs to a year without approved calendar coverage
- **WHEN** the system previews or evaluates a workday task
- **THEN** it SHALL return an unknown-calendar result instead of assuming Monday through Friday
- **AND** SHALL show or emit an actionable administrator warning

#### Scenario: Import annual calendar data

- **WHEN** an authorized administrator imports a reviewed annual data package
- **THEN** the system SHALL validate date uniqueness, year consistency, checksum, and source metadata
- **AND** SHALL audit the import before marking the year available
