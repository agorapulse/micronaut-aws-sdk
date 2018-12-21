package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisListener;
import com.amazonaws.services.kinesis.model.Record;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// tag::all[]
@Singleton                                                                              // <1>
public class KinesisListenerTester {

    @KinesisListener
    public void listenString(String string) {                                           // <2>
        executions.add("EXECUTED: listenString(" + string + ")");
    }

    @KinesisListener
    public void listenRecord(Record record) {                                           // <3>
        executions.add("EXECUTED: listenRecord(" + record + ")");
    }


    @KinesisListener
    public void listenStringRecord(String string, Record record) {                      // <4>
        executions.add("EXECUTED: listenStringRecord(" + string + ", " + record + ")");
    }

    @KinesisListener
    public void listenObject(MyEvent event) {                                           // <5>
        executions.add("EXECUTED: listenObject(" + event + ")");
    }

    @KinesisListener
    public void listenObjectRecord(MyEvent event, Record record) {                      // <6>
        executions.add("EXECUTED: listenObjectRecord(" + event + ", " + record + ")");
    }

    @KinesisListener
    public void listenPogoRecord(Pogo event) {                                          // <7>
        executions.add("EXECUTED: listenPogoRecord(" + event + ")");
    }

    public List<String> getExecutions() {
        return executions;
    }

    public void setExecutions(List<String> executions) {
        this.executions = executions;
    }

    private List<String> executions = new CopyOnWriteArrayList<>();
}
// end::all[]
