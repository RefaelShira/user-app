# 📦 User Management Application

Full-Stack user administration app built as a technical assignment.  
Backend in **Spring Boot (Java)** with **H2** DB, frontend planned in **React**.

---

## 🚀 Features
- Create a new user ('POST /api/users')
- List all users with paging + search ('GET /api/users')
- Soft delete users ('DELETE /api/users/{id}')
- User statistics ('GET /api/users/stats') – number of users created in the last 24 hours
- Robust error handling with consistent API responses
- Resilience via Retry + CircuitBreaker (Resilience4j)

---

## 🛠️ Tech Stack
- **Java 17** + Spring Boot 3
- **H2 Database** (in-memory, can be replaced with Postgres/MySQL)
- **Spring Data JPA**
- **Resilience4j**
- **Lombok**

---

## 📂 Project Structure
- 'src/main/java/com/example/user/' – Entities, Repositories, Services, Controllers
- 'src/main/resources/' – configs ('application.yml'), schema ('schema.sql')
- 'src/test/java/...' – tests
- 'SUMMARY.md' – high-level design summary

---
## API Docs
- Swagger UI: http://localhost:9090/swagger-ui.html

## ⚙️ Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Run locally
'''bash
# build
mvn clean package

# run
mvn spring-boot:run

Server starts at: http://localhost:9090
