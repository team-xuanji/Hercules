# Hercules Plugin Security Configuration Guide

## Overview

This guide explains the plugin security features in Hercules, specifically the plugin white list mechanism that controls which plugins can be executed by Hercules Executors. This security feature helps prevent unauthorized or potentially harmful plugins from running in your environment.

## Plugin White List Mechanism

### How It Works

The `PLUGIN_WHITE_LIST` environment variable in Hercules-Executor provides a security layer that restricts plugin execution based on plugin handles. When configured, only plugins whose handles are included in the white list can be executed.

### Configuration Behavior

| Configuration | Behavior | Use Case |
|---------------|----------|----------|
| **Empty/Not Set** | All plugins allowed | Development environments, trusted environments |
| **Comma-separated list** | Only listed plugins allowed | Production environments, security-conscious deployments |

### Environment Variable Format

```bash
# Allow all plugins (default behavior)
PLUGIN_WHITE_LIST=""

# Allow specific plugins only
PLUGIN_WHITE_LIST="data-export,report-generator,etl-processor"

# Single plugin
PLUGIN_WHITE_LIST="data-export"
```

## Security Scenarios

### Development Environment
```bash
# Allow all plugins for development flexibility
export EXECUTOR_REGION=DEV
export EXECUTOR_REGION_DESC="Development Executor"
export PLUGIN_WHITE_LIST=""  # Empty - all plugins allowed
```

**Benefits:**
- Full flexibility for testing new plugins
- No restrictions on plugin development
- Easy debugging and experimentation

**Risks:**
- Potential execution of untested plugins
- No protection against malicious plugins

### Staging Environment
```bash
# Allow tested plugins only
export EXECUTOR_REGION=STAGING
export EXECUTOR_REGION_DESC="Staging Executor"
export PLUGIN_WHITE_LIST="data-export,report-generator,notification-sender"
```

**Benefits:**
- Controlled testing environment
- Prevents untested plugins from running
- Mimics production security

**Use Cases:**
- Pre-production testing
- Quality assurance validation
- Security testing

### Production Environment
```bash
# Strict plugin control
export EXECUTOR_REGION=PROD
export EXECUTOR_REGION_DESC="Production Executor"
export PLUGIN_WHITE_LIST="data-export,report-generator,etl-processor,notification-sender,compliance-auditor"
```

**Benefits:**
- Maximum security
- Prevents unauthorized plugin execution
- Compliance with security policies
- Audit trail for approved plugins

**Requirements:**
- Careful plugin handle management
- Regular review of approved plugins
- Change management process

## Plugin Handle Management

### Plugin Handle Identification

Plugin handles are unique identifiers for plugins in the Hercules system. They are typically defined in the plugin configuration or metadata.

**Example Plugin Handles:**
- `data-export` - Data export functionality
- `report-generator` - Report generation
- `etl-processor` - ETL operations
- `notification-sender` - Notification services
- `compliance-auditor` - Compliance reporting
- `file-processor` - File processing operations
- `api-integrator` - External API integration

### Best Practices for Plugin Handles

1. **Descriptive Names**: Use clear, descriptive names that indicate plugin functionality
2. **Consistent Naming**: Follow consistent naming conventions across plugins
3. **Version Independence**: Avoid version numbers in handles for flexibility
4. **Environment Agnostic**: Use handles that work across different environments

## Configuration Examples

### Multi-Region Deployment

#### Region A (Data Processing)
```bash
export EXECUTOR_REGION=REGION_A
export EXECUTOR_REGION_DESC="Data Processing Region"
export PLUGIN_WHITE_LIST="data-export,etl-processor,data-validator"
```

#### Region B (Reporting)
```bash
export EXECUTOR_REGION=REGION_B
export EXECUTOR_REGION_DESC="Reporting Region"
export PLUGIN_WHITE_LIST="report-generator,chart-creator,pdf-exporter"
```

#### Region C (Integration)
```bash
export EXECUTOR_REGION=REGION_C
export EXECUTOR_REGION_DESC="Integration Region"
export PLUGIN_WHITE_LIST="api-integrator,webhook-handler,message-processor"
```

### Docker Compose Configuration

```yaml
version: '3.8'
services:
  # Data processing executor
  hercules-executor-data:
    image: hercules-executor:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      EXECUTOR_REGION: DATA_PROC
      EXECUTOR_REGION_DESC: "Data Processing Executor"
      PLUGIN_WHITE_LIST: "data-export,etl-processor,data-validator,file-processor"
      EXECUTOR_SLOT_SIZE: 4
    volumes:
      - ./config/executor-application-prod.yml:/app/config/application-prod.yml:ro

  # Reporting executor
  hercules-executor-reports:
    image: hercules-executor:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      EXECUTOR_REGION: REPORTS
      EXECUTOR_REGION_DESC: "Reporting Executor"
      PLUGIN_WHITE_LIST: "report-generator,chart-creator,pdf-exporter,notification-sender"
      EXECUTOR_SLOT_SIZE: 2
    volumes:
      - ./config/executor-application-prod.yml:/app/config/application-prod.yml:ro
```

## Security Monitoring

### Logging Plugin Execution

The Hercules system logs plugin execution attempts, including:
- Successful plugin executions
- Blocked plugin attempts (when not in white list)
- Plugin loading and initialization

### Monitoring Recommendations

1. **Regular Audit**: Review plugin execution logs regularly
2. **Alert on Blocked Attempts**: Set up alerts for blocked plugin execution attempts
3. **White List Reviews**: Periodically review and update plugin white lists
4. **Change Tracking**: Track changes to plugin white list configurations

### Log Analysis Queries

```bash
# Find blocked plugin attempts
grep "Plugin execution blocked" /var/log/hercules-executor.log

# Monitor plugin white list changes
grep "PLUGIN_WHITE_LIST" /var/log/hercules-executor.log

# Track plugin execution patterns
grep "Plugin executed successfully" /var/log/hercules-executor.log | awk '{print $5}' | sort | uniq -c
```

## Troubleshooting

### Common Issues

#### Plugin Execution Denied
**Symptoms:**
- Tasks fail with "Plugin not allowed" error
- Plugin execution blocked messages in logs

**Solutions:**
1. Check if plugin handle is in `PLUGIN_WHITE_LIST`
2. Verify plugin handle spelling and case sensitivity
3. Ensure no extra spaces in the white list configuration
4. Restart executor after white list changes

#### White List Not Working
**Symptoms:**
- All plugins still execute despite white list configuration
- No filtering behavior observed

**Solutions:**
1. Verify `PLUGIN_WHITE_LIST` environment variable is set correctly
2. Check executor startup logs for white list parsing
3. Ensure executor service restart after configuration changes
4. Validate environment variable format (comma-separated, no spaces)

#### Performance Impact
**Symptoms:**
- Slower plugin loading
- Increased memory usage

**Solutions:**
1. Optimize white list size (remove unused plugins)
2. Use specific plugin handles instead of wildcards
3. Monitor executor resource usage
4. Consider splitting executors by function

### Debugging Commands

```bash
# Check current environment variables
printenv | grep PLUGIN_WHITE_LIST

# Verify executor configuration
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name == "systemEnvironment")'

# Test plugin execution
curl -X POST http://localhost:8080/api/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"pluginHandle": "test-plugin", "parameters": {}}'
```

## Best Practices

### Security Best Practices

1. **Principle of Least Privilege**: Only allow necessary plugins
2. **Regular Reviews**: Periodically review and update white lists
3. **Environment Separation**: Use different white lists for different environments
4. **Change Management**: Implement approval process for white list changes
5. **Monitoring**: Monitor plugin execution and blocked attempts

### Operational Best Practices

1. **Documentation**: Document approved plugins and their purposes
2. **Testing**: Test white list configurations in staging before production
3. **Backup**: Keep backup of working white list configurations
4. **Automation**: Use configuration management tools for white list deployment
5. **Validation**: Validate white list syntax before deployment

### Development Best Practices

1. **Plugin Naming**: Use consistent, descriptive plugin handle names
2. **Versioning**: Avoid version numbers in plugin handles
3. **Testing**: Test plugins in development with empty white list first
4. **Documentation**: Document plugin handles and their functions
5. **Security Review**: Review plugin code before adding to white list

## Integration with CI/CD

### Automated White List Management

```yaml
# Example GitHub Actions workflow
name: Update Plugin White List
on:
  push:
    paths:
      - 'config/plugin-whitelist.txt'

jobs:
  update-whitelist:
    runs-on: ubuntu-latest
    steps:
      - name: Update Production White List
        run: |
          WHITELIST=$(cat config/plugin-whitelist.txt | tr '\n' ',' | sed 's/,$//')
          kubectl set env deployment/hercules-executor PLUGIN_WHITE_LIST="$WHITELIST"
```

### Configuration Validation

```bash
#!/bin/bash
# validate-plugin-whitelist.sh

WHITELIST="$1"

# Check for valid format
if [[ "$WHITELIST" =~ [[:space:]] ]]; then
    echo "Error: White list contains spaces"
    exit 1
fi

# Check for valid plugin handles
IFS=',' read -ra PLUGINS <<< "$WHITELIST"
for plugin in "${PLUGINS[@]}"; do
    if [[ ! "$plugin" =~ ^[a-z0-9-]+$ ]]; then
        echo "Error: Invalid plugin handle format: $plugin"
        exit 1
    fi
done

echo "White list validation passed"
```

This plugin security configuration provides a robust foundation for controlling plugin execution in Hercules environments while maintaining operational flexibility.
