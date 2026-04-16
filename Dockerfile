FROM eclipse-temurin:17-jre-alpine

CMD ["java", "-jar", "/opt/census-rm-job-processor.jar"]

COPY healthcheck.sh /opt/healthcheck.sh
RUN addgroup --gid 1000 jobprocessor && \
    adduser --system --uid 1000 jobprocessor jobprocessor
USER jobprocessor

COPY target/census-rm-job-processor*.jar /opt/census-rm-job-processor.jar
