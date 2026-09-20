# syntax=docker/dockerfile:1
FROM node:24-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk-jammy AS backend-build
WORKDIR /workspace/backend
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn/ ./.mvn/
COPY backend/src/ ./src/
COPY --from=frontend-build /workspace/frontend/dist/ ./src/main/resources/static/
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=backend-build --chown=app:app /workspace/backend/target/portfolio-api-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
