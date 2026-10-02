#FROM hub.shuyun.com/java/base:oracle-8u152-1.1
# Change to the actual image repository address.
FROM shuyun-pass-acr-registry.cn-zhangjiakou.cr.aliyuncs.com/shuyun-pass/java-base-oracle-8u152:1.0.0

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

ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=3"

# Secret key that must be configured for each application (replace with current service key)
#ENV spectrum.key etl-msg-service
#ENV spectrum.secret 1480D2CE57694260916E1303DA48EF59

ENV OPEN_OPTS="-javaagent:/agent/opentelemetry-javaagent.jar"



ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /Hercules-executor.jar --spring.profiles.active=${HERCULES_ACTIVE_PROFILE}"]