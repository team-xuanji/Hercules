#FROM hub.shuyun.com/java/base:oracle-8u152-1.1
FROM shuyun-pass-acr-registry.cn-zhangjiakou.cr.aliyuncs.com/shuyun-pass/java-base-oracle-8u152:1.0.0


MAINTAINER wentjiang "<wentjiang@sina.com>"


#RUN mkdir -p /var/duckdb_spill
#RUN mkdir -p /var/duckdb_data
#RUN mkdir -p /root/.duckdb

COPY target/Hercules-twelve-labors.jar /Hercules-twelve-labors.jar

#COPY target/external-resources/extensions  /root/.duckdb/extensions



#COPY lib/*.jar /usr/java/default/jre/lib/security/

EXPOSE 8080

#ENV ENVIRONMENT = "qa"

ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=3"

#每个应用必须配置的密钥(替换为当前服务的密钥)
#ENV spectrum.key etl-msg-service
#ENV spectrum.secret 1480D2CE57694260916E1303DA48EF59

ENV OPEN_OPTS="-javaagent:/agent/opentelemetry-javaagent.jar"



ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /Hercules-twelve-labors.jar --spring.profiles.active=${HERCULES_ACTIVE_PROFILE}"]