# WeCom Directory Synchronization Specification Delta

## MODIFIED Requirements

### Requirement: Organization management workspace

The system SHALL provide an organization workspace with synchronization overview and filters, a department tree with member counts, and member-list and department-detail views while keeping WeCom as the only organization data source.

#### Scenario: View organization workspace

- **WHEN** an authorized user opens organization management
- **THEN** the page SHALL show latest synchronization status and time, department hierarchy, member counts, and a paginated member list
- **AND** SHALL provide synchronization and synchronization-log actions but no manual member creation action

#### Scenario: Select a department

- **WHEN** a user selects a department node
- **THEN** the member list SHALL filter to that department according to the selected direct-or-recursive scope
- **AND** the displayed count and pagination total SHALL use the same scope

### Requirement: Member search and detail

The system SHALL support filtering synchronized members by name or WeCom account, member status, gender, synchronization status, and department, and SHALL provide a read-only member detail.

#### Scenario: Search by identity

- **WHEN** a user enters a name or WeCom userid
- **THEN** the member query SHALL match the supported identity fields
- **AND** the UI SHALL label userid as “企微账号” unless a configured employee-number mapping exists

#### Scenario: View member detail

- **WHEN** a user opens member detail
- **THEN** the system SHALL show permitted synchronized profile fields, all department memberships, primary department, member status, sync status, and sync time
- **AND** mobile, email, errors, and credentials SHALL follow masking and authorization rules

### Requirement: Account and person terminology separation

The organization workspace SHALL use member or employment status terminology and SHALL NOT represent synchronized persons as login accounts.

#### Scenario: Member becomes inactive

- **WHEN** synchronization marks a person inactive
- **THEN** the organization page SHALL show an inactive member state
- **AND** SHALL NOT automatically disable, create, or delete a login account

### Requirement: Per-record synchronization observability

The system SHALL expose a sanitized per-member synchronization state and retain historical data when synchronization is partial or fails.

#### Scenario: Member update fails

- **WHEN** one member cannot be normalized or persisted during synchronization
- **THEN** the run and affected record SHALL expose a sanitized failure state where technically possible
- **AND** the system SHALL NOT create a manually editable placeholder person
