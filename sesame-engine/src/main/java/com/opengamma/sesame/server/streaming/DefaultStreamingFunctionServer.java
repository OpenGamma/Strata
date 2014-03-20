/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.id.UniqueId;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.server.CycleResultsHandler;
import com.opengamma.sesame.server.CycleRunner;
import com.opengamma.sesame.server.CycleRunnerFactory;
import com.opengamma.sesame.server.CycleTerminator;
import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.GlobalCycleOptions;
import com.opengamma.sesame.server.IndividualCycleOptions;
import com.opengamma.util.ArgumentChecker;

/**
 * Streaming server implementation, allowing views to be created and their
 * results to be streamed asynchronously to the client.
 */
public class DefaultStreamingFunctionServer implements StreamingFunctionServer {

  /**
   * Logger for the class.
   */
  private static final Logger s_logger = LoggerFactory.getLogger(DefaultStreamingFunctionServer.class);

  /**
   * Function server, used for the non-streaming calls.
   */
  private final FunctionServer _functionServer;

  /**
   * Factory which will create cycle runners which will run
   * a view to completion.
   */
  private final CycleRunnerFactory _cycleRunnerFactory;

  /**
   * Executor service for running the streaming client.
   */
  // todo this should probably be passed in so it is controlled by config
  private final ExecutorService _executorService = Executors.newFixedThreadPool(5);

  /**
   * Construct the server.
   *
   * @param functionServer the basic (non-streaming) function server, not null
   * @param cycleRunnerFactory factory which will create cycle runners
   * which will run a view to completion
   */
  public DefaultStreamingFunctionServer(FunctionServer functionServer,
                                        CycleRunnerFactory cycleRunnerFactory) {

    _cycleRunnerFactory = ArgumentChecker.notNull(cycleRunnerFactory, "cycleRunnerFactory");
    _functionServer = ArgumentChecker.notNull(functionServer, "functionServer");
  }

  @Override
  public StreamingClient createStreamingClient(FunctionServerRequest<GlobalCycleOptions> request) {

    ArgumentChecker.notNull(request, "request");

    UniqueId clientId = createId();
    final PublisherAwareStreamingClient streamingClient = new DefaultStreamingClient(clientId);

    CycleRunner cycleRunner = _cycleRunnerFactory.createCycleRunner(request,
        new CycleResultsHandler() {
          @Override
          public void handleResults(Results results) {
            streamingClient.resultsReceived(results);
          }
        },
        new CycleTerminator() {
          @Override
          public boolean shouldContinue() {
            return streamingClient.isRunning();
          }
        });

    s_logger.info("Setting up streaming task for client: {}", clientId);
    _executorService.execute(createStreamingTask(cycleRunner, streamingClient));
    return streamingClient;
  }

  private Runnable createStreamingTask(final CycleRunner cycleRunner, final PublisherAwareStreamingClient streamingClient) {

    return new Runnable() {
      @Override
      public void run() {

        final UniqueId clientId = streamingClient.getUniqueId();
        s_logger.info("Starting streaming task for client: {}", clientId);

        cycleRunner.execute();

        // Tell client that we are done so they can exit cleanly
        streamingClient.processCompleted();
        s_logger.info("Stopping streaming task for client: {}", clientId);
        streamingClient.stop();
      }
    };
  }

  @Override
  public Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request) {
    return _functionServer.executeSingleCycle(request);
  }

  @Override
  public List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request) {
    return _functionServer.executeMultipleCycles(request);
  }

  private UniqueId createId() {
    return UniqueId.of("STREAMING_CLIENT", UUID.randomUUID().toString());
  }
}
