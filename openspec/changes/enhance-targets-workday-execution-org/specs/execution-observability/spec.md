# Execution Observability Specification

## ADDED Requirements

### Requirement: Historical execution snapshots

The system SHALL preserve the task name, trigger type, message type, content summary, target identity, and calendar decision as they were at execution time.

#### Scenario: Task is renamed after execution

- **GIVEN** an execution completed before its task was renamed
- **WHEN** a user views the historical execution
- **THEN** the log SHALL display the task name captured at execution time
- **AND** SHALL NOT rewrite history with the current task name

### Requirement: Business-readable execution list

The execution list SHALL show task name, trigger type, aggregate status, target success count, message summary, start/end time, and duration without requiring users to interpret raw API responses.

#### Scenario: View partial success

- **WHEN** an execution has two successful target deliveries and one failed target delivery
- **THEN** the list SHALL display “部分成功 2/3” or an equivalent localized result
- **AND** SHALL allow the user to open target-level details

#### Scenario: Filter by status

- **WHEN** a user selects successful, failed, partial, or running status
- **THEN** the request and displayed records SHALL use one shared stable status definition
- **AND** success and failure values SHALL NOT be reversed between frontend and backend

### Requirement: Target-level detail and error translation

The execution detail SHALL show each target group and push name, normalized outcome, retry information, and a user-readable failure reason; redacted technical details MAY be shown in a permission-controlled collapsed section.

#### Scenario: WeCom rate limit failure

- **WHEN** a target delivery fails due to the webhook rate limit
- **THEN** the primary result SHALL explain that sending was limited and whether it will retry
- **AND** SHALL NOT expose the webhook key or raw credential-bearing response

#### Scenario: Legacy execution lacks snapshots

- **WHEN** an old execution has no task or message snapshot
- **THEN** the detail SHALL identify the data as incomplete historical data
- **AND** SHALL NOT fabricate target names or message content

### Requirement: Dashboard consistency

The dashboard and execution-log page SHALL consume the same execution summary contract and status dictionary.

#### Scenario: Compare dashboard and log

- **WHEN** the same execution appears on both pages
- **THEN** its task name, status, message summary, and success count SHALL be consistent
