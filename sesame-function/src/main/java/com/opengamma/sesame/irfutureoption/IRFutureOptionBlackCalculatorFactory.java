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
  
  /**
   * Converter used to create definition of the interest rate future option.
   */
  private final InterestRateFutureOptionTradeConverter _converter;
  
  /**
   * Function used to generate a Black volatility provider.
   */
  private final BlackSTIRFuturesProviderFn _blackProviderFn;
  
  /**
   * Converter used to create a definition from an interest rate future option.
   */
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  
  /**
   * Function used to retrieve the historical prices of the underlying interest rate future.
   */
  private final HistoricalTimeSeriesFn _htsFn;
  
  /**
   * Constructs a calculator factory for interest rate future options that will create a Black calculator.
   * @param converter converter used to create the definition of the interest rate future option, not null.
   * @param blackProviderFn function used to generate a Black volatility provider, not null.
   * @param definitionToDerivativeConverter converter used to create the derivative of the interest rate future option, not null.
   * @param htsFn function used to retrieve the historical prices of the underlying interest rate future.
   */
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
    
    IRFutureOptionSecurity security = trade.getSecurity();
    
    Result<BlackSTIRFuturesProviderInterface> blackResult = _blackProviderFn.getBlackSTIRFuturesProvider(env, trade);
    
    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(blackResult, fixingsResult)) {
    
      BlackSTIRFuturesProviderInterface black = blackResult.getValue();
      
      IRFutureOptionCalculator calculator = new IRFutureOptionBlackCalculator(trade, _converter, black, env.getValuationTime(), _definitionToDerivativeConverter, fixingsResult.getValue());
      
      return Result.success(calculator);
    }
    return Result.failure(blackResult, fixingsResult);
  }

}
