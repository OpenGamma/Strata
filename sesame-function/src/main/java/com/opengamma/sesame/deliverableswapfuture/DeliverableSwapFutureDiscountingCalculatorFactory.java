/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.DeliverableSwapFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default factory for deliverable swap future calculators that provides the converter used to convert the security to an
 * OG-Analytics representation.
 */
public class DeliverableSwapFutureDiscountingCalculatorFactory implements DeliverableSwapFutureCalculatorFactory {

  private final DeliverableSwapFutureTradeConverter _deliverableSwapFutureTradeConverter;
  
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  
  private final DiscountingMulticurveCombinerFn _discountingMultiCurveCombinerFn;
  
  private final HistoricalTimeSeriesFn _htsFn;
  
  private final CurveDefinitionFn _curveDefinitionFn; 
  
  
  public DeliverableSwapFutureDiscountingCalculatorFactory(DeliverableSwapFutureTradeConverter deliverableSwapFutureTradeConverter,
                                                           FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                           DiscountingMulticurveCombinerFn discountingMultiCurveCombinerFn,
                                                           HistoricalTimeSeriesFn htsFn,
                                                           CurveDefinitionFn curveDefinitionFn) {
    _deliverableSwapFutureTradeConverter = 
        ArgumentChecker.notNull(deliverableSwapFutureTradeConverter, "deliverableSwapFutureTradeConverter");
    _definitionToDerivativeConverter =
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _discountingMultiCurveCombinerFn = 
        ArgumentChecker.notNull(discountingMultiCurveCombinerFn, "discountingMultiCurveCombinerFn");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
    _curveDefinitionFn = ArgumentChecker.notNull(curveDefinitionFn, "curveDefinitionFn");
  }
  
  
  
  @Override
  public Result<DeliverableSwapFutureCalculator> createCalculator(Environment env, DeliverableSwapFutureTrade trade) {
    
    Result<Boolean> result = Result.success(true);
    FinancialSecurity security = (FinancialSecurity) trade.getSecurity();
    
    MulticurveProviderDiscount bundle = null;
    HistoricalTimeSeriesBundle fixingBundle = null;    
    Map<String, CurveDefinition> curveDefinitions = new HashMap<String, CurveDefinition>();
        
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult = 
                      _discountingMultiCurveCombinerFn.createMergedMulticurveBundle(env, 
                                                                                    security, 
                                                                                    Result.success(new FXMatrix()));
    
    Result<HistoricalTimeSeriesBundle> fixings = _htsFn.getFixingsForSecurity(env, security);
    DeliverableSwapFutureCalculator calculator = null;
    
    if (Result.anyFailures(bundleResult, fixings)) {      
      result = Result.failure(bundleResult, fixings);       
    } else {
      
      bundle = bundleResult.getValue().getFirst();      
      fixingBundle = fixings.getValue();              

      for (String curveName : bundleResult.getValue().getValue().getData().keySet()) {
        Result<CurveDefinition> curveDefinition = _curveDefinitionFn.getCurveDefinition(curveName);
        if (curveDefinition.isSuccess()) {          
          curveDefinitions.put(curveName, curveDefinition.getValue());          
        } else {          
          result = Result.failure(result, Result.failure(curveDefinition));          
        }
      }        
    }
    if (result.isSuccess()) {      
      calculator = new DeliverableSwapFutureDiscountingCalculator(trade, 
                                                                  bundle, 
                                                                  _deliverableSwapFutureTradeConverter, 
                                                                  env.getValuationTime(), 
                                                                  _definitionToDerivativeConverter, 
                                                                  fixingBundle,
                                                                  curveDefinitions);     
            
      return Result.success(calculator);
    } else {
      return Result.failure(result);    
    } 
  }
}
