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
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFnFactory;
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
  private final MarketDataFnFactory _marketDataFnFactory;

  /**
   * Construct the server.
   *
   * @param viewFactory factory used to create the views which will be executed, not null
   * @param marketDataFnFactory factory for the market data to be used, not null
   */
  public DefaultFunctionServer(ViewFactory viewFactory, MarketDataFnFactory marketDataFnFactory) {
    _marketDataFnFactory = ArgumentChecker.notNull(marketDataFnFactory, "marketDataFnFactory");
    _viewFactory = ArgumentChecker.notNull(viewFactory, "viewFactory");
  }

  @Override
  public Results executeOnce(FunctionServerRequest request) {

    View view = _viewFactory.createView(request.getViewConfig(), request.getInputs());
    MarketDataFn marketDataFn = _marketDataFnFactory.create(request.getMarketDataSpec());
    return view.run(new CycleArguments(request.getValuationTime(), VersionCorrection.LATEST, marketDataFn));
  }
}
