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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alias for {@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable}
 * that can be used to mark immutable classes for DynamoDB mapping with Micronaut bean introspection.
 * 
 * This annotation serves as a more concise alternative to the AWS SDK's {@code @DynamoDbImmutable}
 * annotation while providing the same functionality.
 * 
 * Example usage:
 * <pre>
 * <code>
 * {@literal @}Introspected
 * {@literal @}Immutable(builder = Customer.Builder.class)
 * public class Customer {
 *     {@literal @}DynamoDbPartitionKey
 *     public String accountId() { ... }
 *
 *     {@literal @}DynamoDbSortKey  
 *     public int subId() { ... }
 *
 *     {@literal @}Introspected
 *     public static final class Builder {
 *         public Builder accountId(String accountId) { ... }
 *         public Builder subId(int subId) { ... }
 *         public Customer build() { ... }
 *     }
 * }
 * </code>
 * </pre>
 * 
 * <p>
 * Note: This annotation requires both the immutable class and its builder class to be 
 * annotated with {@code @Introspected} for Micronaut bean introspection to work properly.
 * </p>
 * 
 * @see software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Immutable {
    
    /**
     * The builder class that will be used to create new instances of the immutable class.
     * The builder class must have a no-args constructor and a method that returns an instance
     * of the immutable class (typically called {@code build()}).
     * 
     * @return the builder class
     */
    Class<?> builder();
    
    /**
     * A list of {@link AttributeConverterProvider} classes that should be used to convert
     * attributes when mapping this immutable class.
     * 
     * @return array of attribute converter provider classes
     */
    Class<? extends AttributeConverterProvider>[] converterProviders() default {};
}