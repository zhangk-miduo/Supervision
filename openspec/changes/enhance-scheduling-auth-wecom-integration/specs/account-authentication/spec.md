# Account Authentication Specification

## ADDED Requirements

### Requirement: Account and person separation

The system SHALL model login accounts independently from synchronized organization persons, with an optional unique binding from an account to a person.

#### Scenario: Create an unbound administrator account

- **GIVEN** an administrator has account-management permission
- **WHEN** the administrator creates an account without selecting a person
- **THEN** the account SHALL be created successfully
- **AND** no organization person SHALL be created or modified

#### Scenario: Synchronize a new person

- **GIVEN** a new member exists in the enterprise WeCom directory
- **WHEN** directory synchronization completes
- **THEN** a Person SHALL be created or updated
- **AND** a login Account SHALL NOT be created automatically

### Requirement: First-login password change

Every administrator-created account SHALL require a password change after its first successful password verification and after every administrator password reset.

#### Scenario: First login with temporary password

- **GIVEN** an enabled account has `must_change_password=true`
- **WHEN** the user supplies the correct temporary password
- **THEN** the system SHALL issue only a password-change-limited session
- **AND** all other protected application APIs SHALL reject that session

#### Scenario: Complete mandatory password change

- **GIVEN** the user has a password-change-limited session
- **WHEN** the user sets a password that meets policy
- **THEN** the system SHALL replace the password hash, clear the mandatory-change flag, revoke prior sessions, and allow a normal login

### Requirement: Credential security

The system SHALL store passwords using an adaptive one-way password hash and SHALL never store, log, or return a plaintext password.

#### Scenario: Administrator resets a password

- **WHEN** an administrator resets an account password
- **THEN** the new password SHALL be hashed before persistence
- **AND** active sessions SHALL be revoked
- **AND** an audit record SHALL be created without the password value

### Requirement: Account protection and audit

The system SHALL support account enablement, role assignment, failed-login lockout, session revocation, and security audit records.

#### Scenario: Repeated failed logins

- **GIVEN** an enabled account has reached the configured failed-login threshold
- **WHEN** another login attempt occurs before `locked_until`
- **THEN** authentication SHALL be rejected without revealing whether the username or password was incorrect
- **AND** the event SHALL be audited

### Requirement: Departed-person handling

The system SHALL NOT automatically delete or disable an account solely because its bound Person was marked inactive by directory synchronization.

#### Scenario: Bound person leaves the organization

- **WHEN** a synchronized Person changes to inactive
- **THEN** the Person SHALL remain available for historical references
- **AND** the system SHALL create an administrator-visible security warning for any bound active account