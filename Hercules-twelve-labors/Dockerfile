# Phase 2 (JDK 17 + Spring Boot 4.1.1): switched from the private Oracle JDK 8u152 base
# (not container-aware) to the public Eclipse Temurin 17 JRE image. The team can retag this
# into their private registry if a fully air-gapped build is required.
FROM eclipse-temurin:17-jre


MAINTAINER wentjiang "<wentjiang@sina.com>"


#RUN mkdir -p /var/duckdb_spill
#RUN mkdir -p /var/duckdb_data
#RUN mkdir -p /root/.duckdb

COPY target/Hercules-twelve-labors.jar /Hercules-twelve-labors.jar

#COPY target/external-resources/extensions  /root/.duckdb/extensions



#COPY lib/*.jar /usr/java/default/jre/lib/security/

EXPOSE 8080

#ENV ENVIRONMENT = "qa"

# Replaces JDK8 flags: -XX:+UseCGroupMemoryLimitForHeap (removed since JDK 11) and
# -XX:MaxRAMFraction=3 -> -XX:MaxRAMPercentage=33.3 (= 100/3). G1 is the JDK 17 default GC,
# so no explicit collector flag is needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=33.3"

#每个应用必须配置的密钥(替换为当前服务的密钥)
#ENV spectrum.key etl-msg-service
#ENV spectrum.secret 1480D2CE57694260916E1303DA48EF59

ENV OPEN_OPTS="-javaagent:/agent/opentelemetry-javaagent.jar"



ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /Hercules-twelve-labors.jar --spring.profiles.active=${HERCULES_ACTIVE_PROFILE}"]