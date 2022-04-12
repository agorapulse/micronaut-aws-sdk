# Benchmarks

Run benchmarks using Localstack:

```
./gradlew -p benchmarks/micronaut-aws-sdk-dynamodb-benchmarks jmh
```

Results:

```
Benchmark                                                       Mode  Cnt    Score   Error  Units
CompressedEntityDynamoDbBenchmark.test_compressed_json_large    avgt   10   76.421 ± 3.234  ms/op
CompressedEntityDynamoDbBenchmark.test_compressed_json_small    avgt   10   43.031 ± 1.082  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_large  avgt   10  158.065 ± 4.855  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_small  avgt   10   48.963 ± 1.114  ms/op
```

Run benchmarks using AWS DynamoDB:

```
export AWS_ACCESS_KEY_ID=your-aws-access-key
export AWS_SECRET_ACCESS_KEY=your-aws-secret-key
export AWS_REGION=your region (e.g. eu-west-1)

./gradlew -p benchmarks/micronaut-aws-sdk-dynamodb-benchmarks jmh -DuseAws=true
```

Results:

```
Benchmark                                                       Mode  Cnt    Score    Error  Units
CompressedEntityDynamoDbBenchmark.test_compressed_json_large    avgt   10  279.189 ± 68.041  ms/op
CompressedEntityDynamoDbBenchmark.test_compressed_json_small    avgt   10   86.174 ± 20.295  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_large  avgt   10  488.800 ± 92.167  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_small  avgt   10  103.477 ± 15.310  ms/op
```
