package com.agorapulse.micronaut.aws.dynamodb.intro

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import io.micronaut.context.ApplicationContext
import org.junit.Rule
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Singleton

/**
 * Tests that guarantees that more than one DynamoDB service can be injected into the service which was previously
 * not working due the following bug.
 *
 * @see https://github.com/micronaut-projects/micronaut-core/issues/1851
 */
class IntroductionIssueSpec extends Specification {

    @AutoCleanup ApplicationContext context

    @Rule Dru dru = Dru.steal(this)

    IDynamoDBMapper mapper = DynamoDB.createMapper(dru)

    void setup() {
        dru.add(new IntroProblemEntity(hashKey: 'hash1'))
        dru.add(new IntroProblemEntity2(hashKey: 'hash2'))

        context = ApplicationContext.build().build()
        context.registerSingleton(IDynamoDBMapper, mapper)
        context.start()
    }

    void 'can inject two data services'() {
        given:
            Injected service = context.getBean(Injected)
        when:
            service.loadEntities()
        then:
            noExceptionThrown()
    }

}

@Singleton
class Injected {
    final IntroProblemEntity2DBService introProblemEntity2DBService
    final IntroProblemEntityDBService introProblemEntityDBService

    Injected(IntroProblemEntity2DBService introProblemEntity2DBService, IntroProblemEntityDBService introProblemEntityDBService) {
        this.introProblemEntity2DBService = introProblemEntity2DBService
        this.introProblemEntityDBService = introProblemEntityDBService
    }

    void loadEntities() {
        assert introProblemEntityDBService.load('hash') == null
        assert introProblemEntity2DBService.load('hash') == null
    }

}
