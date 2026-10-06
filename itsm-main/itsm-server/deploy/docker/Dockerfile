# =====================================================================
# ITSM Server 镜像(多阶段 + 分层,设计文档 11.5)
# 构建上下文 = 仓库根目录:
#   mvn -DskipTests package
#   docker build -f deploy/docker/Dockerfile -t itsm/server:dev .
# =====================================================================
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY itsm-boot/target/itsm-boot.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
# 分层 COPY:依赖层稳定、业务层变化小,构建与拉取缓存友好
COPY --from=build /build/dependencies/ ./
COPY --from=build /build/spring-boot-loader/ ./
COPY --from=build /build/snapshot-dependencies/ ./
COPY --from=build /build/application/ ./
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
EXPOSE 8080

