/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.util.money.MultipleCurrencyAmount;

public class DiscountingCurrencyExposureCalculator {

  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyAmount> CALCULATOR = PresentValueDiscountingCalculator.getInstance();

/*  public FunctionResult<MultipleCurrencyAmount> calculateCurrencyExposure() {

    final MulticurveProviderInterface data = getMergedProviders(inputs, fxMatrix);
    final ValueRequirement desiredValue = Iterables.getOnlyElement(desiredValues);
    final ValueProperties properties = desiredValue.getConstraints().copy().get();
    final MultipleCurrencyAmount mca = derivative.accept(CALCULATOR, data);
    final ValueSpecification spec = new ValueSpecification(FX_CURRENCY_EXPOSURE, target.toSpecification(), properties);
    return Collections.singleton(new ComputedValue(spec, mca));

  }*/
}
