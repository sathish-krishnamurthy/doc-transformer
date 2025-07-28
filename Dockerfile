FROM openjdk:17-jdk-slim

# Install LibreOffice (for office conversion), Maven, and wget for healthcheck
RUN apt-get update && \
    apt-get install -y libreoffice maven wget && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Expose application port and debug port
EXPOSE 8080 5005

# Run with debug enabled
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "target/doc-transformer-0.0.1-SNAPSHOT.jar"]