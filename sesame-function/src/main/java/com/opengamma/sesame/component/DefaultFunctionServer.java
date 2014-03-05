/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

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
  public Results executeOnce(FunctionServerRequest request) {
    View view = _viewFactory.createView(request.getViewConfig(), request.getInputs());
    MarketDataSource marketDataSource = _marketDataFactory.create(request.getMarketDataSpec());
    return view.run(new CycleArguments(request.getValuationTime(), VersionCorrection.LATEST, marketDataSource));
  }
}
