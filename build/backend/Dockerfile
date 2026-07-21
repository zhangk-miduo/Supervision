# 多阶段构建：maven 编译 -> jre 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -B -q -s settings.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/supervision-platform.jar app.jar
EXPOSE 8080
# 通过 JAVA_OPTS 控制堆内存（在 docker-compose 中按服务器规格设置）
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
