# PDND Interoperability - Catalog Management Micro Service

## How to start
1. Generate boilerplate
```
sbt clean generateCode
```
2. Update dependencies
```
sbt -Djavax.net.ssl.trustStore=<path_to_your_local_trust_store> -Djavax.net.ssl.trustStorePassword=<trust_store_password> update
```
3. Run locally
```
sbt -Dconfig.file="src/main/resources/reference-standalone.conf" run
```