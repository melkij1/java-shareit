FROM eclipse-temurin:21-jre-jammy
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
#VOLUME /tmp
#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]