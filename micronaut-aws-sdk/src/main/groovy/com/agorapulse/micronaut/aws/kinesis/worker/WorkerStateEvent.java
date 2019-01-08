package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.WorkerStateChangeListener;

/**
 * Event published for the changes of the state of the Kinesis worker.
 */
public class WorkerStateEvent {

    private final WorkerStateChangeListener.WorkerState state;
    private final String stream;

    public WorkerStateEvent(WorkerStateChangeListener.WorkerState state, String stream) {
        this.state = state;
        this.stream = stream;
    }

    public WorkerStateChangeListener.WorkerState getState() {
        return state;
    }

    public String getStream() {
        return stream;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "WorkerStateEvent{" +
            "state=" + state +
            ", stream='" + stream + '\'' +
            '}';
    }
    // CHECKSTYLE:ON
}
