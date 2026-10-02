# Hercules Twelve-Labors Extension Guide

## Overview

This guide explains how to extend the Hercules Twelve-Labors module beyond its current data export functionality. The module is designed as an extensible business access gateway that can be customized for various business scenarios.

## Important Understanding

### Current Status
- **Data Export**: The current implementation focuses on data export functionality as a **temporary example**
- **Not Limited**: The module is not restricted to file downloads or data exports
- **Template Design**: Serves as a template for creating business-specific interfaces

### Extension Philosophy
- **Business Abstraction**: Provide simplified interfaces for complex business workflows
- **Manager Integration**: All extensions ultimately interact with the core Manager module
- **Custom Logic**: Add business-specific validation, transformation, and orchestration

## Extension Patterns

### 1. Business Service Pattern
Create specialized services for specific business domains:

```java
@RestController
@RequestMapping("/api/reports")
public class ReportGenerationController {
    
    @Autowired
    private HerculesManagerClient managerClient;
    
    @PostMapping("/generate")
    public ResponseEntity<TaskResult> generateReport(
            @RequestBody ReportRequest request) {
        
        // Business-specific validation
        validateReportRequest(request);
        
        // Transform to Manager task
        TaskSubmissionRequest taskRequest = TaskSubmissionRequest.builder()
            .businessKey("report-" + request.getReportType())
            .pluginGroup("reporting")
            .pluginName("report-generator")
            .parameters(buildReportParameters(request))
            .build();
        
        // Submit to Manager
        return managerClient.submitTask(taskRequest);
    }
    
    private void validateReportRequest(ReportRequest request) {
        // Custom business validation logic
        if (!isValidReportType(request.getReportType())) {
            throw new InvalidReportTypeException();
        }
    }
    
    private Map<String, Object> buildReportParameters(ReportRequest request) {
        // Transform business request to plugin parameters
        return Map.of(
            "reportType", request.getReportType(),
            "dateRange", request.getDateRange(),
            "format", request.getOutputFormat()
        );
    }
}
```

### 2. Workflow Orchestration Pattern
Create complex multi-step workflows:

```java
@Service
public class DataSynchronizationService {
    
    @Autowired
    private HerculesManagerClient managerClient;
    
    public CompletableFuture<SyncResult> synchronizeData(SyncRequest request) {
        return CompletableFuture
            .supplyAsync(() -> extractData(request))
            .thenCompose(this::transformData)
            .thenCompose(this::loadData)
            .thenApply(this::generateSyncReport);
    }
    
    private TaskResult extractData(SyncRequest request) {
        TaskSubmissionRequest extractTask = TaskSubmissionRequest.builder()
            .businessKey("extract-" + request.getSourceSystem())
            .pluginGroup("etl")
            .pluginName("data-extractor")
            .parameters(Map.of(
                "sourceSystem", request.getSourceSystem(),
                "extractQuery", request.getQuery()
            ))
            .build();
        
        return managerClient.submitTaskAndWait(extractTask);
    }
    
    private CompletableFuture<TaskResult> transformData(TaskResult extractResult) {
        // Submit transformation task based on extract result
        // Return CompletableFuture for async processing
    }
    
    private CompletableFuture<TaskResult> loadData(TaskResult transformResult) {
        // Submit load task based on transform result
        // Return CompletableFuture for async processing
    }
}
```

### 3. External System Integration Pattern
Integrate with external APIs and services:

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private HerculesManagerClient managerClient;
    
    @Autowired
    private ExternalNotificationService externalService;
    
    @PostMapping("/send")
    public ResponseEntity<NotificationResult> sendNotification(
            @RequestBody NotificationRequest request) {
        
        // Pre-process with external service
        NotificationTemplate template = externalService
            .getTemplate(request.getTemplateId());
        
        // Submit to Hercules for processing
        TaskSubmissionRequest taskRequest = TaskSubmissionRequest.builder()
            .businessKey("notification-" + request.getType())
            .pluginGroup("communication")
            .pluginName("notification-sender")
            .parameters(Map.of(
                "template", template,
                "recipients", request.getRecipients(),
                "priority", request.getPriority()
            ))
            .build();
        
        TaskResult result = managerClient.submitTask(taskRequest);
        
        // Post-process with external service
        externalService.trackDelivery(result.getTaskId());
        
        return ResponseEntity.ok(
            NotificationResult.from(result)
        );
    }
}
```

## Extension Examples

### 1. Business Intelligence Dashboard
```java
@RestController
@RequestMapping("/api/bi")
public class BusinessIntelligenceController {
    
    @PostMapping("/dashboard/refresh")
    public ResponseEntity<DashboardResult> refreshDashboard(
            @RequestBody DashboardRequest request) {
        
        // Orchestrate multiple data collection tasks
        List<TaskSubmissionRequest> tasks = request.getWidgets()
            .stream()
            .map(this::createWidgetTask)
            .collect(Collectors.toList());
        
        // Submit parallel tasks
        List<TaskResult> results = managerClient.submitParallelTasks(tasks);
        
        // Aggregate results
        DashboardData dashboard = aggregateResults(results);
        
        return ResponseEntity.ok(DashboardResult.from(dashboard));
    }
}
```

### 2. Compliance and Audit System
```java
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    
    @PostMapping("/audit/generate")
    public ResponseEntity<AuditResult> generateAuditReport(
            @RequestBody AuditRequest request) {
        
        // Validate compliance requirements
        validateComplianceRequirements(request);
        
        // Create audit task with specific compliance parameters
        TaskSubmissionRequest auditTask = TaskSubmissionRequest.builder()
            .businessKey("audit-" + request.getAuditType())
            .pluginGroup("compliance")
            .pluginName("audit-generator")
            .parameters(Map.of(
                "auditType", request.getAuditType(),
                "complianceStandard", request.getStandard(),
                "dateRange", request.getDateRange(),
                "includeEvidence", true
            ))
            .build();
        
        return ResponseEntity.ok(
            AuditResult.from(managerClient.submitTask(auditTask))
        );
    }
}
```

### 3. API Gateway Pattern
```java
@RestController
@RequestMapping("/api/gateway")
public class APIGatewayController {
    
    @PostMapping("/execute/{workflowType}")
    public ResponseEntity<WorkflowResult> executeWorkflow(
            @PathVariable String workflowType,
            @RequestBody Map<String, Object> parameters) {
        
        // Route to appropriate workflow based on type
        WorkflowDefinition workflow = workflowRegistry.getWorkflow(workflowType);
        
        // Execute workflow steps
        WorkflowResult result = workflowExecutor.execute(workflow, parameters);
        
        return ResponseEntity.ok(result);
    }
}
```

## Configuration Extensions

### Custom Business Configuration
```yaml
# application.yml
hercules:
  twelve-labors:
    extensions:
      reporting:
        enabled: true
        default-format: pdf
        max-concurrent-reports: 5
      
      notifications:
        enabled: true
        providers:
          - email
          - sms
          - slack
        
      bi-dashboard:
        enabled: true
        refresh-interval: 300
        cache-ttl: 600
```

### Custom Properties
```java
@ConfigurationProperties(prefix = "hercules.twelve-labors.extensions")
@Data
public class ExtensionProperties {
    
    private ReportingConfig reporting = new ReportingConfig();
    private NotificationConfig notifications = new NotificationConfig();
    private BiDashboardConfig biDashboard = new BiDashboardConfig();
    
    @Data
    public static class ReportingConfig {
        private boolean enabled = false;
        private String defaultFormat = "pdf";
        private int maxConcurrentReports = 5;
    }
}
```

## Best Practices

### 1. Separation of Concerns
- Keep business logic separate from Hercules integration
- Use service layers for complex business operations
- Maintain clear boundaries between business and technical concerns

### 2. Error Handling
- Implement comprehensive error handling for business scenarios
- Provide meaningful error messages to end users
- Log technical details for debugging

### 3. Performance Considerations
- Use async processing for long-running operations
- Implement caching for frequently accessed data
- Consider batch processing for bulk operations

### 4. Security
- Implement proper authentication and authorization
- Validate all input parameters
- Secure sensitive business data

### 5. Monitoring and Observability
- Add business-specific metrics
- Implement health checks for external dependencies
- Provide business-friendly monitoring dashboards

## Migration from Current Implementation

### Step 1: Identify Extension Points
- Analyze current data export functionality
- Identify reusable components
- Plan new business functionality

### Step 2: Create Extension Modules
- Create new controller packages for different business domains
- Implement service layers for business logic
- Add configuration for new features

### Step 3: Gradual Migration
- Keep existing data export functionality during transition
- Add new features incrementally
- Test thoroughly before removing old functionality

## Conclusion

The Hercules Twelve-Labors module is designed to be a flexible, extensible business access gateway. While it currently demonstrates data export functionality, its true potential lies in providing simplified interfaces for any complex business workflow that benefits from the underlying Hercules task orchestration platform.

By following the patterns and examples in this guide, you can extend the module to support virtually any business scenario while maintaining the benefits of the distributed, plugin-based Hercules architecture.
