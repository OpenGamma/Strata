/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fedfundsfuture;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FederalFundsFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Discounting calculator factory for federal funds futures.
 */
public class FedFundsFutureDiscountingCalculatorFactory implements FedFundsFutureCalculatorFactory {

  private final FederalFundsFutureTradeConverter _converter;
  
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;
  
  private final HistoricalTimeSeriesFn _htsFn;
  
  public FedFundsFutureDiscountingCalculatorFactory(FederalFundsFutureTradeConverter converter,
                                                    FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                    DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn,
                                                    HistoricalTimeSeriesFn htsFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _definitionToDerivativeConverter =
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _discountingMulticurveCombinerFn =
        ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _htsFn =
        ArgumentChecker.notNull(htsFn, "htsFn");
  }
  
  @Override
  public Result<FedFundsFutureCalculator> createCalculator(Environment env, FedFundsFutureTrade trade) {

    FinancialSecurity security = (FinancialSecurity) trade.getSecurity();
    
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(bundleResult, fixingsResult)) {
    
      MulticurveProviderDiscount bundle = bundleResult.getValue().getFirst();
    
      HistoricalTimeSeriesBundle fixings = fixingsResult.getValue();
    
      FedFundsFutureCalculator calculator = new FedFundsFutureDiscountingCalculator(trade, bundle, _converter, env.getValuationTime(), _definitionToDerivativeConverter, fixings);
      
      return Result.success(calculator);
      
    } else {
      
      return Result.failure(bundleResult, fixingsResult);
    }
  }

}
