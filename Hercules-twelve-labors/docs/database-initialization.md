# Hercules Database Initialization Guide

## Overview

This guide provides detailed instructions for initializing the Hercules database with the required tables and sample data. The Hercules system requires specific database tables to function properly, which are created by executing the provided SQL initialization scripts.

## Prerequisites

- MySQL 8.0+ installed and running
- Database user with appropriate privileges
- Access to the Hercules project files

## Initialization Scripts

The Hercules system includes two initialization SQL scripts:

1. **Hercules-manager/sql/init.sql** - Creates core system tables
2. **Hercules-twelve-labors/sql/init.sql** - Creates business access gateway tables

## Step-by-Step Initialization

### 1. Create Database and User

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE hercules CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user
CREATE USER 'hercules'@'%' IDENTIFIED BY 'your_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON hercules.* TO 'hercules'@'%';
FLUSH PRIVILEGES;

-- Exit MySQL
EXIT;
```

### 2. Execute Hercules-Manager Initialization

```bash
# Navigate to project root
cd /path/to/hercules

# Execute Manager initialization script
mysql -u hercules -p hercules < Hercules-manager/sql/init.sql
```

**Tables Created by Manager Script:**

| Table Name | Purpose |
|------------|---------|
| `hercules_cron_tasks` | Scheduled task management and cron job definitions |
| `HERCULES_MANAGER_RUNNER_INSTANCE` | Manager instance tracking for distributed deployment |
| `HERCULES_PLUGIN_IMPL` | Plugin implementation registry and metadata |
| `HERCULES_PLUGIN` | Plugin information, resources, and binary storage |
| `HERCULES_TASK_INFO` | Task execution information and status tracking |
| `HERCULES_RECOVER_TASKS_HOT` | Hot recovery tasks (recently failed) |
| `HERCULES_RECOVER_TASKS_WARM` | Warm recovery tasks (moderately aged failures) |
| `HERCULES_RECOVER_TASKS_COLD` | Cold recovery tasks (older failures) |
| `HERCULES_RECOVER_TASKS_DEAD` | Dead recovery tasks (permanent failures) |

### 3. Execute Hercules-Twelve-Labors Initialization

```bash
# Execute Twelve-Labors initialization script
mysql -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql
```

**Tables Created by Twelve-Labors Script:**

| Table Name | Purpose |
|------------|---------|
| `HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF` | Data export task definitions and templates |
| `HERCULES_CMS_DATA_DOWNLOAD_TASK` | Data export task execution records and status |

### 4. Verify Table Creation

```sql
-- Connect to the hercules database
mysql -u hercules -p hercules

-- List all tables
SHOW TABLES;

-- Check table structures (example)
DESCRIBE HERCULES_TASK_INFO;
DESCRIBE HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF;

-- Check sample data
SELECT COUNT(*) FROM HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF;
SELECT COUNT(*) FROM HERCULES_CMS_DATA_DOWNLOAD_TASK;
```

## Sample Data Included

### Task Definitions (HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF)

The initialization script includes 3 sample task definitions:

1. **User CSV Export Task**
   - Business Key: `user-export-csv`
   - Executor Region: `PROD`
   - Export Format: CSV with gzip compression
   - SQL Template: `SELECT user_id, username, email, create_time FROM users WHERE create_time >= '${asdasdas}' AND create_time <= '${asdasdas}'`

2. **Order Parquet Export Task**
   - Business Key: `order-export-parquet`
   - Executor Region: `PROD`
   - Export Format: Parquet with snappy compression
   - SQL Template: `SELECT order_id, user_id, amount, status, create_time FROM orders WHERE create_time >= ${asdasdas} AND status = ${asdasdas}`

3. **Product JSON Export Task**
   - Business Key: `product-export-json`
   - Executor Region: `PROD`
   - Export Format: JSON with gzip compression
   - SQL Template: `SELECT product_id, name, price, category, description FROM products WHERE category = ${asdasdas}`

### Task Execution Records (HERCULES_CMS_DATA_DOWNLOAD_TASK)

The initialization script includes 3 sample task execution records with different statuses:

1. **User Export Task** (ID: 1001)
   - Business Key: `user-export-csv`
   - Status: `SUCCESS`
   - File Path: `exports/users/user_export_abc123.csv.gz`
   - User: `1001` (web-console)

2. **Order Export Task** (ID: 1002)
   - Business Key: `order-export-parquet`
   - Status: `RUNNING`
   - File Path: `exports/orders/order_export_def456.parquet`
   - User: `1002` (api-call)

3. **Product Export Task** (ID: 1003)
   - Business Key: `product-export-json`
   - Status: `INIT`
   - File Path: `exports/products/product_export_ghi789.json.gz`
   - User: `1003` (scheduled-task)

## Docker Initialization

### Using Docker Compose

Create a `docker-compose.yml` with automatic initialization:

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: hercules
      MYSQL_USER: hercules
      MYSQL_PASSWORD: your_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./Hercules-manager/sql/init.sql:/docker-entrypoint-initdb.d/01-manager-init.sql:ro
      - ./Hercules-twelve-labors/sql/init.sql:/docker-entrypoint-initdb.d/02-twelve-labors-init.sql:ro
    command: --default-authentication-plugin=mysql_native_password

volumes:
  mysql_data:
```

### Manual Docker Initialization

```bash
# Start MySQL container
docker run -d --name hercules-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=hercules \
  -e MYSQL_USER=hercules \
  -e MYSQL_PASSWORD=your_password \
  -p 3306:3306 \
  mysql:8.0

# Wait for MySQL to be ready
sleep 30

# Copy and execute initialization scripts
docker cp Hercules-manager/sql/init.sql hercules-mysql:/tmp/manager-init.sql
docker cp Hercules-twelve-labors/sql/init.sql hercules-mysql:/tmp/twelve-labors-init.sql

docker exec hercules-mysql mysql -u hercules -p'your_password' hercules -e "source /tmp/manager-init.sql"
docker exec hercules-mysql mysql -u hercules -p'your_password' hercules -e "source /tmp/twelve-labors-init.sql"
```

## Troubleshooting

### Common Issues

1. **Permission Denied**
   ```bash
   ERROR 1045 (28000): Access denied for user 'hercules'@'localhost'
   ```
   **Solution**: Verify user creation and privileges

2. **Database Not Found**
   ```bash
   ERROR 1049 (42000): Unknown database 'hercules'
   ```
   **Solution**: Ensure database is created before running scripts

3. **Table Already Exists**
   ```bash
   ERROR 1050 (42S01): Table 'HERCULES_TASK_INFO' already exists
   ```
   **Solution**: Drop existing tables or use `IF NOT EXISTS` in scripts

4. **Character Set Issues**
   ```bash
   ERROR 1366 (HY000): Incorrect string value
   ```
   **Solution**: Ensure database uses utf8mb4 character set

### Verification Queries

```sql
-- Check all tables exist
SELECT TABLE_NAME, TABLE_ROWS 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'hercules';

-- Check sample data
SELECT business_key, task_name, export_format 
FROM HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF;

-- Check task execution records
SELECT task_id, business_key, task_status, create_time 
FROM HERCULES_CMS_DATA_DOWNLOAD_TASK 
ORDER BY create_time DESC;
```

## Customization

### Removing Sample Data

If you want to start with clean tables without sample data:

```sql
-- Remove sample task definitions
DELETE FROM HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF;

-- Remove sample task records
DELETE FROM HERCULES_CMS_DATA_DOWNLOAD_TASK;

-- Reset auto-increment counters
ALTER TABLE HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF AUTO_INCREMENT = 1;
ALTER TABLE HERCULES_CMS_DATA_DOWNLOAD_TASK AUTO_INCREMENT = 1;
```

### Adding Custom Task Definitions

```sql
-- Add your own task definition
INSERT INTO HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF (
    business_key, task_name, task_description, sql_template,
    rds_info, plugin_group, plugin_name, export_format,
    export_format_params, oss_endpoint, oss_key, oss_secret,
    bucket_name, oss_region, file_prefix, create_time, update_time
) VALUES (
    'your-business-key',
    'Your Task Name',
    'Your task description',
    'SELECT * FROM your_table WHERE condition = ''${#parameter}''',
    '{"host":"your-db-host","port":3306,"database":"your-db","username":"user","password":"pass"}',
    'data-processing',
    'data-exporter',
    'xlsx',
    '{"includeHeader":true,"sheetName":"Data"}',
    'your-oss-endpoint',
    'your-oss-key',
    'your-oss-secret',
    'your-bucket',
    'your-region',
    'export_',
    NOW(),
    NOW()
);
```

## Next Steps

After successful database initialization:

1. Configure each Hercules service with the database connection
2. Start services in the correct order (Manager → Executor → Twelve-Labors)
3. Verify system functionality by submitting a test task
4. Monitor logs for any initialization issues

The database is now ready for the Hercules system to operate with full task distribution and execution capabilities.
