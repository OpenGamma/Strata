/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.List;

import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.marketdata.ProxiedCycleMarketData;
import com.opengamma.util.ArgumentChecker;

/**
 * A simple cycle recorder implementation that captures all market
 * data and component calls made.
 */
public class DefaultCycleRecorder implements CycleRecorder {

  private final ViewConfig _viewConfig;
  private final List<?> _tradeInputs;
  private final CycleArguments _cycleArguments;
  private final ProxiedCycleMarketData _proxiedCycleMarketData;
  private final ProxiedComponentMap _proxiedComponentMap;

  /**
   * Construct the recorder.
   *
   * @param viewConfig the view config used for the cycle
   * @param tradeInputs the trades/securities used for the cycle
   * @param cycleArguments the cycle arguments used to run the cycle
   * @param proxiedCycleMarketData the market data source that will
   * be used whilst the cycle is running
   * @param proxiedComponentMap the components that will be used
   * whilst the cycle is running
   */
  public DefaultCycleRecorder(ViewConfig viewConfig,
                              List<?> tradeInputs,
                              CycleArguments cycleArguments,
                              ProxiedCycleMarketData proxiedCycleMarketData,
                              ProxiedComponentMap proxiedComponentMap) {

    _viewConfig = ArgumentChecker.notNull(viewConfig, "viewConfig");
    _tradeInputs = ArgumentChecker.notNull(tradeInputs, "tradeInputs");
    _cycleArguments = ArgumentChecker.notNull(cycleArguments, "cycleArguments");
    _proxiedCycleMarketData = ArgumentChecker.notNull(proxiedCycleMarketData, "proxiedCycleMarketData");
    _proxiedComponentMap = ArgumentChecker.notNull(proxiedComponentMap, "proxiedComponentMap");
  }

  @Override
  public Results complete(Results results) {
    ViewInputs viewInputs = new ViewInputs(
        _tradeInputs, _viewConfig, _cycleArguments.getFunctionArguments(), _cycleArguments.getValuationTime(),
        _cycleArguments.getScenarioArguments(), _proxiedCycleMarketData.retrieveMarketDataResults(),
        _proxiedComponentMap.retrieveResults());
    return results.withViewInputs(viewInputs);
  }

}
