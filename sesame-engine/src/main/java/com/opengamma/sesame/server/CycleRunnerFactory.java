/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import org.threeten.bp.Duration;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.util.ArgumentChecker;

/**
 * A factory for cycle runners. This class creates all the data
 * required for the runner to be able to execute.
 */
public class CycleRunnerFactory {

  /**
   * Default cycle terminator which does not interrupt the
   * execution of cycles.
   */
  private static final CycleTerminator DEFAULT_CYCLE_TERMINATOR =
      new CycleTerminator() {
        @Override
        public boolean shouldContinue() {
          return true;
        }
      };

  /**
   * Factory used to create the views which will be executed.
   */
  private final ViewFactory _viewFactory;

  /**
   * The manager of market data. This is aware of whether data retrieval will
   * be eager or not and ensures that we eventually get the data we need.
   */
  private final MarketDataFactory _marketDataFactory;

  /**
   * Minimum time period between cycles, not null. This is used to ensure
   * the server does not spin when cycles complete quickly.
   */
  private final Duration _minimumTimeBetweenCycles;

  /**
   * Creates the factory.
   *
   * @param viewFactory factory used to create the views which will be executed, not null
   * @param marketDataFactory  used to handle the market data requirements
   * @param minimumTimeBetweenCycles  minimum time period between cycles, not null
   */
  public CycleRunnerFactory(ViewFactory viewFactory,
                            MarketDataFactory marketDataFactory,
                            Duration minimumTimeBetweenCycles) {
    _viewFactory = ArgumentChecker.notNull(viewFactory, "viewFactory");
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFactory");
    _minimumTimeBetweenCycles = ArgumentChecker.notNull(minimumTimeBetweenCycles, "minimumTimeBetweenCycles");
  }

  /**
   * Creates a cycle runner for the specified request, using the
   * supplied handler to process the results from the cycles. Using
   * this method means a default cycle terminator will be used that
   * will not interrupt the running cycles.
   *
   * @param request the request to be executed by the cycle runner, not null
   * @param handler the handler for the results produced by the runner, not null
   * @return cycle runner primed to execute the required cycles
   */
  public CycleRunner createCycleRunner(FunctionServerRequest<? extends CycleOptions> request,
                                       CycleResultsHandler handler) {
    return createCycleRunner(request, handler, DEFAULT_CYCLE_TERMINATOR);
  }

  /**
   * Creates a cycle runner for the specified request, using the
   * supplied handler to process the results from the cycles and the
   * supplied terminator to potentially allow early exiting from the
   * cycle execution.
   *
   * @param request the request to be executed by the cycle runner, not null
   * @param handler the handler for the results produced by the runner, not null
   * @param cycleTerminator the terminator to potentially allow early exiting from the
   * cycle execution, not null
   * @return cycle runner primed to execute the required cycles
   */
  public CycleRunner createCycleRunner(FunctionServerRequest<? extends CycleOptions> request,
                                       CycleResultsHandler handler,
                                       CycleTerminator cycleTerminator) {

    View view = createView(ArgumentChecker.notNull(request, "request"));

    return new CycleRunner(
        view,
        _marketDataFactory,
        request.getCycleOptions(),
        request.getInputs(),
        ArgumentChecker.notNull(handler, "handler"),
        ArgumentChecker.notNull(cycleTerminator, "cycleTerminator"),
        _minimumTimeBetweenCycles);
  }

  private View createView(FunctionServerRequest<?> request) {
    return _viewFactory.createView(request.getViewConfig(), EngineUtils.getSecurityTypes(request.getInputs()));
  }

}
