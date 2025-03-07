package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.TimeToLive;
import io.micronaut.core.annotation.Introspected;

import java.time.LocalDateTime;

@Introspected
public class EntityWithTtlOnDateField {

    @PartitionKey
    private Long id;

    @SortKey
    private Long sortKey;

    @TimeToLive(value = "6d")
    private LocalDateTime created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSortKey() {
        return sortKey;
    }

    public void setSortKey(Long sortKey) {
        this.sortKey = sortKey;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
