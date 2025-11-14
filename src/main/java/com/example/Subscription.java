package com.example;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents an ongoing, stream-based connection or subscription to a resource.
 * This interface extends Closeable to mandate resource cleanup
 * and provides a method to check the connection's current status.
 */
public interface Subscription extends Closeable {
    /**
     * Stops and closes the active subscription, releasing any associated network resources.
     * * @throws IOException If an I/O error occurs while attempting to close the subscription.
     */
    @Override
    void close() throws IOException;

    /**
     * Indicates whether the subscription is currently open and actively receiving data.
     * * @return true if the subscription is still active and not yet closed; false otherwise.
     */
    boolean isOpen();
}
