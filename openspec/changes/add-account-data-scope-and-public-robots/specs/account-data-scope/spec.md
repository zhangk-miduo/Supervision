# Account Data Scope Specification

## ADDED Requirements

### Requirement: Trusted account ownership

The system SHALL assign ownership of account-scoped business records from the authenticated server-side session and SHALL NOT trust a client-supplied creator identifier for authorization or persistence.

#### Scenario: Create an owned task

- **GIVEN** an authenticated account creates a task
- **WHEN** the task is persisted
- **THEN** its owner account identifier SHALL equal the authenticated account identifier
- **AND** any client-supplied `createdBy` or owner value SHALL be ignored or rejected

#### Scenario: Create an owned robot

- **GIVEN** an authenticated account creates a group webhook
- **WHEN** the webhook and logical group are persisted
- **THEN** both records SHALL be owned by that authenticated account
- **AND** the webhook SHALL be private by default

### Requirement: Account-scoped reads

The system SHALL limit non-administrator reads of robots, tasks, and executions to records owned by the authenticated account, except for the explicitly public robot summaries needed for permitted use.

#### Scenario: List records as a normal account

- **GIVEN** two accounts own different tasks and executions
- **WHEN** either non-administrator lists tasks or executions
- **THEN** only records owned by that account SHALL contribute to records and pagination totals

#### Scenario: Read another account's private record by identifier

- **GIVEN** a non-administrator knows the identifier of another account's private robot, task, or execution
- **WHEN** the account requests its detail
- **THEN** the system SHALL respond as if the resource does not exist
- **AND** SHALL NOT reveal whether the identifier is valid

#### Scenario: Administrator reads all account-scoped records

- **GIVEN** an authenticated administrator
- **WHEN** the administrator lists robots, tasks, or executions
- **THEN** records owned by every account and unassigned migration records SHALL be visible
- **AND** the administrator MAY filter the list by creator account

### Requirement: Owner-only mutation

The system SHALL require the authenticated account to own a robot or task before using ordinary mutation operations, and administrator read scope SHALL NOT implicitly bypass this ownership requirement.

#### Scenario: Administrator views but cannot edit another account's robot

- **GIVEN** an administrator is not the owner of a robot
- **WHEN** the administrator views the robot through the all-records view
- **THEN** the robot SHALL be visible with sensitive values protected
- **AND** ordinary update, visibility change, disable, delete, and configuration-test operations SHALL be rejected

#### Scenario: Account mutates its own task

- **GIVEN** an authenticated account owns a task
- **WHEN** it updates, deletes, or manually executes that task and satisfies applicable role permissions
- **THEN** the ownership check SHALL succeed

### Requirement: Execution ownership inheritance

Every execution record SHALL persist the owner of its task when the execution record is created, independently of whether the trigger is manual or scheduled.

#### Scenario: Scheduled task executes without a user request

- **GIVEN** a scheduled task has an owner account
- **WHEN** Quartz starts or skips an execution without an HTTP session
- **THEN** the execution record SHALL copy the task owner account identifier
- **AND** subsequent access SHALL be scoped by the copied owner

#### Scenario: Task is deleted after execution

- **GIVEN** an execution copied its task owner
- **WHEN** the task is later physically deleted
- **THEN** the historical execution SHALL retain its owner
- **AND** account isolation SHALL NOT depend on joining the deleted task

#### Scenario: Account manually executes a task

- **WHEN** an account manually executes its own task
- **THEN** the execution SHALL retain the task owner as its data owner
- **AND** MAY separately record the triggering account for audit

### Requirement: Creator identity presentation

Robot, task, and execution list and detail responses SHALL expose the owning account identifier, username, and display name through non-sensitive DTO fields.

#### Scenario: Display creator information

- **WHEN** a permitted user views a robot, task, or execution
- **THEN** the response SHALL include `creatorAccountId`, `creatorUsername`, and `creatorDisplayName`
- **AND** SHALL NOT include password hashes, session data, role-link internals, or other account security fields

#### Scenario: Owner account is disabled

- **GIVEN** a historical record belongs to a disabled account
- **WHEN** an authorized user views the record
- **THEN** the creator username and current display name SHALL remain resolvable
- **AND** the historical record SHALL NOT be reassigned automatically

### Requirement: Shared organization directory

Organization departments and persons SHALL remain shared enterprise reference data and SHALL NOT be filtered by the owner of a task, robot, or account.

#### Scenario: Different accounts select organization persons

- **GIVEN** two normally authenticated accounts
- **WHEN** each searches or filters the organization directory from the task editor
- **THEN** both SHALL query the same synchronized department and person dataset
- **AND** account-to-person binding SHALL NOT limit directory visibility

### Requirement: Safe legacy ownership migration

The system SHALL migrate legacy ownership without exposing ambiguously owned records to normal accounts.

#### Scenario: Legacy task creator matches an account

- **GIVEN** a legacy task has a creator string exactly matching an account username
- **WHEN** ownership migration runs
- **THEN** the task SHALL be assigned to that account
- **AND** its existing executions SHALL inherit that owner where possible

#### Scenario: Legacy ownership is ambiguous

- **GIVEN** a legacy task, robot, or execution cannot be assigned deterministically
- **WHEN** migration completes
- **THEN** it SHALL remain unassigned and visible only to administrators
- **AND** it SHALL NOT appear in any normal account's list or pagination total
