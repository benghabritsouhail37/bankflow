# BankFlow

BankFlow is a full-stack banking demo application developed as a **QA Automation portfolio project**.

The project simulates basic banking operations and demonstrates several levels of automated testing, from unit testing to API, integration and end-to-end UI testing.

## Main objective

The goal of BankFlow is not to reproduce a complete banking system.

It was created to demonstrate a realistic QA Automation workflow including:

- backend development
- REST API testing
- business rule validation
- database verification
- frontend testing
- end-to-end automation
- regression testing

---

## Features

### User management

- Create a user
- Retrieve a user
- Validate mandatory fields
- Validate email format
- Prevent duplicate emails

### Bank accounts

- Create an account for an existing user
- Support `MAD` and `EUR`
- Initial balance: `0.00`
- Initial status: `ACTIVE`
- Multiple accounts per user
- Unique account number generation

### Deposits

- Deposit money into an active account
- Reject zero amounts
- Reject negative amounts
- Reject deposits on blocked accounts
- Update the balance
- Save the transaction in history

### Withdrawals

- Withdraw money from an active account
- Reject zero amounts
- Reject negative amounts
- Reject withdrawals on blocked accounts
- Reject withdrawals when the balance is insufficient
- Update the balance
- Save the transaction in history

### Transaction history

BankFlow stores:

- transaction type
- transaction amount
- balance after transaction
- account
- transaction date

Supported transaction types:

```text
DEPOSIT
WITHDRAWAL
```

---

## Technology stack

### Backend

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- Jakarta Validation
- PostgreSQL
- Maven

### Frontend

- React 19
- TypeScript
- Vite
- CSS

### QA & Automation

- JUnit 5
- Mockito
- MockMvc
- REST Assured
- Selenium WebDriver
- ChromeDriver / Selenium Manager
- PostgreSQL integration testing

---

## Test strategy

BankFlow uses several testing layers.

### Unit tests

Business logic is tested independently using JUnit and Mockito.

Examples:

- account creation
- duplicate email
- invalid currency
- deposit validation
- withdrawal validation
- insufficient balance
- blocked accounts
- transaction generation

### Controller tests

Spring MVC controllers are tested using `MockMvc`.

These tests validate:

- HTTP status codes
- JSON responses
- validation errors
- exception handling
- API contracts

### Integration tests

Integration tests use:

```java
@SpringBootTest
@AutoConfigureMockMvc
```

and connect to PostgreSQL.

They validate the complete flow:

```text
HTTP request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

### REST Assured tests

REST Assured executes real HTTP requests against Spring Boot running on a random port.

Examples include:

- successful account creation
- invalid request payload
- malformed JSON
- unsupported Content-Type
- unsupported HTTP method
- deposits
- withdrawals
- insufficient funds
- blocked accounts
- transaction history

### Selenium E2E tests

Selenium tests the React application through Chrome.

Covered scenarios include:

```text
SEL-001  User form display
SEL-002  User creation
SEL-003  Invalid email
SEL-004  Duplicate email
SEL-005  Blank first name
SEL-006  Multiple validation errors

SEL-007  Bank account creation
SEL-008  Deposit
SEL-009  Withdrawal
SEL-010  Withdrawal with insufficient balance
SEL-011  Transaction history
```

---

## Automated test coverage

The current BankFlow V1 regression suite contains approximately:

| Test layer | Tests |
|---|---:|
| Service unit tests | 21 |
| Controller / MockMvc | 23 |
| Integration tests | 17 |
| REST Assured API tests | 45 |
| Selenium E2E tests | 11 |
| **Total** | **117** |

The target regression result is:

```text
117 tests
0 failures
0 errors
```

---

## Main REST APIs

### Users

Create a user:

```http
POST /api/users
```

Example:

```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@example.com"
}
```

Retrieve a user:

```http
GET /api/users/{id}
```

Retrieve all users:

```http
GET /api/users
```

---

### Accounts

Create an account:

```http
POST /api/accounts
```

Example:

```json
{
  "userId": 1,
  "currency": "MAD"
}
```

Retrieve accounts belonging to a user:

```http
GET /api/accounts/user/{userId}
```

---

### Deposit

```http
POST /api/accounts/{accountId}/deposit
```

Example:

```json
{
  "amount": 500.00
}
```

---

### Withdrawal

```http
POST /api/accounts/{accountId}/withdraw
```

Example:

```json
{
  "amount": 100.00
}
```

---

### Transaction history

```http
GET /api/accounts/{accountId}/transactions
```

Example response:

```json
[
  {
    "id": 2,
    "type": "WITHDRAWAL",
    "amount": 100.00,
    "balanceAfter": 400.00,
    "accountId": 1,
    "createdAt": "2026-10-01T18:30:00"
  },
  {
    "id": 1,
    "type": "DEPOSIT",
    "amount": 500.00,
    "balanceAfter": 500.00,
    "accountId": 1,
    "createdAt": "2026-10-01T18:20:00"
  }
]
```

---

## Project structure

```text
bankflow/
│
├── frontend/
│   └── src/
│       ├── App.tsx
│       └── App.css
│
├── src/
│   ├── main/
│   │   ├── java/com/bankflow/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │
│   │   └── resources/
│   │
│   └── test/java/com/bankflow/
│       ├── api/
│       ├── controller/
│       ├── integration/
│       ├── service/
│       └── selenium/
│
├── pom.xml
└── README.md
```

---

## Running the project locally

### Requirements

Install:

- Java 21
- PostgreSQL
- Node.js
- npm
- Google Chrome

---

### PostgreSQL

Create a PostgreSQL database, for example:

```text
bankflow_db
```

Create a local Spring profile:

```text
src/main/resources/application-local.properties
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bankflow_db
spring.datasource.username=YOUR_DATABASE_USER
spring.datasource.password=YOUR_DATABASE_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

`application-local.properties` should not be committed because it may contain local credentials.

---

## Start the backend

From the project root:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

---

## Start the frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

Vite proxies `/api` requests to the Spring Boot backend.

---

## Running automated tests

### Backend and API regression

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,UserControllerTest,UserIntegrationTest,UserApiRestAssuredTest,AccountServiceTest,AccountControllerTest,AccountIntegrationTest,AccountApiRestAssuredTest" test
```

### Selenium regression

The backend and frontend must both be running.

Then execute:

```powershell
.\mvnw.cmd "-Dtest=UserFormSeleniumTest,UserCreationSeleniumTest,UserValidationSeleniumTest,AccountDashboardSeleniumTest" test
```

---

## Example E2E scenario

A typical automated banking scenario is:

```text
Create user
    ↓
Create MAD account
    ↓
Initial balance = 0.00
    ↓
Deposit 500.00
    ↓
Balance = 500.00
    ↓
Withdraw 100.00
    ↓
Balance = 400.00
    ↓
Open transaction history
    ↓
WITHDRAWAL 100.00
DEPOSIT 500.00
```

The automated tests verify both:

```text
React UI
+
PostgreSQL database state
```

---

## QA concepts demonstrated

This project demonstrates practical experience with:

- test case design
- positive and negative testing
- boundary testing
- API contract validation
- database validation
- business rule testing
- test isolation
- test data creation and cleanup
- Page Object Model
- explicit Selenium waits
- regression testing
- layered test automation
- REST API automation
- end-to-end testing

---

## Future improvements

Possible improvements after V1:

- Docker / Docker Compose
- Testcontainers
- GitHub Actions CI
- JWT authentication
- account transfers
- Swagger / OpenAPI
- test reporting
- screenshots on Selenium failure
- parallel test execution

---

## Project purpose

BankFlow was built as a learning and portfolio project focused on **Software Quality Assurance and Test Automation Engineering**.

Its purpose is to demonstrate how different automation layers can be combined to validate a full-stack application from business logic to the browser.