/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;

/**
 * Server capable of executing view requests.
 */
public class DefaultFunctionServer implements FunctionServer {

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
   * Construct the server.
   *
   * @param viewFactory factory used to create the views which will be executed, not null
   * @param marketDataFactory factory for the market data to be used, not null
   */
  public DefaultFunctionServer(ViewFactory viewFactory, MarketDataFactory marketDataFactory) {
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFnFactory");
    _viewFactory = ArgumentChecker.notNull(viewFactory, "viewFactory");
  }

  @Override
  public Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request) {
    View view = _viewFactory.createView(request.getViewConfig(), request.getInputs());
    IndividualCycleOptions cycleOptions = request.getCycleOptions();
    MarketDataSource marketDataSource = _marketDataFactory.create(cycleOptions.getMarketDataSpec());
    return view.run(new CycleArguments(cycleOptions.getValuationTime(), VersionCorrection.LATEST, marketDataSource));
  }

  @Override
  public List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request) {
    View view = _viewFactory.createView(request.getViewConfig(), request.getInputs());
    GlobalCycleOptions globalCycleOptions = request.getCycleOptions();

    // todo - this implementation should share some parts with DefaultStreamingFunctionServer
    List<Results> results = new ArrayList<>();

    for (IndividualCycleOptions cycleOptions : globalCycleOptions) {

      MarketDataSource marketDataSource = _marketDataFactory.create(cycleOptions.getMarketDataSpec());
      results.add(view.run(new CycleArguments(cycleOptions.getValuationTime(),
                                              VersionCorrection.LATEST,
                                              marketDataSource)));
    }

    return results;
  }
}
