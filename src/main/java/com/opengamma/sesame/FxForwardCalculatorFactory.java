/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;

/**
 * Factory class for creating instances of {@link FxForwardCalculator}.
 */
public class FxForwardCalculatorFactory {


  private final FinancialSecurityVisitor<InstrumentDefinition<?>> _securityConverter;

  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;

  private final ValuationTimeProviderFunction _valuationTimeProviderFunction;

  public FxForwardCalculatorFactory(FinancialSecurityVisitor<InstrumentDefinition<?>> securityConverter,
                                    FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                    ValuationTimeProviderFunction valuationTimeProviderFunction) {
    _securityConverter = securityConverter;
    _definitionToDerivativeConverter = definitionToDerivativeConverter;
    _valuationTimeProviderFunction = valuationTimeProviderFunction;
  }

  public FxForwardCalculator createCalculator(FXForwardSecurity security, MulticurveProviderDiscount bundle) {
    return new FxForwardCalculator(security, bundle, _securityConverter, _definitionToDerivativeConverter, _valuationTimeProviderFunction);
  }
}
