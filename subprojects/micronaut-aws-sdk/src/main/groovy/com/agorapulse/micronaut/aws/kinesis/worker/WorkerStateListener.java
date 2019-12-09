package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.WorkerStateChangeListener;
import io.micronaut.context.event.ApplicationEventListener;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the current state for worker for given stream.
 */
@Singleton
public class WorkerStateListener implements ApplicationEventListener<WorkerStateEvent> {

    private final Map<String, WorkerStateChangeListener.WorkerState> states = new ConcurrentHashMap<>();

    /**
     * Return the current state of the worker.
     * @param stream name of the stream
     * @return the state of the worker or {@link WorkerStateChangeListener.WorkerState#SHUT_DOWN} if unknown
     */
    public WorkerStateChangeListener.WorkerState getState(String stream) {
        return states.getOrDefault(stream, WorkerStateChangeListener.WorkerState.SHUT_DOWN);
    }

    /**
     * Return true if the worker for given stream is ready.
     * @param stream name of the stream
     * @return true if the worker is ready to receive events
     */
    public boolean isReady(String stream) {
        return WorkerStateChangeListener.WorkerState.STARTED.equals(getState(stream));
    }

    @Override
    public void onApplicationEvent(WorkerStateEvent event) {
        states.put(event.getStream(), event.getState());
    }
}
