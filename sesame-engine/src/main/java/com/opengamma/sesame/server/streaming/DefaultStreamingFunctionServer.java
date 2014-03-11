/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
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
   * Factory used to create the views which will be executed.
   */
  private final ViewFactory _viewFactory;

  /**
   * Factory for the market data to be used. The market data type will be
   * defined by the specification from the incoming request.
   */
  private final MarketDataFactory _marketDataFactory;

  /**
   * Executor service for running the streaming client.
   */
  // todo this should probably be passed in so it is controlled by config
  private final ExecutorService _executorService = Executors.newFixedThreadPool(5);

  /**
   * Construct the server.
   *
   * @param functionServer the basic (non-streaming) function server, not null
   * @param viewFactory factory used to create the views which will be executed, not null
   * @param marketDataFactory factory for the market data to be used, not null
   */
  public DefaultStreamingFunctionServer(FunctionServer functionServer,
                                        ViewFactory viewFactory,
                                        MarketDataFactory marketDataFactory) {
    _functionServer = ArgumentChecker.notNull(functionServer, "functionServer");
    _viewFactory = ArgumentChecker.notNull(viewFactory, "viewFactory");
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFactory");
  }

  @Override
  public StreamingClient createStreamingClient(FunctionServerRequest<GlobalCycleOptions> request) {

    ArgumentChecker.notNull(request, "request");

    // Build the view first so we can be reasonably confident it's all going to work
    View view = _viewFactory.createView(request.getViewConfig(), request.getInputs());

    final UniqueId clientId = createId();
    PublisherAwareStreamingClient streamingClient = new DefaultStreamingClient(clientId);

    s_logger.info("Setting up streaming task for client: {}", clientId);
    _executorService.execute(createStreamingTask(streamingClient, view, request.getCycleOptions()));
    return streamingClient;
  }

  private Runnable createStreamingTask(final PublisherAwareStreamingClient streamingClient,
                                       final View view, final GlobalCycleOptions globalCycleOptions) {

    return new Runnable() {
      @Override
      public void run() {

        final UniqueId clientId = streamingClient.getUniqueId();
        s_logger.info("Starting streaming task for client: {}", clientId);

        // todo - check if live data is being used and wait for data to be available
        for (Iterator<IndividualCycleOptions> it = globalCycleOptions.iterator(); streamingClient.isRunning() && it.hasNext();) {
          IndividualCycleOptions cycleOptions = it.next();

          MarketDataSource marketDataSource = _marketDataFactory.create(cycleOptions.getMarketDataSpec());

          Results results = view.run(new CycleArguments(cycleOptions.getValuationTime(),
                                                        VersionCorrection.LATEST,
                                                        marketDataSource));
          s_logger.debug("Sending out cycle results to client: {}", clientId);
          streamingClient.resultsReceived(results);
        }

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
