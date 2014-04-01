/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateFutureSecurityConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;

/**
 * Default factory for interest rate future calculators that provides the converter used to convert the security to an
 * OG-Analytics representation.
 */
public class InterestRateFutureCalculatorFactory {

  private final InterestRateFutureSecurityConverter _converter;
  
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  
  public InterestRateFutureCalculatorFactory(InterestRateFutureSecurityConverter converter,
                                             FixedIncomeConverterDataProvider definitionToDerivativeConverter) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _definitionToDerivativeConverter = ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
  }
  
  public InterestRateFutureCalculator createCalculator(Environment env,
                                                       InterestRateFutureSecurity security,
                                                       MulticurveProviderDiscount bundle,
                                                       HistoricalTimeSeriesBundle fixings) {
    return new InterestRateFutureCalculator(security, bundle, _converter, env.getValuationTime(), _definitionToDerivativeConverter, fixings);
  }
  
}
