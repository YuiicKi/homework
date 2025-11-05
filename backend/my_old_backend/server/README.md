# Ruangong GraphQL Server (Java)

## Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL database (schema provided under `../db/schema.sql`)

## Configuration
Environment variables (or edit `src/main/resources/application.yml`):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ruangong
SPRING_DATASOURCE_USERNAME=eth4n
SPRING_DATASOURCE_PASSWORD=******
JWT_SECRET=base64:...
JWT_EXPIRES_IN_SECONDS=604800
```

## Install & Run
```
mvn spring-boot:run
```
GraphiQL available at `http://localhost:8080/graphiql`.
