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
        String message = "EXECUTED: listenString(" + string + ")";
        logExecution(message);
    }

    @KinesisListener
    public void listenRecord(Record record) {                                           // <3>
        logExecution("EXECUTED: listenRecord(" + record + ")");
    }


    @KinesisListener
    public void listenStringRecord(String string, Record record) {                      // <4>
        logExecution("EXECUTED: listenStringRecord(" + string + ", " + record + ")");
    }

    @KinesisListener
    public void listenObject(MyEvent event) {                                           // <5>
        logExecution("EXECUTED: listenObject(" + event + ")");
    }

    @KinesisListener
    public void listenObjectRecord(MyEvent event, Record record) {                      // <6>
        logExecution("EXECUTED: listenObjectRecord(" + event + ", " + record + ")");
    }

    @KinesisListener
    public void listenPogoRecord(Pogo event) {                                          // <7>
        logExecution("EXECUTED: listenPogoRecord(" + event + ")");
    }

    public List<String> getExecutions() {
        return executions;
    }

    public void setExecutions(List<String> executions) {
        this.executions = executions;
    }

    private void logExecution(String message) {
        executions.add(message);
        System.err.println(message);
    }

    private List<String> executions = new CopyOnWriteArrayList<>();
}
// end::all[]
