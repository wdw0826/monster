# 兩階段建置：第一階段只負責編譯出 jar，第二階段只留執行需要的 JRE + jar，
# 不用 image 裡裝一套完整 Maven + JDK 24 GB 跑在正式環境。
# 用專案自帶的 mvnw（Maven Wrapper），不依賴官方 maven image 剛好有沒有 JDK 24 的 tag。
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
