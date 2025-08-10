# Stage 1: Build the native executable
FROM container-registry.oracle.com/graalvm/native-image:17 AS builder

WORKDIR /app

# Copy the Maven project
COPY pom.xml .
COPY src ./src/

# Install Maven
RUN microdnf install -y maven

# Build the native executable
RUN mvn -Pnative clean package -DskipTests

# Stage 2: Create a minimal runtime container
FROM alpine:latest

# Install necessary dependencies
RUN apk add --no-cache libc6-compat

WORKDIR /app

# Copy the native executable from builder stage
COPY --from=builder /app/target/candidate-management-backend ./app

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["/app/app"]