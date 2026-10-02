# Hercules Build Guide

This guide provides detailed instructions for building the Hercules distributed task orchestration platform from source.

## Prerequisites

### Required Software
- **Java 8+**: OpenJDK or Oracle JDK (JDK 8, 11, 17+ all supported)
- **Maven 3.6+**: Build automation tool
- **Git**: Version control system

### Optional Software
- **Docker**: For building container images
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### System Requirements
- **Memory**: Minimum 2GB RAM (4GB recommended)
- **Disk Space**: At least 1GB free space for build artifacts
- **Network**: Internet connection for downloading dependencies

### JDK Version Support

#### Supported Versions: JDK 8+
The project supports JDK 8+ with upgraded Spring Boot version. Tested and compatible with:
- **JDK 8**: Fully supported (minimum requirement)
- **JDK 11**: Fully supported and recommended for production
- **JDK 17+**: Fully supported with current Spring Boot version

#### Version Recommendations
```bash
# Development
JDK 8 or 11 - Stable and widely used

# Production
JDK 11 or 17 - Long-term support versions with better performance

# Latest Features
JDK 17+ - Latest features and optimizations
```

**Note**: The Spring Boot version has been upgraded to support modern JDK versions while maintaining backward compatibility with JDK 8.

#### JDK Selection Guide

**For Development:**
- **JDK 8**: Minimum requirement, good for legacy compatibility testing
- **JDK 11**: Recommended for most development work (LTS version)
- **JDK 17**: Latest LTS, best performance and features

**For Production:**
- **JDK 11**: Stable LTS version, widely adopted
- **JDK 17**: Latest LTS, better performance, recommended for new deployments

**For CI/CD:**
- Test with JDK 8 to ensure backward compatibility
- Build and deploy with JDK 11 or 17 for optimal performance

## Project Structure

```
hercules/
├── Hercules-common/          # Common utilities and shared code
├── Hercules-manager/         # Central orchestration service
├── Hercules-executor/        # Distributed execution nodes
├── Hercules-twelve-labors/   # Business access gateway
├── Hercules-executor-plugin/ # Plugin framework
├── pom.xml                   # Root Maven configuration
└── README.md
```

## Build Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/team-xuanji/hercules.git
cd hercules
```

### 2. Verify Prerequisites

```bash
# Check Java version (should be 8+)
java -version
javac -version

# Check Maven version (should be 3.6+)
mvn -version

# Verify JAVA_HOME is set
echo $JAVA_HOME
```

### 3. Build All Modules

#### Quick Build (Skip Tests)
```bash
mvn clean package -Dmaven.test.skip=true
```

#### Full Build (With Tests)
```bash
mvn clean package
```

#### Build with Specific Profile
```bash
# Build for production
mvn clean package -Pprod -Dmaven.test.skip=true

# Build for development
mvn clean package -Pdev -Dmaven.test.skip=true
```

### 4. Build Individual Modules

Build specific modules with their dependencies (recommended approach):

```bash
# Build Manager module with dependencies
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am

# Build Executor module with dependencies
mvn clean package -Dmaven.test.skip=true -pl Hercules-executor -am

# Build Twelve-Labors module with dependencies
mvn clean package -Dmaven.test.skip=true -pl Hercules-twelve-labors -am

# Build Common module only
mvn clean package -Dmaven.test.skip=true -pl Hercules-common

# Build Plugin framework with dependencies
mvn clean package -Dmaven.test.skip=true -pl Hercules-executor-plugin -am
```

**Maven Parameters:**
- `-pl` (--projects): Specifies which module to build
- `-am` (--also-make): Also build required dependency modules
- `-Dmaven.test.skip=true`: Skip test compilation and execution (faster than -DskipTests)

### 5. Build Output

After successful build, JAR files will be available:

```
Hercules-manager/target/hercules-manager-{version}.jar
Hercules-executor/target/hercules-executor-{version}.jar
Hercules-twelve-labors/target/hercules-twelve-labors-{version}.jar
Hercules-common/target/hercules-common-{version}.jar
Hercules-executor-plugin/target/hercules-executor-plugin-{version}.jar
```

## Docker Build

### Build Docker Images

```bash
# Build all Docker images
docker build -t hercules-manager:latest -f Hercules-manager/Dockerfile .
docker build -t hercules-executor:latest -f Hercules-executor/Dockerfile .
docker build -t hercules-twelve-labors:latest -f Hercules-twelve-labors/Dockerfile .
```

### Build with Docker Compose

```bash
# Build all services using Docker Compose
docker-compose build

# Build specific service
docker-compose build hercules-manager
```

## Development Build

### IDE Setup

#### IntelliJ IDEA
1. Import project as Maven project
2. Set Project SDK to Java 17+
3. Enable annotation processing
4. Configure code style (optional)

#### Eclipse
1. Import as Existing Maven Project
2. Set Java Build Path to Java 17+
3. Enable Project Facets for Java

#### VS Code
1. Install Java Extension Pack
2. Open project folder
3. Configure Java runtime in settings

### Development Build Commands

```bash
# Compile without packaging
mvn compile

# Run tests only
mvn test

# Install to local repository
mvn install

# Generate sources and documentation
mvn generate-sources javadoc:javadoc
```

## Troubleshooting

### Common Issues

#### 1. Maven Dependencies Not Found
```bash
# Clear local repository and rebuild
mvn dependency:purge-local-repository
mvn clean install -DskipTests
```

#### 2. Java Version Issues
```bash
# Check Java version (should be 8+)
java -version

# Set JAVA_HOME (Linux/Mac)
export JAVA_HOME=/path/to/java8  # or java11, java17, etc.

# Set JAVA_HOME (Windows)
set JAVA_HOME=C:\path\to\java8   # or java11, java17, etc.

# Verify JDK compatibility
# Check current Spring Boot version in pom.xml (should support your JDK version)
grep -r "spring-boot.version" pom.xml
```

#### 3. Memory Issues
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"

# For Windows
set MAVEN_OPTS=-Xmx2g -XX:MaxPermSize=512m
```

#### 4. Network/Proxy Issues
```bash
# Configure Maven proxy in ~/.m2/settings.xml
<settings>
  <proxies>
    <proxy>
      <id>proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.company.com</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
```

#### 5. Module Dependency Issues
```bash
# Force update dependencies
mvn clean install -U -Dmaven.test.skip=true

# Build with dependency tree
mvn dependency:tree

# Build specific module with all dependencies
mvn clean install -Dmaven.test.skip=true -pl Hercules-common
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am

# Build all modules in correct order
mvn clean install -Dmaven.test.skip=true
```

### Build Verification

#### Verify JAR Files
```bash
# List all built JAR files
find . -name "*.jar" -path "*/target/*" -type f

# Check JAR contents
jar -tf Hercules-manager/target/hercules-manager-*.jar | head -20

# Verify main class
java -jar Hercules-manager/target/hercules-manager-*.jar --version
```

#### Run Basic Tests
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run specific test class
mvn test -Dtest=YourTestClass
```

## Build Profiles

### Available Profiles

- **dev**: Development profile with debug settings
- **prod**: Production profile with optimizations
- **docker**: Docker-specific build settings

### Using Profiles

```bash
# Development build
mvn clean package -Pdev

# Production build
mvn clean package -Pprod

# Docker build
mvn clean package -Pdocker
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build and Test
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: [8, 11, 17]
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v2
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'adopt'
    - name: Cache Maven dependencies
      uses: actions/cache@v2
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    - name: Build with Maven
      run: mvn clean package -Dmaven.test.skip=true
    - name: Run tests
      run: mvn test
```

#### Single JDK Version Example (Recommended for most projects)
```yaml
name: Build and Test
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        distribution: 'adopt'
    - name: Cache Maven dependencies
      uses: actions/cache@v2
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    - name: Build with Maven
      run: mvn clean package -Dmaven.test.skip=true
    - name: Run tests
      run: mvn test
```

## Performance Tips

### Build Optimization

```bash
# Parallel builds
mvn -T 4 clean package -Dmaven.test.skip=true

# Skip unnecessary plugins
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

# Use offline mode (after dependencies are cached)
mvn -o clean package -Dmaven.test.skip=true

# Build specific module only (fastest)
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am
```

### Incremental Builds

```bash
# Build only changed modules
mvn clean package -Dmaven.test.skip=true -pl :changed-module -am

# Resume from specific module
mvn clean package -Dmaven.test.skip=true -rf :specific-module

# Build multiple specific modules
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager,Hercules-executor -am
```

## Next Steps

After successful build:

1. **Configure Services**: Set up configuration files for each service
2. **Initialize Database**: Run database initialization scripts
3. **Deploy Services**: Deploy JAR files or Docker images
4. **Monitor Build**: Set up continuous integration and monitoring

For deployment instructions, see [README.md](README.md) and [docs/configuration-guide.md](Hercules-twelve-labors/docs/configuration-guide.md).
