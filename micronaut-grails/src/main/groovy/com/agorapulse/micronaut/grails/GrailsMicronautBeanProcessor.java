/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agorapulse.micronaut.grails;

import io.micronaut.context.DefaultBeanContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

/**
 * Adds Micronaut beans to a Grails' Spring application context.  This processor will
 * find all of the Micronaut beans of the specified types
 * and add them as beans to the Spring application context.
 *
 * The Grails properties can be translated to Micronaut properties if required.
 *
 * @author jeffbrown
 * @author musketyr
 * @since 1.0
 */
public class GrailsMicronautBeanProcessor implements BeanFactoryPostProcessor, DisposableBean, EnvironmentAware {

    private static final String MICRONAUT_BEAN_TYPE_PROPERTY_NAME = "micronautBeanType";
    private static final String MICRONAUT_CONTEXT_PROPERTY_NAME = "micronautContext";
    private static final String MICRONAUT_SINGLETON_PROPERTY_NAME = "micronautSingleton";

    private DefaultBeanContext micronautContext;
    private final List<Class<?>> micronautBeanQualifierTypes;
    private final PropertyTranslatingCustomizer customizer;
    private Environment environment;

    /**
     * @param customizer properties translation customizer
     * @param qualifierTypes The types associated with the
     *                   Micronaut beans which should be added to the
     *                   Spring application context.
     */
    public GrailsMicronautBeanProcessor(PropertyTranslatingCustomizer customizer, Class<?>... qualifierTypes) {
        this.customizer = customizer;
        this.micronautBeanQualifierTypes = Arrays.asList(qualifierTypes);
    }

    public GrailsMicronautBeanProcessor(Class<?>... qualifierTypes) {
        this(PropertyTranslatingCustomizer.grails().build(), qualifierTypes);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            throw new IllegalStateException("Spring environment not set!");
        }

        micronautContext = new GrailsPropertyTranslatingApplicationContext(environment, customizer);

        micronautContext.start();

        micronautBeanQualifierTypes
                .forEach(micronautBeanQualifierType -> {
                    Qualifier<Object> micronautBeanQualifier;
                    if (micronautBeanQualifierType.isAnnotation()) {
                        micronautBeanQualifier = Qualifiers.byStereotype((Class<? extends Annotation>) micronautBeanQualifierType);
                    } else {
                        micronautBeanQualifier = Qualifiers.byType(micronautBeanQualifierType);
                    }
                    micronautContext.getBeanDefinitions(micronautBeanQualifier)
                            .forEach(definition -> {
                                final BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                                        .rootBeanDefinition("io.micronaut.spring.beans.MicronautSpringBeanFactory");
                                beanDefinitionBuilder.addPropertyValue(MICRONAUT_BEAN_TYPE_PROPERTY_NAME, definition.getBeanType());
                                beanDefinitionBuilder.addPropertyValue(MICRONAUT_CONTEXT_PROPERTY_NAME, micronautContext);
                                beanDefinitionBuilder.addPropertyValue(MICRONAUT_SINGLETON_PROPERTY_NAME, definition.isSingleton());
                                String name = definition.getName();
                                if (name.equals(definition.getBeanType().getName())) {
                                    name = NameUtils.decapitalize(definition.getBeanType().getSimpleName());
                                }
                                ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
                            });
                });
    }

    @Override
    public void destroy() {
        if (micronautContext != null) {
            micronautContext.close();
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns the property name representation of the given name.
     *
     * Ported from grails.util.GrailsNameUtils
     *
     * @param name The name to convert
     * @return The property name representation
     */

}

