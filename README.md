# Transaction Management System

A simple transaction management application built with Spring Boot.

## Features

- Create, read, update and delete transactions
- In-memory data storage
- RESTful API
- Comprehensive unit and load testing
- Containerized deployment with Docker and Kubernetes
- Caching mechanism for improved performance
- Swagger API documentation
- Health monitoring with Spring Boot Actuator

## API Documentation

The API documentation is available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Running the Application

### Prerequisites
- Java 21
- Maven 3.8+

### Build and Run
```bash
mvn clean package
java -jar target/transaction-management-*.jar
```

### Running Tests
```bash
mvn test
```

### Load Testing
```bash
mvn test -Dtest=TransactionControllerLoadTest

# Run JMeter load test
jmeter -n -t src/test/resources/jmeter/TransactionLoadTest.jmx -l results.jtl
jmeter -g results.jtl -o report
```

## Containerization

### Build Docker Image
```bash
docker build -t transaction-management .
```

### Run in Docker
```bash
docker run -p 8080:8080 transaction-management
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/deployment.yaml
```

## External Libraries

### Core Dependencies
- **Spring Boot Starter Web**: 提供RESTful Web服务支持
- **Spring Boot Starter Validation**: 提供数据验证功能
- **Spring Boot Starter Cache**: 提供缓存支持
- **Caffeine**: 高性能缓存库，用于内存缓存

### Monitoring
- **Micrometer**: 应用指标收集
- **Spring Boot Actuator**: 提供健康检查和监控端点

### API Documentation
- **Springdoc OpenAPI**: 自动生成API文档

### Testing
- **Apache JMeter**: 负载测试工具
- **Spring Boot Starter Test**: 单元测试支持

## Monitoring
The application provides health check endpoints:
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/metrics
