DEMO: Business Data Export Task Registration:

{
"businessKey":"CMS-ASICS-THIRD-PART-COUPON_NO-RDS-EXPORT",
"businessDesc":"CMS Download Center - ASICS Third-party Coupon Code Export",
"executorRegion":"common_executor",
"pluginGroup":"common-business",
"pluginHandle":"common_rds_exporter_v1",
"sqlTemplate":"SELECT coupon_project_code AS 'Coupon ID', third_part_coupon_code as 'Third-party Coupon Code', redeem_code_start_time as 'Coupon Effective Time', redeem_code_end_time  as 'Coupon Expiration Time', if(used=1,'Issued','Not Issued') as 'Is Issued', used_time as 'Coupon Issue Time', if(enable=1,'Allowed','Not Allowed') as 'Coupon Enable Status', create_time as 'Creation Time' FROM asics_third_coupon.manager_coupon_third_part_coupon_code_detail where coupon_project_code='${coupon_project_code}'",
"sqlRdsInfos":"[{\"rdsUrl\":\"rm-xxx.mysql.zhangbei.rds.aliyuncs.com\",\"rdsAttachName\":\"asics_third_coupon\",\"rdsPort\":\"3306\",\"rdsDatabaseName\":\"yaseshi\",\"rdsUser\":\"xx\",\"rdsPassword\":\"xx$O\"}]",
"sqlParamTemplate":"",
"exportFormatType":"CSV",
"exportFormatConfig":"{\"compression\":\"GZIP\"}",
"ossBucket":"test01",
"ossRootPath":"test-duckdb-io/test_output/",
"ossAccessId":"xx",
"ossAccessSecret":"xx",
"ossEndpoint":"oss-cn-zhangjiakou.aliyuncs.com",
"ossRegion":"cn-zhangjiakou",
"filePrefix":"ASICS_Third_Party_Coupon_Export_"
}