# IBKR TWS API JAR

## Build behavior

The project builds and boots without the Interactive Brokers API JAR. The IB
adapter is disabled by default with:

```yaml
app:
  ib:
    enabled: false
```

The adapter loads the SDK reflectively only when explicitly enabled. This keeps
the licensed SDK outside source control and allows a clean checkout to run the
full Maven build.

## Enabling the IB adapter

The Interactive Brokers TWS API is **not published on Maven Central**. Download
it under the applicable IB license and place the JAR in this directory before
setting `IB_ENABLED=true`.

### Step 1 — Download

1. Go to: https://interactivebrokers.github.io/
2. Click **TWS API** → **Download** → **TWS API Stable**
3. Download version **10.19** (or latest stable)
4. On Windows: run the installer
   - Default install path: `C:\TWS API\source\JavaClient`
   - Copy `TwsApi.jar` from that directory to **this** `libs/` folder

### Step 2 — Verify

After placing the JAR:
```
d:\Codebase\ib-api\libs\TwsApi.jar
```

The `infrastructure` Maven module includes it at runtime via:
```xml
<dependency>
    <groupId>com.ib</groupId>
    <artifactId>twsapi</artifactId>
    <version>10.19</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../libs/TwsApi.jar</systemPath>
</dependency>
```

### Alternative: Local Maven Install

If you prefer Maven Local:
```bash
mvn install:install-file \
  -Dfile=TwsApi.jar \
  -DgroupId=com.interactivebrokers \
  -DartifactId=tws-api \
  -Dversion=10.19 \
  -Dpackaging=jar
```
Then update `infrastructure/pom.xml` to use:
```xml
<dependency>
    <groupId>com.interactivebrokers</groupId>
    <artifactId>tws-api</artifactId>
    <version>10.19</version>
</dependency>
```

### API Version Matrix

| TwsApi Version | Compatible TWS/Gateway | Java Version |
|---|---|---|
| 10.19 | TWS 10.19 / Gateway 10.19 | 1.8+ |
| 10.26 | TWS 10.26 / Gateway 10.26 | 1.8+ |

> **Important**: The client version in `IbConnectionManager` must match your installed Gateway/TWS version.
