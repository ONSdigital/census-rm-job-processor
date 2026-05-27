FROM eclipse-temurin:21-jre-alpine

CMD ["java", "-jar", "/opt/census-rm-job-processor.jar"]

COPY healthcheck.sh /opt/healthcheck.sh
# Create a system group and user without forcing UID/GID
RUN addgroup --system jobprocessor && \
    adduser --system --ingroup jobprocessor jobprocessor

USER jobprocessor

COPY target/census-rm-job-processor*.jar /opt/census-rm-job-processor.jar
