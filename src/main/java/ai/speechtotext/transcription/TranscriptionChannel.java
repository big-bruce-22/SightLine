package ai.speechtotext.transcription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TranscriptionChannel<T> {

    @FunctionalInterface
    public static interface ChannelListener<T> {
        void onReceive(T message);
    }

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();

    private final List<ChannelListener<T>> listeners = new ArrayList<>();

    public void publish(T message) {
        listeners.forEach(t -> t.onReceive(message));
    }

    public void subscribe(ChannelListener<T> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ChannelListener<T> listener) {
        listeners.remove(listener);
    }
    
    public void send(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Channel cannot send null");
        }
        publish(value);
        queue.offer(value);
    }

    public T receive() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Channel receive interrupted", e);
        }
    }

    public boolean hasSent() {
        return !queue.isEmpty();
    }
    
    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
