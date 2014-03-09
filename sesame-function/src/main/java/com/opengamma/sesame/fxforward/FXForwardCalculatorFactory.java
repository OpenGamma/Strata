/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;

/**
 * Factory class for creating instances of {@link FXForwardCalculator}.
 */
public class FXForwardCalculatorFactory {


  private final FinancialSecurityVisitor<InstrumentDefinition<?>> _securityConverter;

  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;

  public FXForwardCalculatorFactory(FinancialSecurityVisitor<InstrumentDefinition<?>> securityConverter,
                                    FixedIncomeConverterDataProvider definitionToDerivativeConverter) {
    _securityConverter = securityConverter;
    _definitionToDerivativeConverter = definitionToDerivativeConverter;
  }

  public FXForwardCalculator createCalculator(FXForwardSecurity security,
                                              MulticurveProviderDiscount bundle,
                                              CurveBuildingBlockBundle mergedJacobianBundle) {
    return new FXForwardCalculator(security, bundle, mergedJacobianBundle, _securityConverter, _definitionToDerivativeConverter);
  }
}
