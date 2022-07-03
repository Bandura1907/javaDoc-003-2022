#FROM openjdk:11
#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} javaDoc-003-2022-0.0.1.jar
#EXPOSE 3001
#ENTRYPOINT ["java","-jar","/javaDoc-003-2022-0.0.1.jar"]

FROM openjdk:11
ADD target/javaDoc-003-2022-0.0.1.jar java-doc.jar
ENTRYPOINT ["java", "-jar", "java-doc.jar"]