# WeCom Directory Synchronization Specification

## ADDED Requirements

### Requirement: Managed WeCom configuration

The system SHALL provide administrator-only settings for CorpId, directory credential, application AgentId and Secret, callback settings, and group webhook configurations.

#### Scenario: Read saved configuration

- **GIVEN** valid secrets have been saved
- **WHEN** an administrator opens WeCom settings
- **THEN** the API SHALL return masked values and configured-state indicators
- **AND** SHALL NOT return the complete Secret or webhook key

### Requirement: Configuration verification

The system SHALL allow an administrator to verify connectivity, credentials, application identity, and available directory scope before synchronization.

#### Scenario: Application visibility is insufficient

- **WHEN** the configured application cannot read the expected organization scope
- **THEN** verification SHALL fail with an actionable permission or visibility-range diagnosis
- **AND** the Secret SHALL not appear in the response or logs

### Requirement: WeCom-only organization source

Departments and persons SHALL be created and updated only through WeCom synchronization; the management UI and public API SHALL NOT provide manual create operations for these records.

#### Scenario: Administrator views personnel management

- **WHEN** an administrator opens personnel management
- **THEN** the UI SHALL provide search, detail, synchronization, and synchronization-history actions
- **AND** SHALL NOT provide an add-person or add-department action

### Requirement: Idempotent full synchronization

The system SHALL upsert departments, persons, and multi-department membership by stable WeCom identifiers and SHALL produce the same local state when the same snapshot is synchronized repeatedly.

#### Scenario: Synchronize the same snapshot twice

- **GIVEN** a successful directory snapshot has already been applied
- **WHEN** the same snapshot is applied again
- **THEN** no duplicate department, person, or membership records SHALL be created
- **AND** the synchronization log SHALL accurately report unchanged records

### Requirement: Safe deactivation

The system SHALL mark missing departments and persons inactive only after a complete successful snapshot; it SHALL preserve historical records and SHALL NOT perform convergence after a partial failure.

#### Scenario: Person-fetch request fails midway

- **GIVEN** department retrieval succeeded but a person page failed
- **WHEN** the synchronization terminates
- **THEN** the run SHALL be recorded as failed or partial
- **AND** previously active local persons absent from the partial data SHALL remain active

### Requirement: Synchronization observability

The system SHALL prevent concurrent full synchronization and record start time, end time, operator, status, counts, and sanitized error details.

#### Scenario: Concurrent synchronization request

- **GIVEN** a full synchronization holds the synchronization lock
- **WHEN** another administrator starts synchronization
- **THEN** the second request SHALL be rejected as already running
- **AND** SHALL NOT start duplicate WeCom API traversal