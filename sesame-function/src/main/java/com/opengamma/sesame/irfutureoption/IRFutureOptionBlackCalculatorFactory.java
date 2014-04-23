/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateFutureOptionTradeConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Black calculator for interest rate future options.
 */
public class IRFutureOptionBlackCalculatorFactory implements IRFutureOptionCalculatorFactory {
  
  private final InterestRateFutureOptionTradeConverter _converter;
  
  private final BlackSTIRFuturesProviderFn _blackProviderFn;
  
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  
  private final HistoricalTimeSeriesFn _htsFn;
  
  public IRFutureOptionBlackCalculatorFactory(InterestRateFutureOptionTradeConverter converter,
                                              BlackSTIRFuturesProviderFn blackProviderFn,
                                              FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                              HistoricalTimeSeriesFn htsFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _blackProviderFn = ArgumentChecker.notNull(blackProviderFn, "blackProviderFn");
    _definitionToDerivativeConverter =
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
  }

  @Override
  public Result<IRFutureOptionCalculator> createCalculator(Environment env, IRFutureOptionTrade trade) {
    
    IRFutureOptionSecurity security = (IRFutureOptionSecurity) trade.getSecurity();
    
    Result<BlackSTIRFuturesProviderInterface> blackResult = _blackProviderFn.getBlackSTIRFuturesProvider(env, security);
    
    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(blackResult, fixingsResult)) {
    
      BlackSTIRFuturesProviderInterface black = blackResult.getValue();
      
      IRFutureOptionCalculator calculator = new IRFutureOptionBlackCalculator(trade, _converter, black, env.getValuationTime(), _definitionToDerivativeConverter, fixingsResult.getValue());
      
      return Result.success(calculator);
    }
    return Result.failure(blackResult, fixingsResult);
  }

}
