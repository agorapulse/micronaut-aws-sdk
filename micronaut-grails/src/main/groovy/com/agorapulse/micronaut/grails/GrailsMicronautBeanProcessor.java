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
import io.micronaut.inject.BeanDefinition;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adds Micronaut beans to a Grails' Spring application context.  This processor will
 * find all of the Micronaut beans of the specified types
 * and add them as beans to the Spring application context.
 * <p>
 * The Grails properties can be translated to Micronaut properties if required.
 *
 * @author jeffbrown
 * @author musketyr
 * @since 1.0
 */
public class GrailsMicronautBeanProcessor implements BeanFactoryPostProcessor, DisposableBean, EnvironmentAware {

    public static class Builder {
        private final Map<String, Qualifier<?>> micronautBeanQualifiers = new LinkedHashMap<>();
        private final PropertyTranslatingCustomizer customizer;

        Builder(PropertyTranslatingCustomizer customizer) {
            this.customizer = customizer;
        }

        public Builder addByType(Class<?> type) {
            return addByQualifiers(NameUtils.decapitalize(type.getSimpleName()), Qualifiers.byType(type));
        }

        public Builder addByType(String grailsBeanName, Class<?>... types) {
            micronautBeanQualifiers.put(grailsBeanName, Qualifiers.byType(types));
            return this;
        }

        public Builder addByStereotype(String grailsBeanName, Class<? extends Annotation> type) {
            micronautBeanQualifiers.put(grailsBeanName, Qualifiers.byStereotype(type));
            return this;
        }

        public Builder addByName(String name) {
            return addByName(name, name);
        }

        public Builder addByName(String grailsBeanName, String micronautName) {
            micronautBeanQualifiers.put(grailsBeanName, Qualifiers.byName(micronautName));
            return this;
        }

        public <T> Builder addByQualifiers(String grailsBeanName, Qualifier<T>... qualifiers) {
            micronautBeanQualifiers.put(grailsBeanName, Qualifiers.byQualifiers(qualifiers));
            return this;
        }

        public GrailsMicronautBeanProcessor build() {
            return new GrailsMicronautBeanProcessor(micronautBeanQualifiers, customizer);
        }
    }

    /**
     * Starts creation of bean processor using default {@link PropertyTranslatingCustomizer#grails()} customizer.
     * @return bean processor builder using {@link PropertyTranslatingCustomizer#grails()}
     */
    public static GrailsMicronautBeanProcessor.Builder builder() {
        return builder(PropertyTranslatingCustomizer.grails());
    }

    /**
     * Starts creation of bean processor using given customizer.
     * @param customizerBuilder customizer being used
     * @return bean processor builder using given cusotomizer
     */
    public static GrailsMicronautBeanProcessor.Builder builder(PropertyTranslatingCustomizer.Builder customizerBuilder) {
        return builder(customizerBuilder.build());
    }

    /**
     * Starts creation of bean processor using given customizer.
     * @param customizer customizer being used
     * @return bean processor builder using given cusotomizer
     */
    public static GrailsMicronautBeanProcessor.Builder builder(PropertyTranslatingCustomizer customizer) {
        return new Builder(customizer);
    }

    private static final String MICRONAUT_BEAN_TYPE_PROPERTY_NAME = "micronautBeanType";
    private static final String MICRONAUT_CONTEXT_PROPERTY_NAME = "micronautContext";
    private static final String MICRONAUT_SINGLETON_PROPERTY_NAME = "micronautSingleton";

    private DefaultBeanContext micronautContext;
    private final Map<String, Qualifier<?>> micronautBeanQualifiers;
    private final PropertyTranslatingCustomizer customizer;
    private Environment environment;

    /**
     * @param customizer properties translation customizer
     * @param qualifiers the names and qualifiers of the Micronaut beans which should be added to the
     *                   Spring application context.
     */
    private GrailsMicronautBeanProcessor(Map<String, Qualifier<?>> qualifiers, PropertyTranslatingCustomizer customizer) {
        this.customizer = customizer;
        this.micronautBeanQualifiers = qualifiers;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            throw new IllegalStateException("Spring environment not set!");
        }

        micronautContext = new GrailsPropertyTranslatingApplicationContext(environment, customizer);

        micronautContext.start();

        for (Map.Entry<String, Qualifier<?>> entry : micronautBeanQualifiers.entrySet()) {
            String name = entry.getKey();
            Qualifier<?> micronautBeanQualifier = entry.getValue();
            Collection<BeanDefinition<?>> beanDefinitions = micronautContext.getBeanDefinitions((Qualifier<Object>) micronautBeanQualifier);

            Optional<BeanDefinition<?>> firstBean = beanDefinitions.stream().findFirst();
            BeanDefinition<?> definition = firstBean.orElseThrow(()-> new IllegalArgumentException("There is no candidate for " + micronautBeanQualifier));

            final BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                .rootBeanDefinition("io.micronaut.spring.beans.MicronautSpringBeanFactory");
            beanDefinitionBuilder.addPropertyValue(MICRONAUT_BEAN_TYPE_PROPERTY_NAME, definition.getBeanType());
            beanDefinitionBuilder.addPropertyValue(MICRONAUT_CONTEXT_PROPERTY_NAME, micronautContext);
            beanDefinitionBuilder.addPropertyValue(MICRONAUT_SINGLETON_PROPERTY_NAME, definition.isSingleton());

            ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
        }
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

}

