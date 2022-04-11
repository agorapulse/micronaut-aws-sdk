# Benchmarks

Run benchmarks:

```
./gradlew -p benchmarks/micronaut-amazon-awssdk-dynamodb-benchmarks jmh
```

Results:

```
Benchmark                                                       Mode  Cnt    Score   Error  Units
CompressedEntityDynamoDbBenchmark.test_compressed_json_large    avgt   10   64.537 ± 3.809  ms/op
CompressedEntityDynamoDbBenchmark.test_compressed_json_small    avgt   10   34.014 ± 0.810  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_large  avgt   10  141.866 ± 6.792  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_small  avgt   10   38.899 ± 2.607  ms/op
```

Run benchmarks using AWS DynamoDB:

```
export AWS_ACCESS_KEY_ID=your-aws-access-key
export AWS_SECRET_ACCESS_KEY=your-aws-secret-key
export AWS_REGION=your region (e.g. eu-west-1)

./gradlew -p benchmarks/micronaut-amazon-awssdk-dynamodb-benchmarks jmh -DuseAws=true
```

```
Benchmark                                                       Mode  Cnt    Score     Error  Units
CompressedEntityDynamoDbBenchmark.test_compressed_json_large    avgt   10  268.222 ±  71.215  ms/op
CompressedEntityDynamoDbBenchmark.test_compressed_json_small    avgt   10   80.738 ±  12.523  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_large  avgt   10  500.517 ± 125.609  ms/op
CompressedEntityDynamoDbBenchmark.test_uncompressed_json_small  avgt   10  103.782 ±  15.030  ms/op
```
