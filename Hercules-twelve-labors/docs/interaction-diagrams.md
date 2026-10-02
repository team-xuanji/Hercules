# Hercules System Interaction Diagrams

## Overview

This document provides detailed interaction diagrams for the Hercules distributed task orchestration system. These diagrams illustrate the communication patterns, data flows, and component relationships within the system.

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Hercules Ecosystem                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │   Business      │    │         External Systems            │ │
│  │   Access        │◄──►│  (APIs, Databases, File Systems)   │ │
│  │   Gateway       │    │                                      │ │
│  │ (twelve-labors) │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules        │◄──►│         Plugin Registry             │ │
│  │ Manager         │    │     (Execution Plugins)             │ │
│  │ (Orchestration) │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Task            │◄──►│      Task Distribution Storage       │ │
│  │ Scheduler       │    │      (MySQL Tables)                 │ │
│  │ & Dispatcher    │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules        │◄──►│         Storage Layer               │ │
│  │ Executors       │    │  (Database, Object Storage, etc.)   │ │
│  │ (Distributed)   │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

## Detailed Interaction Flows

### 1. Complete Task Lifecycle Flow

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   User/Client   │  │ Twelve-Labors   │  │    Manager      │  │    Database     │  │    Executor     │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
         │                     │                     │                     │                     │
    ┌────┴────┐                │                     │                     │                     │
    │ 1. Task │                │                     │                     │                     │
    │Submission│               │                     │                     │                     │
    └────┬────┘                │                     │                     │                     │
         ├────────────────────►│                     │                     │                     │
         │                     │ 2. Validate &       │                     │                     │
         │                     │    Transform        │                     │                     │
         │                     ├────────────────────►│                     │                     │
         │                     │                     │ 3. Create Task      │                     │
         │                     │                     ├────────────────────►│                     │
         │                     │                     │                     │ 4. Store Task       │
         │                     │                     │                     ├────────────────────►│
         │                     │                     │                     │                 Database
         │                     │ 5. Task Created     │                     │                     │
         │                     │◄────────────────────┤                     │                     │
         │ 6. Task ID          │                     │                     │                     │
         │◄────────────────────┤                     │                     │                     │
         │                     │                     │                     │                     │
    ┌────┴────┐                │                     │                     │                     │
    │ 2. Task │                │                     │                     │                     │
    │Execution│                │                     │                     │                     │
    └────┬────┘                │                     │                     │                     │
         │                     │                     │                     │ 7. Poll Tasks       │
         │                     │                     │                     │◄────────────────────┤
         │                     │                     │                     │ 8. Return Tasks     │
         │                     │                     │                     ├────────────────────►│
         │                     │                     │ 9. Get Plugin Info  │                     │
         │                     │                     │◄────────────────────┤                     │
         │                     │                     │ 10. Plugin Details  │                     │
         │                     │                     ├────────────────────►│                     │
         │                     │                     │                     │                     │
         │                     │                     │                     │              ┌──────┴──────┐
         │                     │                     │                     │              │ 11. Execute │
         │                     │                     │                     │              │    Plugin   │
         │                     │                     │                     │              └──────┬──────┘
         │                     │                     │                     │                     │
         │                     │                     │ 12. Update Status   │                     │
         │                     │                     │◄────────────────────┤                     │
         │                     │                     │                     │ 13. Store Result   │
         │                     │                     │                     │◄────────────────────┤
         │                     │                     │                     │                 Database
    ┌────┴────┐                │                     │                     │                     │
    │ 3. Task │                │                     │                     │                     │
    │Monitoring│               │                     │                     │                     │
    └────┬────┘                │                     │                     │                     │
         ├────────────────────►│                     │                     │                     │
         │                     │ 14. Query Status    │                     │                     │
         │                     ├────────────────────►│                     │                     │
         │                     │                     │ 15. Get Status      │                     │
         │                     │                     ├────────────────────►│                     │
         │                     │                     │                     │ 16. Return Status  │
         │                     │                     │                     ├────────────────────►│
         │                     │                     │◄────────────────────┤                 Database
         │                     │ 17. Status Info     │                     │                     │
         │                     │◄────────────────────┤                     │                     │
         │ 18. Status Response │                     │                     │                     │
         │◄────────────────────┤                     │                     │                     │
         │                     │                     │                     │                     │
    ┌────┴────┐                │                     │                     │                     │
    │ 4. File │                │                     │                     │                     │
    │Download │                │                     │                     │                     │
    └────┬────┘                │                     │                     │                     │
         ├────────────────────►│                     │                     │                     │
         │                     │ 19. Get Download    │                     │                     │
         │                     ├────────────────────►│                     │                     │
         │                     │                     │ 20. Generate URL    │                     │
         │                     │                     ├────────────────────►│                     │
         │                     │                     │                     │ 21. Return URL     │
         │                     │                     │                     ├────────────────────►│
         │                     │                     │◄────────────────────┤                 File Storage
         │                     │ 22. Download URL    │                     │                     │
         │                     │◄────────────────────┤                     │                     │
         │ 23. File Stream     │                     │                     │                     │
         │◄────────────────────┤                     │                     │                     │
```

### 2. Plugin Lifecycle Management

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Developer     │  │    Manager      │  │  File Storage   │  │    Executor     │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Plugin  │                │                     │                     │
    │ Upload  │                │                     │                     │
    └────┬────┘                │                     │                     │
         ├────────────────────►│                     │                     │
         │                     │ 1. Store Plugin     │                     │
         │                     ├────────────────────►│                     │
         │                     │ 2. Storage Success  │                     │
         │                     │◄────────────────────┤                     │
         │                     │ 3. Register Plugin  │                     │
         │                     ├─────────────────────────────────────────► Database
         │                     │                     │                     │
         │ 4. Upload Success   │                     │                     │
         │◄────────────────────┤                     │                     │
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Plugin  │                │                     │                     │
    │Discovery│                │                     │                     │
    └────┬────┘                │                     │                     │
         │                     │                     │ 5. Request Plugin   │
         │                     │◄────────────────────────────────────────┤
         │                     │ 6. Query Plugin     │                     │
         │                     ├─────────────────────────────────────────► Database
         │                     │ 7. Plugin Metadata │                     │
         │                     │◄─────────────────────────────────────────┤
         │                     │ 8. Plugin Info     │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Plugin  │                │                     │                     │
    │Download │                │                     │                     │
    └────┬────┘                │                     │                     │
         │                     │                     │ 9. Download Request │
         │                     │◄────────────────────────────────────────┤
         │                     │ 10. Get Binary      │                     │
         │                     ├────────────────────►│                     │
         │                     │ 11. Binary Data     │                     │
         │                     │◄────────────────────┤                     │
         │                     │ 12. Forward Binary  │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                     │
         │                     │                     │              ┌──────┴──────┐
         │                     │                     │              │ 13. Load &  │
         │                     │                     │              │   Validate  │
         │                     │                     │              └──────┬──────┘
         │                     │                     │                     │
         │                     │ 14. Load Status     │                     │
         │                     │◄────────────────────────────────────────┤
```

### 3. Error Handling and Recovery Flow

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    Manager      │  │    Database     │  │    Executor     │  │  Recovery       │
│  (Scheduler)    │  │                 │  │                 │  │  System         │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Health  │                │                     │                     │
    │ Check   │                │                     │                     │
    └────┬────┘                │                     │                     │
         ├────────────────────►│                     │                     │
         │ 1. Scan Failed      │                     │                     │
         │    Tasks            │                     │                     │
         │                     │ 2. Return Failed   │                     │
         │                     │    Task List       │                     │
         │◄────────────────────┤                     │                     │
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Task    │                │                     │                     │
    │Recovery │                │                     │                     │
    └────┬────┘                │                     │                     │
         │ 3. Categorize       │                     │                     │
         │    by Age           │                     │                     │
         ├────────────────────►│                     │                     │
         │                     │ 4. Move to Recovery │                     │
         │                     │    Tables           │                     │
         │                     │    (HOT/WARM/COLD)  │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                     │
    ┌────┴────┐                │                     │                     │
    │ Retry   │                │                     │                     │
    │Schedule │                │                     │                     │
    └────┬────┘                │                     │                     │
         │ 5. Schedule Retry   │                     │                     │
         ├────────────────────►│                     │                     │
         │                     │ 6. Update Status    │                     │
         │                     │    to RETRY         │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                     │
         │                     │                     │ 7. Poll Retry      │
         │                     │                     │    Tasks           │
         │                     │◄────────────────────┤                     │
         │                     │ 8. Return Retry     │                     │
         │                     │    Tasks            │                     │
         │                     ├────────────────────►│                     │
         │                     │                     │                     │
         │                     │                     │ 9. Execute Retry    │
         │                     │                     ├────────────────────►│
         │                     │                     │                     │
         │                     │                     │ 10. Update Result   │
         │                     │◄────────────────────┤                     │
         │                     │ 11. Store Result    │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                 Database
    ┌────┴────┐                │                     │                     │
    │ Dead    │                │                     │                     │
    │ Letter  │                │                     │                     │
    └────┬────┘                │                     │                     │
         │ 12. Move Failed     │                     │                     │
         │     to DEAD         │                     │                     │
         ├────────────────────►│                     │                     │
         │                     │ 13. Archive Task    │                     │
         │                     ├────────────────────────────────────────►│
         │                     │                     │                 Dead Letter
```

## Communication Protocols

### 1. REST API Communication
- **Protocol**: HTTP/HTTPS
- **Format**: JSON
- **Authentication**: API Key/Signature
- **Error Handling**: Standard HTTP status codes

### 2. Database Communication
- **Protocol**: MySQL Protocol
- **ORM**: MyBatis-Plus
- **Connection Pool**: HikariCP
- **Transaction**: ACID compliance

### 3. File Storage Communication
- **Protocol**: Object Storage API (OSS/S3)
- **Authentication**: Access Key/Secret Key
- **Transfer**: Multipart upload for large files
- **Encryption**: Server-side encryption

## Performance Considerations

### 1. Scalability Patterns
- **Horizontal Scaling**: Multiple Executor instances
- **Load Balancing**: Database-based task distribution
- **Caching**: Plugin and metadata caching
- **Async Processing**: Non-blocking task execution

### 2. Reliability Patterns
- **Circuit Breaker**: API call protection
- **Retry Logic**: Exponential backoff
- **Dead Letter Queue**: Failed task handling
- **Health Checks**: Service monitoring

### 3. Monitoring Points
- **Task Throughput**: Tasks per second
- **Error Rates**: Failed task percentage
- **Response Times**: API latency
- **Resource Usage**: CPU, Memory, Storage

This interaction diagram documentation provides a comprehensive view of how the Hercules system components communicate and collaborate to provide distributed task orchestration capabilities.
