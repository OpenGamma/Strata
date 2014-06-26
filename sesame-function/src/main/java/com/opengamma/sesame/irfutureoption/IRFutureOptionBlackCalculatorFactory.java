/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateFutureOptionTradeConverter;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.sesame.CurveDefinitionFn;
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
   * Function used to retrieve the curve definitions from within a multicurve bundle.
   */
  private final CurveDefinitionFn _curveDefinitionFn;
  
  /**
   * Constructs a calculator factory for interest rate future options that will create a Black calculator.
   * @param converter converter used to create the definition of the interest rate future option, not null.
   * @param blackProviderFn function used to generate a Black volatility provider, not null.
   * @param definitionToDerivativeConverter converter used to create the derivative of the future option, not null.
   * @param htsFn function used to retrieve the historical prices of the underlying interest rate future.
   * @param curveDefinitionFn function used to retrieve curve definitions for the multicurve
   */
  public IRFutureOptionBlackCalculatorFactory(InterestRateFutureOptionTradeConverter converter,
                                              BlackSTIRFuturesProviderFn blackProviderFn,
                                              FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                              HistoricalTimeSeriesFn htsFn,
                                              CurveDefinitionFn curveDefinitionFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _blackProviderFn = ArgumentChecker.notNull(blackProviderFn, "blackProviderFn");
    _definitionToDerivativeConverter =
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
    _curveDefinitionFn = ArgumentChecker.notNull(curveDefinitionFn, "curveDefinitionFn");
  }

  @Override
  public Result<IRFutureOptionCalculator> createCalculator(Environment env, IRFutureOptionTrade trade) {
    
    Result<Boolean> result = Result.success(true);
    
    IRFutureOptionSecurity security = trade.getSecurity();
    
    Result<BlackSTIRFuturesProviderInterface> blackResult = _blackProviderFn.getBlackSTIRFuturesProvider(env, security);
        
    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    Set<String> curveNames = null;
    
    BlackSTIRFuturesProviderInterface black = null;
    
    IRFutureOptionCalculator calculator = null;
    
    if (Result.allSuccessful(blackResult, fixingsResult)) {
    
      black = blackResult.getValue();
      
      MulticurveProviderInterface multicurveProvider = black.getMulticurveProvider();
      
      curveNames = multicurveProvider.getAllCurveNames();
      
    } else {
      
      result = Result.failure(blackResult, fixingsResult);
     
    }
      
    Map<String, CurveDefinition> curveDefinitions = new HashMap<>();
    for (String curveName : curveNames) {
      Result<CurveDefinition> curveDefinition = _curveDefinitionFn.getCurveDefinition(curveName);
      
      if (curveDefinition.isSuccess()) {
        
        curveDefinitions.put(curveName, curveDefinition.getValue());
        
      } else {
        
        result = Result.failure(result, curveDefinition);
        
      }
            
      calculator = new IRFutureOptionBlackCalculator(trade, 
                                                    _converter, 
                                                    black, 
                                                    env.getValuationTime(), 
                                                    _definitionToDerivativeConverter, 
                                                    fixingsResult.getValue(), 
                                                    curveDefinitions);
      
    }
    if (result.isSuccess()) {
      return Result.success(calculator);
    } else {
      return Result.failure(result);
    }
  }
}
