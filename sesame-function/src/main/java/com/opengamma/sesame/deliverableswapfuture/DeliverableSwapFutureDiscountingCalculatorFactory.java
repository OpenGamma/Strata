/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;


import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.DeliverableSwapFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.future.DeliverableSwapFutureSecurity;
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
  
  /**
   * Constructs a discounting calculator factory for deliverable swap futures.
   * @param deliverableSwapFutureTradeConverter the converter used to convert the OG-Financial deliverable swap future to
   *    the OG-Analytic definition, not null.
   * @param definitionToDerivativeConverter the converter used to convert the definition to a derivative, not null.
   * @param discountingMultiCurveCombinerFn the multicurve function, not null.
   * @param htsFn the historical time series function, not null.
   */
  public DeliverableSwapFutureDiscountingCalculatorFactory(DeliverableSwapFutureTradeConverter deliverableSwapFutureTradeConverter,
                                                           FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                           DiscountingMulticurveCombinerFn discountingMultiCurveCombinerFn,
                                                           HistoricalTimeSeriesFn htsFn) {
    _deliverableSwapFutureTradeConverter = ArgumentChecker.notNull(deliverableSwapFutureTradeConverter, "deliverableSwapFutureTradeConverter");
    _definitionToDerivativeConverter = ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _discountingMultiCurveCombinerFn = ArgumentChecker.notNull(discountingMultiCurveCombinerFn, "discountingMultiCurveCombinerFn");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
  }
  
  
  
  @Override
  public Result<DeliverableSwapFutureCalculator> createCalculator(Environment env, DeliverableSwapFutureTrade trade) {
    
    DeliverableSwapFutureSecurity security = trade.getSecurity();
    
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult = 
                      _discountingMultiCurveCombinerFn.createMergedMulticurveBundle(env, 
                                                                                    trade, 
                                                                                    Result.success(new FXMatrix()));
    
    Result<HistoricalTimeSeriesBundle> fixings = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(bundleResult, fixings)) {
      
      MulticurveProviderDiscount bundle = bundleResult.getValue().getFirst();
      
      HistoricalTimeSeriesBundle fixes = fixings.getValue();
      
      DeliverableSwapFutureCalculator calculator = new DeliverableSwapFutureDiscountingCalculator(trade, 
                                                                                                  bundle, 
                                                                                                  _deliverableSwapFutureTradeConverter, 
                                                                                                  env.getValuationTime(), 
                                                                                                  _definitionToDerivativeConverter, 
                                                                                                  fixes);              
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult, fixings);
    }
  }
}
