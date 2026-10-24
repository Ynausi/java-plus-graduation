FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/collector-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
