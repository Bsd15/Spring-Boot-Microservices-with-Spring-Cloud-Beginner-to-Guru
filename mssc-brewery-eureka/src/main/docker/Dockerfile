# Ref: https://stackoverflow.com/a/70804586/8015568 for why I'm using below image
FROM bellsoft/liberica-openjdk-alpine:17.0.12-10 as builder
WORKDIR application
ADD maven/${project.build.finalName}.jar ${project.build.finalName}.jar
RUN java -Djarmode=layertools -jar ${project.build.finalName}.jar extract

FROM bellsoft/liberica-openjre-alpine:17.0.12-10
LABEL PROJECT_NAME=${project.artifactId} \
      PROJECT=${project.id}
      
RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

EXPOSE 8080

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]
HEALTHCHECK --interval=20s --timeout=10s --start-period=15s CMD ["curl", "-f", "http://netflix:eureka@localhost:8761/actuator/health"]