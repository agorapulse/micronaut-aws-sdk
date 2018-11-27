package com.agorapulse.micronaut.aws.kinesis.client

import com.agorapulse.micronaut.aws.kinesis.AbstractEvent
import com.amazonaws.services.kinesis.model.Record
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Singleton

class KinesisListenerSpec extends Specification {

    @AutoCleanup ApplicationContext context

    void 'kinesis listener is executed'() {
        when:
            context = ApplicationContext.run()
        then:
            noExceptionThrown()

        when:
            KinesisListenerTester tester = context.getBean(KinesisListenerTester)
            10.times {
                if (tester.executions.size() != 5) {
                    Thread.sleep(100)
                }
            }
        then:
            tester.executions
            tester.executions.size() == 5
            tester.executions.any { it?.startsWith('EXECUTED: listenStringRecord') }
            tester.executions.any { it?.startsWith('EXECUTED: listenString') }
            tester.executions.any { it?.startsWith('EXECUTED: listenRecord') }
            tester.executions.any { it?.startsWith('EXECUTED: listenObject') }
            tester.executions.any { it?.startsWith('EXECUTED: listenObjectRecord') }
    }

}

class MyEvent extends AbstractEvent {
    String value
}

@Singleton
class KinesisListenerTester {

    List<String> executions = []

    @KinesisListener('test')
    void listenStringRecord(String string, Record record) {
        executions << "EXECUTED: listenStringRecord($string, $record)".toString()
    }

    @KinesisListener('test')
    void listenString(String string) {
        executions << "EXECUTED: listenString($string)".toString()
    }

    @KinesisListener('test')
    void listenRecord(Record record) {
        executions << "EXECUTED: listenRecord($record)\n".toString()
    }

    @KinesisListener('test')
    void listenObject(MyEvent event) {
        executions << "EXECUTED: listenObject($event)".toString()
    }

    @KinesisListener('test')
    void listenObjectRecord(MyEvent event, Record record) {
        executions << "EXECUTED: listenObjectRecord($event, $record)".toString()
    }

}
