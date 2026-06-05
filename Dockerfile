FROM eclipse-temurin:24-jdk AS build

WORKDIR /app

COPY src ./src

RUN javac -d out src/server/ProductApiServer.java

FROM eclipse-temurin:24-jre

WORKDIR /app

COPY --from=build /app/out ./out
COPY frontend ./frontend
COPY static ./static

ENV PORT=8086
EXPOSE 8086

CMD ["java", "-cp", "out", "server.ProductApiServer"]
