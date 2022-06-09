FROM openjdk:11
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} javaDoc-003-2022-0.0.1.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","/javaDoc-003-2022-0.0.1.jar"]