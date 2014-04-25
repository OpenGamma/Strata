/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackBondFuturesProviderInterface;
import com.opengamma.financial.analytics.conversion.BondFutureOptionTradeConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.option.BondFutureOptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Black calculator for bond future options.
 */
public class BondFutureOptionBlackCalculatorFactory implements BondFutureOptionCalculatorFactory {
  
  private final BondFutureOptionTradeConverter _converter;
  
  private final BlackBondFuturesProviderFn _blackBondFuturesProviderFn;
  
  private final HistoricalTimeSeriesFn _htsFn;
  
  public BondFutureOptionBlackCalculatorFactory(BondFutureOptionTradeConverter converter,
                                                BlackBondFuturesProviderFn blackBondFuturesProviderFn,
                                                HistoricalTimeSeriesFn htsFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _blackBondFuturesProviderFn = ArgumentChecker.notNull(blackBondFuturesProviderFn, "blackBondFuturesProviderFn");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
  }

  @Override
  public Result<BondFutureOptionCalculator> createCalculator(Environment env, BondFutureOptionTrade trade) {
    
    BondFutureOptionSecurity security = trade.getSecurity();
    
    Result<BlackBondFuturesProviderInterface> blackResult = _blackBondFuturesProviderFn.getBlackBondFuturesProvider(env, security);
    
    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(blackResult, fixingsResult)) {
    
      BlackBondFuturesProviderInterface black = blackResult.getValue();
      
      HistoricalTimeSeriesBundle fixings = fixingsResult.getValue();
      
      BondFutureOptionCalculator calculator = new BondFutureOptionBlackCalculator(trade, _converter, black, env.getValuationTime(), fixings);
      
      return Result.success(calculator);
      
    } else {
      return Result.failure(blackResult, fixingsResult);
    }
  }

}
