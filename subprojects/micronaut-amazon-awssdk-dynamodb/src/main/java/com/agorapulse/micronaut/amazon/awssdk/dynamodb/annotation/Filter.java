/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.KeyConditionCollector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
public @interface Filter {

    Operator value() default Operator.EQ;

    String name() default "";

    enum Operator {

        IN_LIST {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.inList(attributeOrIndex, (Collection<?>) firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("IN_LIST is not supported for key conditions");
            }
        },

        EQ {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                if (firstValue == null) {
                    collector.isNull(attributeOrIndex);
                } else if (firstValue instanceof Collection) {
                    collector.inList(attributeOrIndex, (Collection<?>) firstValue);
                } else if (firstValue.getClass().isArray()) {
                    collector.inList(attributeOrIndex, Arrays.asList((Object[]) firstValue));
                } else {
                    collector.eq(attributeOrIndex, firstValue);
                }
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.eq(firstValue);
            }
        },

        NE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.ne(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("NE is not supported for key conditions");
            }
        },

        LE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.le(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.le(firstValue);
            }

        },

        LT {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.lt(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.lt(firstValue);
            }
        },

        GE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.ge(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.ge(firstValue);
            }
        },

        GT {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.gt(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.gt(firstValue);
            }
        },

        SIZE_EQ {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeEq(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_EQ is not supported for key conditions");
            }
        },

        SIZE_NE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeNe(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_NE is not supported for key conditions");
            }
        },

        SIZE_LE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeLe(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_LE is not supported for key conditions");
            }
        },

        SIZE_LT {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeLt(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_LT is not supported for key conditions");
            }
        },

        SIZE_GE {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeGe(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_GE is not supported for key conditions");
            }
        },

        SIZE_GT {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.sizeGt(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("SIZE_GT is not supported for key conditions");
            }
        },

        BETWEEN {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.between(attributeOrIndex, firstValue, secondValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.between(firstValue, secondValue);
            }
        },

        CONTAINS {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.contains(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("CONTAINS is not supported for key conditions");
            }
        },

        NOT_CONTAINS {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.notContains(attributeOrIndex, firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("NOT_CONTAINS is not supported for key conditions");
            }
        },

        TYPE_OF {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.typeOf(attributeOrIndex, (Class<?>) firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                throw new UnsupportedOperationException("TYPE_OF is not supported for key conditions");
            }
        },

        BEGINS_WITH {
            @Override
            public void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.beginsWith(attributeOrIndex, (String) firstValue);
            }

            @Override
            public void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue) {
                collector.beginsWith(String.valueOf(firstValue));
            }
        };

        public abstract void apply(FilterConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue);
        public abstract void apply(KeyConditionCollector<?> collector, String attributeOrIndex, Object firstValue, Object secondValue);

    }

}
