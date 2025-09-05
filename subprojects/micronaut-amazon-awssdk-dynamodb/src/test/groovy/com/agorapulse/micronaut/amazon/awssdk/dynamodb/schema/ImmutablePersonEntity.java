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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Address;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.PhoneNumber;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Immutable;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Introspected
@Immutable(builder = ImmutablePersonEntity.Builder.class)
public class ImmutablePersonEntity {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final Integer age;
    private final Map<String, Address> addresses;
    private final List<PhoneNumber> phoneNumbers;
    private final List<String> hobbies;
    private final Set<String> favoriteColors;

    private ImmutablePersonEntity(Builder builder) {
        this.id = builder.id;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.age = builder.age;
        this.addresses = builder.addresses;
        this.phoneNumbers = builder.phoneNumbers;
        this.hobbies = builder.hobbies;
        this.favoriteColors = builder.favoriteColors;
    }

    @PartitionKey
    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Integer getAge() {
        return age;
    }

    public Map<String, Address> getAddresses() {
        return addresses;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public Set<String> getFavoriteColors() {
        return favoriteColors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutablePersonEntity that = (ImmutablePersonEntity) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName) &&
            Objects.equals(age, that.age) &&
            Objects.equals(addresses, that.addresses) &&
            Objects.equals(phoneNumbers, that.phoneNumbers) &&
            Objects.equals(hobbies, that.hobbies) &&
            Objects.equals(favoriteColors, that.favoriteColors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, age, addresses, phoneNumbers, hobbies, favoriteColors);
    }

    @Override
    public String toString() {
        return "ImmutablePersonEntity{" +
            "id=" + id +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", age=" + age +
            ", addresses=" + addresses +
            ", phoneNumbers=" + phoneNumbers +
            ", hobbies=" + hobbies +
            ", favoriteColors=" + favoriteColors +
            '}';
    }

    @Introspected
    public static class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private Integer age;
        private Map<String, Address> addresses;
        private List<PhoneNumber> phoneNumbers;
        private List<String> hobbies;
        private Set<String> favoriteColors;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder age(Integer age) {
            this.age = age;
            return this;
        }

        public Builder addresses(Map<String, Address> addresses) {
            this.addresses = addresses;
            return this;
        }

        public Builder phoneNumbers(List<PhoneNumber> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
            return this;
        }

        public Builder hobbies(List<String> hobbies) {
            this.hobbies = hobbies;
            return this;
        }

        public Builder favoriteColors(Set<String> favoriteColors) {
            this.favoriteColors = favoriteColors;
            return this;
        }

        public ImmutablePersonEntity build() {
            return new ImmutablePersonEntity(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}