# arangodb-java-driver-resiliency-tests

## run

Start ArangoDB docker image:
```shell
./docker/start_db_single.sh docker.io/arangodb/arangodb:3.8.0
```

Start [toxiproxy-server](https://github.com/Shopify/toxiproxy) at `127.0.0.1:8474`.

Run the tests:
```shell
mvn test
```
