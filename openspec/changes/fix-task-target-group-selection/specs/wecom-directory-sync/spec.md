# WeCom Directory Synchronization Specification Delta

## ADDED Requirements

### Requirement: Backward-compatible organization schema migration

The system SHALL upgrade previously existing organization tables to the current person, department, and membership schema without deleting organization records or changing referenced primary keys.

#### Scenario: Upgrade a legacy person table

- **GIVEN** `supervision_person` exists without the current tenant and WeCom identity columns
- **WHEN** the new Flyway migration runs
- **THEN** required columns SHALL be added and deterministically backfilled from existing data
- **AND** current person-list queries SHALL execute successfully after migration

#### Scenario: Upgrade an already current schema

- **GIVEN** the organization tables already contain the required columns and indexes
- **WHEN** the compatibility migration runs
- **THEN** it SHALL complete without duplicating columns, indexes, departments, persons, or memberships

#### Scenario: Legacy data cannot satisfy a unique constraint

- **GIVEN** legacy rows contain missing or conflicting values needed by the current unique identity
- **WHEN** the migration validates data before applying the constraint
- **THEN** the migration SHALL fail with a diagnostic that identifies the conflict category
- **AND** SHALL NOT silently discard, merge, or renumber organization records
