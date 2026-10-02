-- CMS Data Download Task Definition Table
CREATE TABLE IF NOT EXISTS `HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF` (
                                                                     `BUSINESS_KEY` varchar(128) NOT NULL COMMENT 'Business scenario key (primary key)',
    `BUSINESS_DESC` varchar(512) DEFAULT NULL COMMENT 'Business scenario description',
    `EXECUTOR_REGION` varchar(128) DEFAULT NULL COMMENT 'Executor business key',
    `PLUGIN_GROUP` varchar(128) DEFAULT NULL COMMENT 'Plugin group',
    `PLUGIN_HANDLE` varchar(128) DEFAULT NULL COMMENT 'Plugin handler',
    `SQL_TEMPLATE` text COMMENT 'SQL template, requires sqlParamTemplate for PrepareStatement compilation',
    `SQL_PARAM_TEMPLATE` text COMMENT 'SQL parameter template, JSON format Map(String,String)',
    `SQL_RDS_INFOS` text COMMENT 'RDS connection parameters',
    `EXPORT_FORMAT_TYPE` varchar(50) DEFAULT NULL COMMENT 'Export format (csv, json, parquet, xlsx)',
    `EXPORT_FORMAT_CONFIG` text COMMENT 'Export parameters, mainly controls export format and compression format related detail parameters',
    `OSS_BUCKET` varchar(128) DEFAULT NULL COMMENT 'Export file OSS bucket',
    `OSS_ROOT_PATH` varchar(512) DEFAULT NULL COMMENT 'Root path of export files',
    `OSS_ACCESS_ID` varchar(128) DEFAULT NULL COMMENT 'OSS access key',
    `OSS_ACCESS_SECRET` varchar(256) DEFAULT NULL COMMENT 'OSS access secret',
    `OSS_ENDPOINT` varchar(256) DEFAULT NULL COMMENT 'OSS endpoint',
    `OSS_REGION` varchar(64) DEFAULT NULL COMMENT 'OSS Region e.g.: endpoint = oss-cn-zhangjiakou.aliyuncs.com, region = cn-zhangjiakou',
    `FILE_PREFIX` varchar(128) DEFAULT NULL COMMENT 'Export file name prefix',
    `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
    `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`BUSINESS_KEY`),
    KEY `idx_plugin_group` (`PLUGIN_GROUP`),
    KEY `idx_export_format_type` (`EXPORT_FORMAT_TYPE`),
    KEY `idx_update_time` (`UPDATE_TIME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='CMS data download task definition table';

-- Insert sample data
INSERT INTO `HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF` (
    `BUSINESS_KEY`,
    `BUSINESS_DESC`,
    `EXECUTOR_REGION`,
    `PLUGIN_GROUP`,
    `PLUGIN_HANDLE`,
    `SQL_TEMPLATE`,
    `SQL_PARAM_TEMPLATE`,
    `EXPORT_FORMAT_TYPE`,
    `EXPORT_FORMAT_CONFIG`,
    `OSS_BUCKET`,
    `OSS_ROOT_PATH`,
    `OSS_ACCESS_ID`,
    `OSS_ACCESS_SECRET`,
    `OSS_ENDPOINT`,
    `OSS_REGION`,
    `FILE_PREFIX`
) VALUES (
             'user-export-csv',
             'User data CSV export',
             'PROD',
             'data-export',
             'csv-export-handler',
             'SELECT user_id, username, email, create_time FROM users WHERE create_time >= \'${asdasdas}\' AND create_time <= \'${asdasdas}\'',
             '{"startTime": "2024-01-01 00:00:00", "endTime": "2024-12-31 23:59:59"}',
             'csv',
             '{"compression": "gzip", "header": "true", "delim": ","}',
             'data-export-bucket',
             '/exports/users/',
             'your-access-key-id',
             'your-access-key-secret',
             'oss-cn-hangzhou.aliyuncs.com',
             'cn-hangzhou',
             'user_export_'
         ), (
             'order-export-parquet',
             'Order data Parquet export',
             'PROD',
             'data-export',
             'parquet-export-handler',
             'SELECT order_id, user_id, amount, status, create_time FROM orders WHERE create_time >= ${asdasdas} AND status = ${asdasdas}',
             '{"startTime": "2024-01-01 00:00:00", "status": "completed"}',
             'parquet',
             '{"compression": "snappy", "rowGroupSize": "100000"}',
             'data-export-bucket',
             '/exports/orders/',
             'your-access-key-id',
             'your-access-key-secret',
             'oss-cn-hangzhou.aliyuncs.com',
             'cn-hangzhou',
             'order_export_'
         ), (
             'product-export-json',
             'Product data JSON export',
             'PROD',
             'data-export',
             'json-export-handler',
             'SELECT product_id, name, price, category, description FROM products WHERE category = ${asdasdas}',
             '{"category": "electronics"}',
             'json',
             '{"array": "true", "compression": "gzip"}',
             'data-export-bucket',
             '/exports/products/',
             'your-access-key-id',
             'your-access-key-secret',
             'oss-cn-hangzhou.aliyuncs.com',
             'cn-hangzhou',
             'product_export_'
         );



-- CMS Data Download Task Table
CREATE TABLE IF NOT EXISTS `HERCULES_CMS_DATA_DOWNLOAD_TASK` (
                                                                 `TASK_ID` bigint NOT NULL COMMENT 'Task ID (primary key)',
                                                                 `BUSINESS_KEY` varchar(128) NOT NULL COMMENT 'Business scenario key',
    `CONTEXT` json DEFAULT NULL COMMENT 'RdsExportContext context information',
    `FILE_PATH` varchar(512) DEFAULT NULL COMMENT 'Export file path',
    `TASK_STATUS` varchar(50) DEFAULT 'INIT' COMMENT 'Task status (INIT, RUNNING, SUCCESS, FAILED, CANCELLED)',
    `APP_KEY` varchar(128) DEFAULT NULL COMMENT 'Application key',
    `USER_ID` varchar(128) DEFAULT NULL COMMENT 'User ID',
    `USER_NAME` varchar(128) DEFAULT NULL COMMENT 'User name',
    `OPERATOR_SOURCE` varchar(128) DEFAULT NULL COMMENT 'Operation source',
    `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
    `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`TASK_ID`),
    KEY `idx_business_key` (`BUSINESS_KEY`),
    KEY `idx_task_status` (`TASK_STATUS`),
    KEY `idx_app_key` (`APP_KEY`),
    KEY `idx_user_id` (`USER_ID`),
    KEY `idx_operator_source` (`OPERATOR_SOURCE`),
    KEY `idx_app_user` (`APP_KEY`, `USER_ID`),
    KEY `idx_insert_time` (`INSERT_TIME`),
    KEY `idx_update_time` (`UPDATE_TIME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='CMS data download task table';

-- Insert sample data
INSERT INTO `HERCULES_CMS_DATA_DOWNLOAD_TASK` (
    `TASK_ID`,
    `BUSINESS_KEY`,
    `CONTEXT`,
    `FILE_PATH`,
    `TASK_STATUS`,
    `APP_KEY`,
    `USER_ID`,
    `OPERATOR_SOURCE`
) VALUES (
             1001,
             'user-export-csv',
             '{"rdsInfos":[{"rdsUrl":"localhost","rdsPort":"3306","rdsDatabaseName":"test","rdsUser":"root","rdsPassword":"password","rdsAttachName":"testdb"}],"rdsQuery":"SELECT user_id, username, email FROM users WHERE create_time >= \'2024-01-01 00:00:00\' AND create_time <= \'2024-12-31 23:59:59\'","ossPath":"exports/users/user_export_abc123.csv.gz","bucketName":"data-export-bucket","ossKey":"your-access-key-id","ossSecret":"your-access-key-secret","ossRegion":"cn-hangzhou","ossEndpoint":"oss-cn-hangzhou.aliyuncs.com","exportFormat":"CSV","formatConfig":{"compression":"gzip","header":"true","delim":","}}',
             'exports/users/user_export_abc123.csv.gz',
             'SUCCESS',
             'app-key-001',
             '1001',
             'web-console'
         ), (
             1002,
             'order-export-parquet',
             '{"rdsInfos":[{"rdsUrl":"localhost","rdsPort":"3306","rdsDatabaseName":"test","rdsUser":"root","rdsPassword":"password","rdsAttachName":"testdb"}],"rdsQuery":"SELECT order_id, user_id, amount, status FROM orders WHERE create_time >= \'2024-01-01 00:00:00\' AND status = \'completed\'","ossPath":"exports/orders/order_export_def456.parquet","bucketName":"data-export-bucket","ossKey":"your-access-key-id","ossSecret":"your-access-key-secret","ossRegion":"cn-hangzhou","ossEndpoint":"oss-cn-hangzhou.aliyuncs.com","exportFormat":"PARQUET","formatConfig":{"compression":"snappy","rowGroupSize":"100000"}}',
             'exports/orders/order_export_def456.parquet',
             'RUNNING',
             'app-key-001',
             '1002',
             'api-call'
         ), (
             1003,
             'product-export-json',
             '{"rdsInfos":[{"rdsUrl":"localhost","rdsPort":"3306","rdsDatabaseName":"test","rdsUser":"root","rdsPassword":"password","rdsAttachName":"testdb"}],"rdsQuery":"SELECT product_id, name, price, category FROM products WHERE category = \'electronics\'","ossPath":"exports/products/product_export_ghi789.json.gz","bucketName":"data-export-bucket","ossKey":"your-access-key-id","ossSecret":"your-access-key-secret","ossRegion":"cn-hangzhou","ossEndpoint":"oss-cn-hangzhou.aliyuncs.com","exportFormat":"JSON","formatConfig":{"array":"true","compression":"gzip"}}',
             'exports/products/product_export_ghi789.json.gz',
             'INIT',
             'app-key-002',
             '1003',
             'scheduled-task'
         );
