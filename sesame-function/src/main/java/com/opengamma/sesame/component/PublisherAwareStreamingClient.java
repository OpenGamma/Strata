package com.opengamma.sesame.component;

/**
 * A streaming client that can have results pushed to it by a View.
 */
public interface PublisherAwareStreamingClient extends StreamingClient, StreamingClientResultListener {
  // No methods, this just allows two interfaces to be combined into one
}
