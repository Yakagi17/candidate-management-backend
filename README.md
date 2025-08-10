# Candidate & Vacancy Management System Backend

A Java Spring Boot backend service for managing job candidates and vacancies with intelligent ranking capabilities.

## Features

- **Candidate Management (CRUD)**: Create, read, update, and delete candidate profiles
- **Vacancy Management (CRUD)**: Create, read, update, and delete job vacancies with flexible criteria
- **Intelligent Ranking**: Rank candidates against vacancy criteria using a weighted scoring system
- **Extensible Architecture**: Strategy pattern implementation for easy addition of new criterion types
- **RESTful API**: Standard REST endpoints with proper HTTP status codes
- **Data Validation**: Comprehensive input validation using Bean Validation
- **MongoDB Integration**: Flexible document-based storage with indexing

## Technology Stack

- **Framework**: Java Spring Boot 3.2.0
- **Database**: MongoDB
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Validation**: Bean Validation (Hibernate Validator)
- **Documentation**: Postman Collection

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MongoDB 4.4 or higher (running on localhost:27017)

## Quick Start

### 1. Clone and Setup

```bash
cd candidate-management-backend
```

### 2. Start MongoDB

Make sure MongoDB is running on localhost:27017. If using Docker:

```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 3. Build and Run

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Test the API

Import the provided Postman collection (`Candidate_Management_API.postman_collection.json`) into Postman to test all endpoints.

## API Endpoints

### Candidates

- `POST /api/v1/candidates` - Create a new candidate
- `GET /api/v1/candidates` - Get all candidates
- `GET /api/v1/candidates/{id}` - Get candidate by ID
- `PUT /api/v1/candidates/{id}` - Update candidate
- `DELETE /api/v1/candidates/{id}` - Delete candidate

### Vacancies

- `POST /api/v1/vacancies` - Create a new vacancy
- `GET /api/v1/vacancies` - Get all vacancies
- `GET /api/v1/vacancies/{id}` - Get vacancy by ID
- `PUT /api/v1/vacancies/{id}` - Update vacancy
- `DELETE /api/v1/vacancies/{id}` - Delete vacancy

### Candidate Ranking

- `GET /api/v1/vacancies/{vacancyId}/rank-candidates` - Rank all candidates for a specific vacancy

## Data Models

### Candidate

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "birthdate": "1990-01-01",
  "gender": "MALE",
  "currentSalary": 5000000
}
```

### Vacancy

```json
{
  "name": "Junior Software Engineer",
  "criteria": [
    {
      "type": "AGE",
      "weight": 3,
      "details": {
        "minAge": 22,
        "maxAge": 30
      }
    },
    {
      "type": "GENDER",
      "weight": 1,
      "details": {
        "gender": "ANY"
      }
    },
    {
      "type": "SALARY_RANGE",
      "weight": 5,
      "details": {
        "minSalary": 4500000,
        "maxSalary": 6500000
      }
    }
  ]
}
```

## Ranking Algorithm

The system calculates candidate scores by:

1. **Evaluating each criterion**: Check if candidate matches the criterion requirements
2. **Accumulating weights**: Sum the weights of all matching criteria
3. **Sorting results**: Order candidates by score in descending order

### Matching Rules

- **Age**: Candidate's age (calculated from birthdate) must be within min/max range
- **Gender**: Exact match required, or criterion gender is "ANY"
- **Salary**: Candidate's current salary must be within min/max range

## Example Usage

### 1. Create Test Candidates

```bash
# Siti Rahayu
curl -X POST http://localhost:8080/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Siti Rahayu",
    "email": "siti.r@example.com",
    "birthdate": "1996-05-15",
    "gender": "FEMALE",
    "currentSalary": 5500000
  }'

# Budi Santoso
curl -X POST http://localhost:8080/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Budi Santoso",
    "email": "budi.s@example.com",
    "birthdate": "1989-11-20",
    "gender": "MALE",
    "currentSalary": 8000000
  }'

# Indah Lestari
curl -X POST http://localhost:8080/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Indah Lestari",
    "email": "indah.l@example.com",
    "birthdate": "2002-03-01",
    "gender": "FEMALE",
    "currentSalary": 4000000
  }'
```

### 2. Create a Vacancy

```bash
curl -X POST http://localhost:8080/api/v1/vacancies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Junior Software Engineer",
    "criteria": [
      {
        "type": "AGE",
        "weight": 3,
        "details": {
          "minAge": 22,
          "maxAge": 30
        }
      },
      {
        "type": "GENDER",
        "weight": 1,
        "details": {
          "gender": "ANY"
        }
      },
      {
        "type": "SALARY_RANGE",
        "weight": 5,
        "details": {
          "minSalary": 4500000,
          "maxSalary": 6500000
        }
      }
    ]
  }'
```

### 3. Rank Candidates

```bash
curl -X GET http://localhost:8080/api/v1/vacancies/{vacancyId}/rank-candidates
```

Expected result for the Junior Software Engineer vacancy:
```json
[
  {
    "id": "...",
    "name": "Indah Lestari",
    "email": "indah.l@example.com",
    "score": 9
  },
  {
    "id": "...",
    "name": "Siti Rahayu",
    "email": "siti.r@example.com",
    "score": 9
  },
  {
    "id": "...",
    "name": "Budi Santoso",
    "email": "budi.s@example.com",
    "score": 1
  }
]
```

## Architecture

### Strategy Pattern Implementation

The system uses the Strategy pattern for criterion matching:

- `CriterionMatcher` interface defines the contract
- Concrete implementations: `AgeCriterionMatcher`, `GenderCriterionMatcher`, `SalaryRangeCriterionMatcher`
- `CandidateRankingService` uses a map of matchers for dynamic criterion evaluation

### Extensibility

To add new criterion types:

1. Create a new details class (e.g., `SkillCriteriaDetails`)
2. Implement `CriterionMatcher` interface
3. Register as a Spring `@Component`
4. The system will automatically detect and use the new matcher

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn test -Dtest=CandidateManagementIntegrationTest
```

### Test Coverage

The project includes comprehensive tests for:
- Service layer logic
- Criterion matching algorithms
- API endpoints
- Data validation
- Error handling

## Configuration

### Application Properties

```properties
# Server Configuration
server.port=8080

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=candidate_management

# Logging Configuration
logging.level.com.candidatemanagement=DEBUG
```

### Environment Variables

You can override configuration using environment variables:

```bash
export SPRING_DATA_MONGODB_HOST=your-mongodb-host
export SPRING_DATA_MONGODB_PORT=27017
export SPRING_DATA_MONGODB_DATABASE=candidate_management
```

## Performance Considerations

- **Database Indexing**: Email field is indexed for unique constraint and fast lookups
- **Stream Processing**: Ranking algorithm uses Java Streams for efficient processing
- **Lazy Loading**: MongoDB documents are loaded on-demand
- **Connection Pooling**: Spring Boot automatically configures MongoDB connection pooling

## Error Handling

The API provides comprehensive error responses:

- **400 Bad Request**: Validation errors, duplicate email, etc.
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Unexpected server errors

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License.
