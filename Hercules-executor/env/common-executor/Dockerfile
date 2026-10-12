# Phase 2 (JDK 17 + Spring Boot 4.1.1): switched from the private Oracle JDK 8u152 base
# (not container-aware) to the public Eclipse Temurin 17 JRE image. The team can retag this
# into their private registry if a fully air-gapped build is required.
# (Was: # Change to the actual image repository address.)
FROM eclipse-temurin:17-jre

#Memory usage in DuckDB
ENV DUCKDB_MEM_GB_SIZE="1"
#duckdb disk buffer size (spill+cache)
ENV DUCKDB_SPILL_GB_SIZE="200"
#duckdb disk buffer path
ENV DUCKDB_SPILL_PATH="/var/duckdb_spill"
#duckdb persistence path
ENV DUCKDB_STORAGE_PATH="/var/duckdb_data/storage_fs.data"
#Maximum number of parallel tasks per executor
ENV EXECUTOR_SLOT_SIZE="4"
#Executor Business Grouping
ENV EXECUTOR_REGION="common_executor"
#Executor Business Group Description
ENV EXECUTOR_REGION_DESC="Public Business Executor"
#Whether to enable duckdb
ENV ENABLE_DUCKDB="true"



RUN mkdir -p /var/duckdb_spill
RUN mkdir -p /var/duckdb_data
RUN mkdir -p /root/.duckdb

COPY target/Hercules-executor.jar /Hercules-executor.jar

COPY target/external-resources/extensions  /root/.duckdb/extensions



#COPY lib/*.jar /usr/java/default/jre/lib/security/

EXPOSE 8080

#ENV ENVIRONMENT = "qa"

# Replaces JDK8 flags: -XX:+UseCGroupMemoryLimitForHeap (removed since JDK 11) and
# -XX:MaxRAMFraction=3 -> -XX:MaxRAMPercentage=33.3 (= 100/3). G1 is the JDK 17 default GC,
# so no explicit collector flag is needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=33.3"

# Secret key that must be configured for each application (replace with current service key)
#ENV spectrum.key etl-msg-service
#ENV spectrum.secret 1480D2CE57694260916E1303DA48EF59

ENV OPEN_OPTS="-javaagent:/agent/opentelemetry-javaagent.jar"



ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /Hercules-executor.jar --spring.profiles.active=${HERCULES_ACTIVE_PROFILE}"]