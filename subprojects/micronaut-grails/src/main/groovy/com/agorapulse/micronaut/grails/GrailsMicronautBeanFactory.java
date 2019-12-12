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
package com.agorapulse.micronaut.grails;

import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.context.exceptions.BeanInstantiationException;
import org.springframework.beans.factory.FactoryBean;

import java.util.Optional;

/**
 * A spring FactoryBean for adding Micronaut beans to a
 * Spring application context.
 *
 * @since 1.0
 */
class GrailsMicronautBeanFactory implements FactoryBean {

    private Class micronautBeanType;
    private DefaultApplicationContext micronautContext;
    private Qualifier<Object> micronautQualifier;
    private boolean isMicronautSingleton;

    /**
     * @param micronautBeanType The type of bean this factory will create
     */
    public void setMicronautBeanType(Class micronautBeanType) {
        this.micronautBeanType = micronautBeanType;
    }

    /**
     * @param micronautContext The Micronaut application context
     */
    public void setMicronautContext(DefaultApplicationContext micronautContext) {
        this.micronautContext = micronautContext;
    }

    /**
     *
     * @param isMicronautSingleton indicates if the Micronaut bean is a singleton
     */
    public void setMicronautSingleton(boolean isMicronautSingleton) {
        this.isMicronautSingleton = isMicronautSingleton;
    }

    /**
     * @param micronautQualifier micronaut qualifier of the bean
     */
    public void setMicronautQualifier(Qualifier<Object> micronautQualifier) {
        this.micronautQualifier = micronautQualifier;
    }

    @Override
    public Object getObject() throws Exception {
        Optional bean = micronautContext.findBean(micronautBeanType, micronautQualifier);
        if (bean.isPresent()) {
            return bean.get();
        }

        throw new BeanInstantiationException("Could Not Create Bean [" + micronautBeanType + "]");
    }

    @Override
    public Class<?> getObjectType() {
        return micronautBeanType;
    }

    @Override
    public boolean isSingleton() {
        return isMicronautSingleton;
    }
}
