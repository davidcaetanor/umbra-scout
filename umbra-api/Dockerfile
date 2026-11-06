FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:25-jre
RUN groupadd --system umbra && useradd --system --gid umbra --no-create-home --shell /usr/sbin/nologin umbra
WORKDIR /app

COPY --from=build /build/target/umbra-api-*.jar umbra-api.jar

EXPOSE 8080
USER umbra
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/umbra-api.jar"]
