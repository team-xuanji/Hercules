CREATE TABLE IF NOT EXISTS `HERCULES_CRON_TASKS` (
                                       `JOB_ID` varchar(128) NOT NULL COMMENT 'Job ID',
                                       `EXECUTOR_REGION` varchar(128) DEFAULT NULL COMMENT 'Executor region',
                                       `PLUGIN_GROUP` varchar(128) DEFAULT NULL COMMENT 'Plugin group',
                                       `PLUGIN_HANDLE` varchar(128) DEFAULT NULL COMMENT 'Plugin handler',
                                       `DESCRIPTION` text COMMENT 'Description',
                                       `CRON_EXPRESSION` varchar(255) NOT NULL COMMENT 'Cron expression',
                                       `ENABLE` tinyint(1) DEFAULT '1' COMMENT 'Whether enabled',
                                       `BEGIN` datetime DEFAULT NULL COMMENT 'Start time',
                                       `END` datetime DEFAULT NULL COMMENT 'End time',
                                       `SNAPSHOT` datetime DEFAULT NULL COMMENT 'Snapshot time',
                                       `CHECKPOINT` text COMMENT 'Checkpoint',
                                       `CONTEXT` text COMMENT 'Context',
                                       `ASYNC_RECOVER_CONTEXT` text COMMENT 'Async recovery context',
                                       `SUPPORT_MISFIRE` tinyint(1) DEFAULT '0' COMMENT 'Whether supports misfire execution',
                                       `MISFIRE_PARALLELISM` int DEFAULT NULL COMMENT 'Misfire execution parallelism',
                                       `MAX_RETRY_TIMES` int DEFAULT '3' COMMENT 'Maximum retry times',
                                       `BUCKET_ID` int DEFAULT NULL COMMENT 'Bucket ID',
                                       `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
                                       `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
                                       PRIMARY KEY (`JOB_ID`),
                                       KEY `idx_snapshot` (`SNAPSHOT`),
                                       KEY `idx_executor_region` (`EXECUTOR_REGION`),
                                       KEY `idx_enable` (`ENABLE`),
                                       KEY `idx_begin` (`BEGIN`),
                                       KEY `idx_end` (`END`),
                                       KEY `idx_bucket_id` (`BUCKET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Scheduled tasks table';


CREATE TABLE IF NOT EXISTS `HERCULES_MANAGER_RUNNER_INSTANCE` (
    `INSTANCE_ID` varchar(128) NOT NULL COMMENT 'Instance ID',
    `HEARTBEAT_TIME` datetime DEFAULT NULL COMMENT 'Heartbeat time',
    `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
    `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`INSTANCE_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Manager runner instance table';

CREATE TABLE IF NOT EXISTS `HERCULES_PLUGIN_IMPL` (
    `PLUGIN_HANDLE` varchar(128) NOT NULL COMMENT 'Plugin handler',
    `PLUGIN_GROUP` varchar(128) DEFAULT NULL COMMENT 'Plugin group',
    `IMPL_CLASS_NAME` varchar(512) DEFAULT NULL COMMENT 'Implementation class name',
    `DESCRIPTION` text COMMENT 'Description',
    `REVISION` bigint DEFAULT '0' COMMENT 'Version number',
    `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
    `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`PLUGIN_HANDLE`),
    KEY `idx_plugin_group` (`PLUGIN_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Plugin implementation information table';

CREATE TABLE IF NOT EXISTS `HERCULES_PLUGIN` (
    `PLUGIN_GROUP` varchar(128) NOT NULL COMMENT 'Plugin group',
    `RESOURCES` json DEFAULT NULL COMMENT 'Resource list',
    `DESCRIPTION` text COMMENT 'Description',
    `REVISION` varchar(64) DEFAULT NULL COMMENT 'Version',
    `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
    `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`PLUGIN_GROUP`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Plugin information table';

CREATE TABLE IF NOT EXISTS `HERCULES_TASK_INFO` (
   `TASK_ID` varchar(128) NOT NULL COMMENT 'Task ID',
   `EXECUTOR_REGION` varchar(128) DEFAULT NULL COMMENT 'Executor region',
   `PLUGIN_GROUP` varchar(128) DEFAULT NULL COMMENT 'Plugin group',
   `PLUGIN_HANDLE` varchar(128) DEFAULT NULL COMMENT 'Plugin handler',
   `DESCRIPTION` text COMMENT 'Description',
   `FROM_TYPE` varchar(50) DEFAULT NULL COMMENT 'Source type',
   `SOURCE_ID` varchar(128) DEFAULT NULL COMMENT 'Source ID',
   `CHAIN_DEPTH` int DEFAULT '0' COMMENT 'Forward chain depth (0 = not a chain task)',
   `CONTEXT` text COMMENT 'Context',
   `ASYNC_RECOVER_CONTEXT` text COMMENT 'Async recovery context',
   `CHECK_POINT_INFO` text COMMENT 'Checkpoint information',
   `ENABLE` tinyint(1) DEFAULT '1' COMMENT 'Whether enabled',
   `STATUS` varchar(50) DEFAULT NULL COMMENT 'Status',
   `MAX_RETRY_TIMES` int DEFAULT '3' COMMENT 'Maximum retry times',
   `OWNER_ID` varchar(128) DEFAULT NULL COMMENT 'Owner ID',
   `BUCKET_ID` int DEFAULT NULL COMMENT 'Bucket ID',
   `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
   `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
   PRIMARY KEY (`TASK_ID`),
   KEY `idx_source_id` (`SOURCE_ID`),
   KEY `idx_from_type` (`FROM_TYPE`),
   KEY `idx_task_dispatch` (`EXECUTOR_REGION`, `STATUS`, `ENABLE`, `BUCKET_ID`, `OWNER_ID`, `INSERT_TIME`),
   KEY `idx_status_update` (`STATUS`, `UPDATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Task information table';

-- HERCULES_RECOVER_TASKS table creation statement
CREATE TABLE IF NOT EXISTS HERCULES_RECOVER_TASKS_HOT (
     ID BIGINT NOT NULL COMMENT 'Primary key ID, generated by snowflake algorithm',
     PLUGIN_GROUP VARCHAR(128) COMMENT 'Plugin group',
     PLUGIN_HANDLE VARCHAR(128) COMMENT 'Plugin handler',
     EXECUTOR_REGION VARCHAR(128) COMMENT 'Executor region',
     DESCRIPTION TEXT COMMENT 'Description information',
     TASK_INFO JSON COMMENT 'Task information, JSON format storing HerculesTaskInfo object',
     ERROR_INFO TEXT COMMENT 'Error information',
     BUCKET_ID INT COMMENT 'Bucket ID, used for sharding processing',
     NEXT_PROCESS_TIME DATETIME COMMENT 'Next processing time',
     `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
     `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
     PRIMARY KEY (ID),
     INDEX idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Hercules failed task recovery table';

CREATE TABLE IF NOT EXISTS HERCULES_RECOVER_TASKS_WARM (
                                            ID BIGINT NOT NULL COMMENT 'Primary key ID, generated by snowflake algorithm',
                                            PLUGIN_GROUP VARCHAR(128) COMMENT 'Plugin group',
                                            PLUGIN_HANDLE VARCHAR(128) COMMENT 'Plugin handler',
                                            EXECUTOR_REGION VARCHAR(128) COMMENT 'Executor region',
                                            DESCRIPTION TEXT COMMENT 'Description information',
                                            TASK_INFO JSON COMMENT 'Task information, JSON format storing HerculesTaskInfo object',
                                            ERROR_INFO TEXT COMMENT 'Error information',
                                            BUCKET_ID INT COMMENT 'Bucket ID, used for sharding processing',
                                            NEXT_PROCESS_TIME DATETIME COMMENT 'Next processing time',
                                            `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
                                            `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
                                            PRIMARY KEY (ID),
                                            INDEX idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Hercules failed task recovery table';

CREATE TABLE IF NOT EXISTS HERCULES_RECOVER_TASKS_COLD (
                                            ID BIGINT NOT NULL COMMENT 'Primary key ID, generated by snowflake algorithm',
                                            PLUGIN_GROUP VARCHAR(128) COMMENT 'Plugin group',
                                            PLUGIN_HANDLE VARCHAR(128) COMMENT 'Plugin handler',
                                            EXECUTOR_REGION VARCHAR(128) COMMENT 'Executor region',
                                            DESCRIPTION TEXT COMMENT 'Description information',
                                            TASK_INFO JSON COMMENT 'Task information, JSON format storing HerculesTaskInfo object',
                                            ERROR_INFO TEXT COMMENT 'Error information',
                                            BUCKET_ID INT COMMENT 'Bucket ID, used for sharding processing',
                                            NEXT_PROCESS_TIME DATETIME COMMENT 'Next processing time',
                                            `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
                                            `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
                                            PRIMARY KEY (ID),
                                            INDEX idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Hercules failed task recovery table';

CREATE TABLE IF NOT EXISTS HERCULES_RECOVER_TASKS_DEAD (
                                            ID BIGINT NOT NULL COMMENT 'Primary key ID, generated by snowflake algorithm',
                                            PLUGIN_GROUP VARCHAR(128) COMMENT 'Plugin group',
                                            PLUGIN_HANDLE VARCHAR(128) COMMENT 'Plugin handler',
                                            EXECUTOR_REGION VARCHAR(128) COMMENT 'Executor region',
                                            DESCRIPTION TEXT COMMENT 'Description information',
                                            TASK_INFO JSON COMMENT 'Task information, JSON format storing HerculesTaskInfo object',
                                            ERROR_INFO TEXT COMMENT 'Error information',
                                            BUCKET_ID INT COMMENT 'Bucket ID, used for sharding processing',
                                            NEXT_PROCESS_TIME DATETIME COMMENT 'Next processing time',
                                            `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
                                            `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
                                            PRIMARY KEY (ID),
                                            INDEX idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Hercules failed task recovery table';

CREATE TABLE IF NOT EXISTS HERCULES_EXECUTOR_INFO (
                                        EXECUTOR_ID VARCHAR(128) NOT NULL COMMENT 'Executor ID, Primary Key',
                                        IDENTITY_ID VARCHAR(128) COMMENT 'Executor Identity ID',
                                        EXECUTOR_REGION VARCHAR(128) COMMENT 'Executor Region',
                                        EXECUTOR_REGION_DESC VARCHAR(500) COMMENT 'Executor Region Desc',
                                        EXECUTOR_MAX_SLOT INT COMMENT 'Maximum number of executor resource slots',
                                        EXECUTOR_AVAILABLE_SLOT INT COMMENT 'Number of available executor resource slots',
                                        ENABLE_DUCKDB TINYINT(1) DEFAULT 1 COMMENT 'Whether to enable DuckDB, 0-No, 1-Yes',
                                        EXECUTOR_LOAD_PLUGIN_INFO TEXT COMMENT 'Executor loads plugin information, storing the ExecutorCurrentLoadPluginInfo object in JSON format.',
                                        EXECUTOR_PLUGIN_HANDLE_WHITE_LIST TEXT COMMENT 'Executor plugin processes the whitelist, storing the ExecutorProcessHandleWhiteList object in JSON format.',
                                        `INSERT_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Insertion time',
                                        `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
                                        PRIMARY KEY (EXECUTOR_ID),
                                        INDEX idx_update_time (UPDATE_TIME)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Hercules Executor Information Table';
