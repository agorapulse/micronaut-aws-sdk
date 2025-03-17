/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.ArgumentMarshaller
import com.amazonaws.services.dynamodbv2.datamodeling.ArgumentUnmarshaller
import com.amazonaws.services.dynamodbv2.datamodeling.ConversionSchema
import com.amazonaws.services.dynamodbv2.datamodeling.ConversionSchemas
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverterFactory
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage
import com.amazonaws.services.dynamodbv2.model.AttributeAction
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.amazonaws.services.dynamodbv2.model.ReturnValue
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult
import groovy.transform.CompileDynamic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import io.micronaut.core.naming.NameUtils

import java.lang.reflect.Method
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Default implementation of DynamoDB service.
 *
 * @param <TItemClass >     type of the item
 */
@Slf4j
@PackageScope
@SuppressWarnings([
    'MethodCount',
    'NoWildcardImports',
    'DuplicateStringLiteral', // Instanceof
])
@CompileDynamic
class DefaultDynamoDBService<TItemClass> implements DynamoDBService<TItemClass> {

    private static final ConversionSchema GROOVY_AWARE_CONVERSION_SCHEMA = ConversionSchemas
        .v2CompatibleBuilder('v2CompatibileConversationSchemaWithGroovyAwareness')
        .addFirstType(
            MetaClass,
            new ArgumentMarshaller() {

                @Override
                AttributeValue marshall(Object obj) {
                    return new AttributeValue().withNULL(true)
                }

            },
            new ArgumentUnmarshaller() {

                @Override
                void typeCheck(AttributeValue value, Method setter) {
                    // do nothing
                }

                @Override
                Object unmarshall(AttributeValue value) throws ParseException {
                    throw new ParseException('Cannot unmarshall MetaClass', 0)
                }

            }
        )
        .addFirstType(Enum,
            new ArgumentMarshaller() {

                @Override
                AttributeValue marshall(Object obj) {
                    return obj == null ? null : new AttributeValue().withS(obj.toString())
                }

            },
            new ArgumentUnmarshaller() {

                @Override
                void typeCheck(AttributeValue value, Method setter) { }

                @Override
                Object unmarshall(AttributeValue value) throws ParseException {
                    // should not happen
                    throw new IllegalStateException('Cannot unmarshall Enum, add @DynamoDBConvertedEnum annotation to the field')
                }

            }
        )
        .addFirstType(Instant,
            new ArgumentMarshaller() {

                @Override
                AttributeValue marshall(Object obj) {
                    return obj == null ? null : new AttributeValue().withS(serializeDate((Instant) obj))
                }

            },
            new ArgumentUnmarshaller() {

                @Override
                void typeCheck(AttributeValue value, Method setter) { }

                @Override
                Object unmarshall(AttributeValue value) throws ParseException {
                    return value.s == null ? null : Instant.from(Instant.parse(value.s))
                }

            }
        )
        .build()

    // Specific ranges ending with 'Index' are String concatenated indexes,
    // to keep ordering (ex.: createdByUserIdIndex=37641047|2011-02-21T17:15:23.000Z|2424353910)
    public static final String INDEX_NAME_SUFFIX = 'Index'
    public static final String SERIALIZED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    public static final String SERIALIZED_DATE_TIMEZONE = 'GMT'
    public static final int DEFAULT_READ_CAPACTIY = 2
    public static final int DEFAULT_WRITE_CAPACITY = 2
    public static final String BATCH_ENABLED_KEY = 'batchEnabled'
    public static final int ONE_SECOND = 1000
    public static final String MAX_AFTER_DATE_KEY = 'maxAfterDate'
    public static final String AFTER_KEY = 'after'
    public static final String BEFORE_KEY = 'before'

    static String serializeDate(Date date) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(SERIALIZED_DATE_FORMAT, Locale.ENGLISH)
        dateFormatter.timeZone = TimeZone.getTimeZone(SERIALIZED_DATE_TIMEZONE)
        return dateFormatter.format(date)
    }

    static String serializeDate(Instant instant) {
        return DateTimeFormatter.ofPattern(SERIALIZED_DATE_FORMAT)
                                .withZone(TimeZone.getTimeZone(SERIALIZED_DATE_TIMEZONE).toZoneId())
                                .format(instant)
    }

    @CompileDynamic
    @SuppressWarnings([
        'Instanceof',
        'CatchException',
    ])
    static void nullifyHashSets(Object object) {
        object.properties.each { String prop, val ->
            if (object.hasProperty(prop)
                && object[prop] instanceof HashSet
                && object[prop]?.size() == 0) {
                try {
                    // log.debug("Nullifying collection ${prop} before sending to DynamoDB")
                    Method getter = object.getClass().getMethod(NameUtils.getterNameFor(prop))
                    if (getter != null && getter.getAnnotation(DynamoDBIgnore) == null) {
                        object[prop] = null
                    }
                } catch (Exception e) {
                    log.error "failed to nullify collection ${prop} of ${object} before sending to DynamoDB", e
                }
            }
        }
    }

    protected final AmazonDynamoDB client
    protected final IDynamoDBMapper mapper

    protected DynamoDBMetadata<TItemClass> metadata

    /**
     * Initialize service for a given mapper class
     *
     * @param itemClass
     * @param amazonWebService
     */
    protected DefaultDynamoDBService(AmazonDynamoDB client, IDynamoDBMapper mapper, Class<TItemClass> itemClass) {
        this.client = client
        this.mapper = mapper
        this.metadata = DynamoDBMetadata.create(itemClass)
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    int count(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator, Map settings) {
        Map conditions = rangeKeyValue ? [(rangeKeyName): buildCondition(rangeKeyValue, operator)] : [:]
        return countByConditions(hashKey, conditions, settings)
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     * - maxAfterDate (default to null)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @param settings
     * @return
     */
    int countByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates, Map settings) {
        Map conditions = buildDateConditions(rangeKeyName, rangeKeyDates, settings[MAX_AFTER_DATE_KEY] as Date)
        return countByConditions(hashKey, conditions, settings)
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    int countByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map querySettings) {
        Map settings = new LinkedHashMap(querySettings)
        settings.putAll(batchGetDisabled: true)
        if (!settings.limit) {
            settings.putAll(limit: DEFAULT_COUNT_LIMIT)
        }
        QueryResultPage resultPage = queryByConditions(hashKey, rangeKeyConditions, settings)
        return resultPage?.results?.size() ?: 0
    }

    /**
     * Create the DynamoDB table for the given Class.
     *
     * @param classToCreate Class to create the table for
     * @param dynamoDB the dynamoDB client
     */
    CreateTableResult createTable(Long readCapacityUnits, Long writeCapacityUnits) {
        DynamoDBTable table = metadata.itemClass.getAnnotation(DynamoDBTable)

        try {
            // Check if the table exists
            client.describeTable(table.tableName())
            return null
        } catch (ResourceNotFoundException ignored) {
            CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(metadata.itemClass)
            // new CreateTableRequest().withTableName(table.tableName())

            // ProvisionedThroughput
            createTableRequest.provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(readCapacityUnits)
                .withWriteCapacityUnits(writeCapacityUnits)

            // ProvisionedThroughput for GSIs
            if (createTableRequest.globalSecondaryIndexes) {
                createTableRequest.globalSecondaryIndexes.each { GlobalSecondaryIndex globalSecondaryIndex ->
                    globalSecondaryIndex.provisionedThroughput = new ProvisionedThroughput()
                        .withReadCapacityUnits(DEFAULT_READ_CAPACTIY)
                        .withWriteCapacityUnits(DEFAULT_WRITE_CAPACITY)
                    log.debug("Creating DynamoDB GSI: ${globalSecondaryIndex}")
                }
            }

            log.debug("Creating DynamoDB table: ${createTableRequest}")

            return client.createTable(createTableRequest)
        }
    }

    /**
     * Delete item by IDs.
     *
     * @param hashKey hash key of the item to delete
     * @param rangeKey range key of the item to delete
     * @param settings settings
     */
    void delete(Object hashKey, Object rangeKey, Map settings) {
        if (rangeKey) {
            delete(metadata.itemClass.newInstance((hashKeyName): hashKey, (rangeKeyName): rangeKey), settings)
            return
        }
        delete(metadata.itemClass.newInstance((hashKeyName): hashKey), settings)
    }

    /**
     * Delete a list of items from DynamoDB.
     *
     * @param itemsToDelete a list of objects to delete
     * @param settings settings
     */
    void deleteAll(List<TItemClass> itemsToDelete, Map querySettings) {
        Map settings = new LinkedHashMap(querySettings)
        if (!settings.containsKey(BATCH_ENABLED_KEY)) {
            settings.putAll(batchEnabled: true)
        }

        if (settings.batchEnabled && itemsToDelete.size() > 1) {
            itemsToDelete.collate(WRITE_BATCH_SIZE).each { List batchItems ->
                log.debug("Deleting items from DynamoDB ${batchItems}")
                mapper.batchDelete(batchItems)
            }
        } else {
            itemsToDelete.each {
                log.debug("Deleting item from DynamoDB ${it}")
                mapper.delete(it)
            }
        }
    }

    /**
     * Delete all items for a given hashKey and a rangeKey condition
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    int deleteAll(
        Object hashKey,
        String rangeKeyName,
        Object rangeKeyValue,
        ComparisonOperator operator,
        Map settings
    ) {
        Map conditions = [(rangeKeyName): buildCondition(rangeKeyValue, operator)]
        return deleteAllByConditions(
            hashKey,
            conditions,
            settings
        )
    }

    /**
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @param indexName
     * @return
     */
    int deleteAllByConditions(
        Object hashKey,
        Map<String, Condition> rangeKeyConditions,
        Map querySettings,
        String indexName
    ) {
        Map settings = new LinkedHashMap(querySettings)
        if (!settings.containsKey(BATCH_ENABLED_KEY)) {
            settings.putAll((BATCH_ENABLED_KEY): true)
        }
        if (!settings.limit) {
            settings.putAll(limit: BATCH_DELETE_LIMIT)
        }

        DynamoDBQueryExpression query = buildQueryExpression(hashKeyName, hashKey, settings)
        query.hashKeyValues = metadata.itemClass.newInstance((hashKeyName): hashKey)
        if (rangeKeyConditions) {
            query.rangeKeyConditions = rangeKeyConditions
        }
        if (indexName) {
            query.indexName = indexName
        }

        return deleteAllByConditions(query, settings)
    }

    /**
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @param indexName
     * @return
     */
    @SuppressWarnings('DuplicateNumberLiteral')
    int deleteAllByConditions(DynamoDBQueryExpression query, Map settings) {
        QueryResultPage itemsPage = mapper.queryPage(metadata.itemClass, query)

        int deletedItemsCount = -1
        Map lastEvaluatedKey = itemsPage.lastEvaluatedKey
        while (lastEvaluatedKey || deletedItemsCount == -1) {
            if (deletedItemsCount == -1) {
                deletedItemsCount = 0
            } else {
                query.exclusiveStartKey = lastEvaluatedKey
            }
            itemsPage = mapper.queryPage(metadata.itemClass, query)
            if (itemsPage.results) {
                log.debug "Deleting ${itemsPage.results.size()} items, class: ${metadata.itemClass}"
                deletedItemsCount = deletedItemsCount + itemsPage.results.size()
                // Delete all items
                deleteAll(itemsPage.results, settings)
            }
            lastEvaluatedKey = itemsPage.lastEvaluatedKey
        }
        log.debug "Successfully deleted ${deletedItemsCount} items"
        return deletedItemsCount
    }

    /**
     * Load an item
     *
     * @param hashKey
     * @param rangeKey
     * @return
     */
    TItemClass get(Object hashKey, Object rangeKey) {
        return mapper.load(metadata.itemClass, hashKey, rangeKey)
    }

    /**
     * Retrieve batched items corresponding to a list of item IDs, in the same order.
     * Example: items = twitterItemDBService.getAll(1, [1, 2]).
     *
     * @param hashKey Hash Key of the items to retrieve
     * @param rangeKey Range keys of the items to retrieve
     * @param settings only used for setting throttle/readCapacityUnit when getting large sets
     * @return a list of DynamoDBItem
     */
    @SuppressWarnings('AssignCollectionUnique')
    List<TItemClass> getAll(Object hashKey, List rangeKeys, Map settings) {
        Map result = [:]
        List objects = rangeKeys.unique(false).collect { it -> metadata.itemClass.newInstance((hashKeyName): hashKey, (rangeKeyName): it) }
        if (settings.throttle) {
            int resultCursor = 0
            long readCapacityUnit = settings.readCapacityUnit
            if (!readCapacityUnit) {
                DescribeTableResult tableResult = client.describeTable(metadata.mainTable.tableName())
                readCapacityUnit = tableResult?.table?.provisionedThroughput?.readCapacityUnits ?: 10
            }
            objects.collate(20).each { List batchObjects ->
                result += mapper.batchLoad(batchObjects)
                resultCursor++
                if (readCapacityUnit && resultCursor >= (readCapacityUnit * 0.8)) {
                    resultCursor = 0
                    sleep(ONE_SECOND)
                }
            }
        } else {
            result = mapper.batchLoad(objects)
        }
        if (result[metadata.mainTable.tableName()]) {
            List<TItemClass> unorderedItems = result[metadata.mainTable.tableName()] as List<TItemClass>
            List<TItemClass> items = []

            // Build an item list ordered in the same manner as the list of IDs we've been passed
            rangeKeys.each { rangeKey ->
                TItemClass matchingItem = unorderedItems.find { item ->
                    item[rangeKeyName] == rangeKey
                }
                if (matchingItem) {
                    items.add(matchingItem)
                }
                // Remove the matching item from the unordered list to reduce the number of loops in the find above
                unorderedItems.remove(matchingItem)
            }
            items
        } else {
            []
        }
    }

    /**
     * Increment a count with an atomic operation
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeIncrement
     * @return
     */
    Integer increment(Object hashKey, Object rangeKey, String attributeName, int attributeIncrement) {
        UpdateItemResult result = updateItemAttribute(hashKey, rangeKey, attributeName, attributeIncrement, AttributeAction.ADD)
        return result?.attributes[attributeName]?.n?.toInteger()
    }

    TItemClass getNewInstance() {
        return metadata.itemClass.newInstance()
    }

    PaginatedQueryList<TItemClass> query(DynamoDBQueryExpression queryExpression) {
        return mapper.query(this.metadata.itemClass, queryExpression)
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    QueryResultPage<TItemClass> query(
        Object hashKey,
        String rangeKeyName,
        Object rangeKeyValue,
        ComparisonOperator operator,
        Map settings
    ) {
        if (rangeKeyValue == 'ANY' || !operator) {
            if (!rangeKeyName.endsWith(INDEX_NAME_SUFFIX)) {
                rangeKeyName += INDEX_NAME_SUFFIX
            }
            queryByConditions(hashKey, [:], settings, rangeKeyName)
        } else {
            Map conditions = [(rangeKeyName): buildCondition(rangeKeyValue, operator)]
            queryByConditions(hashKey, conditions, settings)
        }
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     * - throttle insert sleeps during execution to avoid reaching provisioned read throughput (default to false)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    QueryResultPage<TItemClass> queryByConditions(
        Object hashKey,
        Map<String, Condition> rangeKeyConditions,
        Map settings,
        String indexName
    ) {
        DynamoDBQueryExpression query = buildQuery(hashKey, settings, rangeKeyConditions, indexName)

        long readCapacityUnit = 0
        int resultCursor = 0

        if (!settings.returnAll) {
            // Query page
            return fetchResults(mapper.queryPage(metadata.itemClass, query), rangeKeyConditions, indexName, settings, hashKey, readCapacityUnit)
        }

        // Get table read Throughput
        if (settings.throttle) {
            DescribeTableResult tableResult = client.describeTable(metadata.mainTable.tableName())
            readCapacityUnit = tableResult?.table?.provisionedThroughput?.readCapacityUnits ?: 0
        }

        // Query all
        Map lastEvaluatedKey = [items: 'none']

        QueryResultPage resultPage = new QueryResultPage()
        resultPage.results = []

        while (lastEvaluatedKey) {
            QueryResultPage currentPage = mapper.queryPage(metadata.itemClass, query)
            resultPage.results.addAll(currentPage.results)
            lastEvaluatedKey = currentPage.lastEvaluatedKey
            query.exclusiveStartKey = currentPage.lastEvaluatedKey
            if (settings.throttle) {
                resultCursor++
                if (readCapacityUnit && resultCursor >= (readCapacityUnit * 0.5)) {
                    resultCursor = 0
                    sleep(ONE_SECOND)
                }
            }
        }

        return fetchResults(resultPage, rangeKeyConditions, indexName, settings, hashKey, readCapacityUnit)
    }

    /**
     * Query by dates with 'after' and/or 'before' range value
     * 1) After a certain date : [after: new Date()]
     * 2) Before a certain date : [before: new Date()]
     * 3) Between provided dates : [after: new Date() + 1, before: new Date()]
     *
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - maxAfterDate (default to null)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @param settings
     * @return
     */
    QueryResultPage<TItemClass> queryByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates, Map settings) {
        Map conditions = buildDateConditions(rangeKeyName, rangeKeyDates, settings[MAX_AFTER_DATE_KEY] as Date)
        return queryByConditions(hashKey, conditions, settings)
    }

    /**
     * Save a list of objects in DynamoDB.
     *
     * @param itemsToSave a list of objects to save
     * @param settings settings
     */
    List<TItemClass> saveAll(List<TItemClass> itemsToSave, Map querySettings) {
        Map settings = new LinkedHashMap(querySettings)
        if (!settings.containsKey(BATCH_ENABLED_KEY)) {
            settings.putAll((BATCH_ENABLED_KEY): true)
        }

        // Nullify empty collection properties
        itemsToSave.each { object ->
            nullifyHashSets(object)
        }

        log.debug "Saving items in DynamoDB ${itemsToSave}"

        if (settings.batchEnabled && itemsToSave.size() > 1) {
            itemsToSave.collate(WRITE_BATCH_SIZE).each { List<TItemClass> batchItems ->
                log.debug "Saving batched items in DynamoDB ${batchItems}"
                List failedBatchResult = settings.config ? mapper.batchSave(batchItems, settings.config) : mapper.batchSave(batchItems)
                if (failedBatchResult) {
                    failedBatchResult.each { DynamoDBMapper.FailedBatch failedBatch ->
                        int unprocessedItemsCount = 0
                        failedBatch.unprocessedItems.keySet().each { String key ->
                            unprocessedItemsCount += failedBatch.unprocessedItems.get(key).size()
                        }
                        log.error "Failed batch with ${unprocessedItemsCount} unprocessed items"
                        log.error "Exception: ${failedBatch.exception}"
                        throw failedBatch.exception
                    }
                }
            }
            return itemsToSave
        }

        DynamoDBMapperConfig config = settings.config
            ? settings.config as DynamoDBMapperConfig
            : DynamoDBMapperConfig.builder()
                                  .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                                  .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
                                  .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.LAZY_LOADING)
                                  .withTableNameResolver(DynamoDBMapperConfig.DefaultTableNameResolver.INSTANCE)
                                  .withBatchWriteRetryStrategy(DynamoDBMapperConfig.DefaultBatchWriteRetryStrategy.INSTANCE)
                                  .withBatchLoadRetryStrategy(DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy.INSTANCE)
                                  .withTypeConverterFactory(DynamoDBTypeConverterFactory.standard())
                                  .withConversionSchema(GROOVY_AWARE_CONVERSION_SCHEMA)
                                  .build()

        return itemsToSave.each {
            log.debug "Saving item in DynamoDB ${it}"
            mapper.save(it, config)
        }
    }

    /**
     * Delete a single item attribute
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeValue
     * @param action
     * @return
     */
    UpdateItemResult deleteItemAttribute(Object hashKey, Object rangeKey, String attributeName) {
        Map<String, Object> key = [(hashKeyName): buildAttributeValue(hashKey)]
        if (rangeKey) {
            key[rangeKeyName] = buildAttributeValue(rangeKey)
        }
        UpdateItemRequest request = new UpdateItemRequest(
            tableName: metadata.mainTable.tableName(),
            key: key,
            returnValues: ReturnValue.UPDATED_NEW
        ).addAttributeUpdatesEntry(
            attributeName,
            new AttributeValueUpdate(
                action: AttributeAction.DELETE
            )
        )
        return client.updateItem(request)
    }

    /**
     * Update a single item attribute
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeValue
     * @param action
     * @return
     */
    UpdateItemResult updateItemAttribute(
        Object hashKey,
        Object rangeKey,
        String attributeName,
        Object attributeValue,
        AttributeAction action
    ) {
        return this.updateItemAttributes(hashKey, rangeKey, Map.of(attributeName, attributeValue), action)
    }

    /**
     * Update a multiple item attributes
     *
     * @param hashKey
     * @param rangeKey
     * @param valueByAttributeKey
     * @param action
     * @return
     */
    UpdateItemResult updateItemAttributes(
        Object hashKey,
        Object rangeKey,
        Map<String, Object> valueByAttributeKey,
        AttributeAction action
    ) {
        Map<String, Object> key = [(hashKeyName): buildAttributeValue(hashKey)]
        if (rangeKey) {
            key[rangeKeyName] = buildAttributeValue(rangeKey)
        }

        UpdateItemRequest request = new UpdateItemRequest(
            tableName: metadata.mainTable.tableName(),
            key: key,
            returnValues: ReturnValue.UPDATED_NEW
        )

        valueByAttributeKey.entrySet().forEach { entry ->
            request.addAttributeUpdatesEntry(
                entry.key,
                new AttributeValueUpdate(
                    action: action,
                    value: buildAttributeValue(entry.value)
                )
            )
        }

        return client.updateItem(request)
    }

    boolean isIndexRangeKey(String rangeName) {
        return metadata.secondaryIndexes.contains(rangeName)
    }

    @Override
    String getPartitionKeyName() {
        return metadata.hashKeyName
    }

    @Override
    Class getPartitionKeyClass() {
        return metadata.hashKeyClass
    }

    @Override
    String getSortKeyName() {
        return metadata.rangeKeyName
    }

    @Override
    Class getSortKeyClass() {
        return metadata.rangeKeyClass
    }

    /**
     *
     * @param params
     * @param maxAfterDate
     * @return
     */
    static protected Map addMaxAfterDateCondition(
        Map params,
        Date maxAfterDate
    ) {
        if (!params.containsKey(AFTER_KEY)) {
            params[AFTER_KEY] = maxAfterDate
        } else if (((Date) params[AFTER_KEY]).before(maxAfterDate)) {
            params[AFTER_KEY] = maxAfterDate
        }
        if (params.containsKey(BEFORE_KEY) && ((Date) params[BEFORE_KEY]).before((Date) params[AFTER_KEY])) {
            // Make sure that 'before' date is after 'after' date, to generate valid BETWEEN DynamoDB query (even if query will return nothing, it won't break)
            params[BEFORE_KEY] = params[AFTER_KEY]
        }
        return params
    }

    /**
     *
     * @param key
     * @return
     */
    @SuppressWarnings([
        'Instanceof',
        'CouldBeSwitchStatement',
    ])
    static protected AttributeValue buildAttributeValue(Object key) {
        if (key instanceof Number) {
            return new AttributeValue().withN(key.toString())
        }
        if (key instanceof Boolean) {
            return new AttributeValue().withN(key ? '1' : '0')
        }
        if (key instanceof Date) {
            return new AttributeValue().withS(serializeDate(key))
        }
        if (key instanceof Instant) {
            return new AttributeValue().withS(serializeDate(key))
        }
        return new AttributeValue().withS(key.toString())
    }

    /**
     *
     * @param rangeKeyValue
     * @param operator
     * @return
     */
    static protected Condition buildCondition(
        Object rangeKeyValue,
        ComparisonOperator operator = ComparisonOperator.EQ
    ) {
        return new Condition()
            .withComparisonOperator(operator)
            .withAttributeValueList(buildAttributeValue(rangeKeyValue))
    }

    /**
     *
     * @param rangeKeyName
     * @param rangeKeyDates
     * @param maxAfterDate
     * @return
     */
    static protected Map buildDateConditions(
        String rangeKeyName,
        Map<String, Date> rangeKeyDates,
        Date maxAfterDate = null
    ) {
        assert rangeKeyDates.keySet().any { it in [AFTER_KEY, BEFORE_KEY] }
        ComparisonOperator operator
        List attributeValueList = []
        if (maxAfterDate) {
            addMaxAfterDateCondition(rangeKeyDates, maxAfterDate)
        }
        if (rangeKeyDates.containsKey(AFTER_KEY)) {
            operator = ComparisonOperator.GE
            attributeValueList << new AttributeValue().withS(serializeDate(rangeKeyDates[AFTER_KEY]))
        }
        if (rangeKeyDates.containsKey(BEFORE_KEY)) {
            operator = ComparisonOperator.LE
            attributeValueList << new AttributeValue().withS(serializeDate(rangeKeyDates[BEFORE_KEY]))
        }
        if (rangeKeyDates.containsKey(AFTER_KEY) && rangeKeyDates.containsKey(BEFORE_KEY)) {
            operator = ComparisonOperator.BETWEEN
        }
        return [
            (rangeKeyName): new Condition()
                .withComparisonOperator(operator)
                .withAttributeValueList(attributeValueList),
        ]
    }

    /**
     *
     * @param hashKeyName
     * @param hashKey
     * @param settings
     * @return
     */
    @CompileDynamic
    @SuppressWarnings('Instanceof')
    static protected DynamoDBQueryExpression buildQueryExpression(
        Object hashKeyName,
        Object hashKey,
        Map settings = [:]
    ) {
        DynamoDBQueryExpression query = new DynamoDBQueryExpression()
        if (settings.containsKey('consistentRead')) {
            query.consistentRead = settings.consistentRead
        }
        if (settings.exclusiveStartKey) {
            assert settings.exclusiveStartKey instanceof Map
            query.exclusiveStartKey = buildStartKey(settings.exclusiveStartKey + [(hashKeyName): hashKey])
        }
        if (settings.limit) {
            assert settings.limit.toString().number
            query.limit = settings.limit
        } else {
            query.limit = DEFAULT_QUERY_LIMIT
        }
        if (settings.containsKey('scanIndexForward')) {
            query.scanIndexForward = settings.scanIndexForward
        } else {
            query.scanIndexForward = false
        }
        return query
    }

    /**
     * Transform a simple map (Map<Object, Object>) into a {@link Map} of
     * {@link com.amazonaws.services.dynamodbv2.model.AttributeValue} (Map<Object, AttributeValue>).
     *
     * @param map the {@link Map} to transform
     * @return an exclusiveStartKey ready to use in a {@link com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression}
     */
    @SuppressWarnings('Instanceof')
    static protected Map buildStartKey(Map map) {
        return map.inject([:]) { startKey, it ->
            if (it.value instanceof AttributeValue) {
                startKey[it.key] = it.value
            } else {
                startKey[it.key] = buildAttributeValue(it.value)
            }
            startKey
        } as Map
    }

    private DynamoDBQueryExpression buildQuery(
        Object hashKey,
        Map settings,
        Map<String, Condition> rangeKeyConditions,
        String indexName
    ) {
        DynamoDBQueryExpression query = buildQueryExpression(hashKeyName, hashKey, settings)
        query.hashKeyValues = metadata.itemClass.newInstance((hashKeyName): hashKey)
        if (rangeKeyConditions) {
            query.rangeKeyConditions = rangeKeyConditions
        }
        if (indexName) {
            query.indexName = indexName
        }
        return query
    }

    @SuppressWarnings('ParameterCount')
    private QueryResultPage fetchResults(
        QueryResultPage resultPage,
        Map<String, Condition> rangeKeyConditions,
        String indexName,
        Map settings,
        Object hashKey,
        long readCapacityUnit
    ) {
        if (resultPage && (rangeKeyConditions || indexName) && !settings.batchGetDisabled) {
            // Indexes result only provides hash+range attributes, we need to batch get all items
            List rangeKeys = resultPage.results.collect { it[rangeKeyName] }
            if (rangeKeys) {
                resultPage.results = getAll(hashKey, rangeKeys, settings + [readCapacityUnit: readCapacityUnit])
            }
        }
        return resultPage
    }

}
