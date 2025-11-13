# Ruangong GraphQL Server (Java)

## Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL database (schema provided under `../db/schema.sql`)

## Configuration
Environment variables (required; `JWT_SECRET` has no default):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ruangong
SPRING_DATASOURCE_USERNAME=eth4n
SPRING_DATASOURCE_PASSWORD=******
JWT_SECRET=base64:<output of `openssl rand -base64 48`>
JWT_EXPIRES_IN_SECONDS=604800
```

Generate a strong secret with:

```
openssl rand -base64 48
```

Set the environment variable (example):

```
export JWT_SECRET=base64:$(openssl rand -base64 48)
```


## Install & Run
```
mvn spring-boot:run
```
GraphiQL available at `http://localhost:8080/graphiql`.
