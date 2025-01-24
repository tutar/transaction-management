# Use official OpenJDK 21 image
FROM eclipse-temurin:21-jdk-jammy as builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Use slim JRE image for runtime
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy built artifact from builder
COPY --from=builder /app/target/transaction-management-*.jar app.jar

# Expose port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
