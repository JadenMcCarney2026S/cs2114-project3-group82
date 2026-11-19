FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY src/spotter/ ./spotter/
RUN mkdir /classes && find spotter -maxdepth 1 -name '*.java' ! -name '*Test.java' \
    | xargs javac -d /classes

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --gid 10001 spotter \
    && useradd --uid 10001 --gid spotter --no-create-home spotter \
    && install -d -o spotter -g spotter -m 0700 /data
COPY --from=build /classes/ /app/
COPY scripts/spotter-server.sh /app/spotter-server.sh
USER spotter
EXPOSE 8080
ENTRYPOINT ["sh", "/app/spotter-server.sh"]
