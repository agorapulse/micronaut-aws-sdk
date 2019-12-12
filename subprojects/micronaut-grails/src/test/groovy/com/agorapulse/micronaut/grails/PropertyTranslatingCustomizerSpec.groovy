/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.grails

import spock.lang.Specification

/**
 * Tests for property translating customizer.
 */
class PropertyTranslatingCustomizerSpec extends Specification {

    void 'custom replacements works'() {
        when:
            PropertyTranslatingCustomizer customizer = PropertyTranslatingCustomizer
                .builder()
                .ignore('foo.bar')
                .replacePrefix('foo.', 'bar.')
                .replacePrefix('foo.', '')
                .replacePrefix('', 'foo.')
                .ignoreAll(/.*\.g.*/)
                .build()

        then:
            customizer.getAlternativeNames(null).isEmpty()
            customizer.getAlternativeNames('').isEmpty()
            customizer.getAlternativeNames('foo.bar').isEmpty()
            customizer.getAlternativeNames('foo.gar').isEmpty()
            customizer.getAlternativeNames('foo.baz') == ['bar.baz', 'baz'] as Set<String>
            customizer.getAlternativeNames('baz') == ['foo.baz'] as Set<String>
    }

    void 'none customizer does nothing'() {
        when:
            PropertyTranslatingCustomizer customizer = PropertyTranslatingCustomizer.none()

        then:
            customizer.getAlternativeNames('foo.bar').isEmpty()
            customizer.getAlternativeNames('foo.baz').isEmpty()
            customizer.getAlternativeNames('baz').isEmpty()
    }

    void 'standard replacements works'() {
        when:
            PropertyTranslatingCustomizer customizer = PropertyTranslatingCustomizer
                .grails()
                .ignore('redis.timeout')
                .build()

        then:
            customizer.getAlternativeNames('redis.timeout').isEmpty()
            customizer.getAlternativeNames('redis.port') == ['grails.redis.port'] as Set<String>
            customizer.getAlternativeNames('micronaut.redis.port') == ['grails.redis.port', 'grails.micronaut.redis.port'] as Set<String>
            customizer.getAlternativeNames('micronaut.server.url') == ['grails.server.url', 'grails.micronaut.server.url'] as Set<String>
            customizer.getAlternativeNames('micronaut.s-url') ==
                ['grails.s-url', 'grails.micronaut.s-url', 'grails.sUrl', 'grails.micronaut.sUrl'] as Set<String>
    }

}
