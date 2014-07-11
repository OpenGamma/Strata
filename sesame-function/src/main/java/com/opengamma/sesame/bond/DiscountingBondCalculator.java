/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorDelegate;
import com.opengamma.analytics.financial.provider.calculator.issuer.PresentValueIssuerCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.analytics.conversion.BondAndBondFutureTradeConverter;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Discounting calculator for bond.
 */
public class DiscountingBondCalculator implements BondCalculator {

  private static final PresentValueIssuerCalculator PVIC = PresentValueIssuerCalculator.getInstance();

  private final InstrumentDerivative _derivative;

  private final ParameterIssuerProviderInterface _curves;

  public DiscountingBondCalculator(BondTrade trade,
                                   ParameterIssuerProviderInterface curves,
                                   BondAndBondFutureTradeConverter converter,
                                   ZonedDateTime valuationTime) {
    _derivative = createInstrumentDerivative(trade, converter, valuationTime);
    _curves = curves;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVIC));
  }

  private InstrumentDerivative createInstrumentDerivative(BondTrade trade,
                                                          BondAndBondFutureTradeConverter converter,
                                                          ZonedDateTime valuationTime) {
    InstrumentDefinition<?> definition = converter.convert(trade);
    return definition.toDerivative(valuationTime);
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorDelegate<ParameterIssuerProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _curves);
  }
}
