FROM openjdk:11-jre-slim

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    ARCANA_SLEEP=10 \
    JAVA_OPTS=""

# Add a filling user to run our application so that it doesn't need to run as root
RUN adduser filling
WORKDIR /home/filling
ADD filling-service/entrypoint.sh entrypoint.sh
ADD ./filling-service/target/*.jar app.war
ADD ./filling-core/target/filling-core-1.0-SNAPSHOT.jar filling-core.jar
ENV application_flink_url=http://flink_cluster:8081
ENV application_flink_jar=/home/filling/filling-core.jar
RUN chmod 755 entrypoint.sh && chown filling:filling entrypoint.sh
USER filling

ENTRYPOINT ["./entrypoint.sh"]

EXPOSE 8080