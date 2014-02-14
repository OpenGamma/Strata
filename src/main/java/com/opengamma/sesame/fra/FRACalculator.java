/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.money.MultipleCurrencyAmount;

public class FRACalculator {

  /**
   * Calculator for present value.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();

  /**
   * Calculator for par rate.
   */
  private static final ParRateDiscountingCalculator PRDC = ParRateDiscountingCalculator.getInstance();

  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;

  /**
   * The multicurve bundle.
   */
  private final MulticurveProviderDiscount _bundle;

  /**
   * The Jacobian bundle.
   */
  private final CurveBuildingBlockBundle _mergedJacobianBundle;

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _fraConverter;

  /**
   * Provides the valuation time to perform calculations as at.
   */
  private final ValuationTimeFn _valuationTimeFn;

  public FRACalculator(FRASecurity security,
                       MulticurveProviderDiscount bundle,
                       CurveBuildingBlockBundle mergedJacobianBundle,
                       FRASecurityConverter fraConverter,
                       ValuationTimeFn valuationTimeFn) {

    _derivative = createInstrumentDerivative(security);
    _bundle = bundle;
    _mergedJacobianBundle = mergedJacobianBundle;
    _fraConverter = fraConverter;
    _valuationTimeFn = valuationTimeFn;
  }

  public MultipleCurrencyAmount calculatePV() {
    return calculateResult(PVDC);
  }

  public double calculateRate() {
    return calculateResult(PRDC);
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _bundle);
  }

  private InstrumentDerivative createInstrumentDerivative(FRASecurity security) {
    InstrumentDefinition<?> definition = security.accept(_fraConverter);
    return definition.toDerivative(_valuationTimeFn.getTime());
  }
}
