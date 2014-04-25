/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Calculator for Discounting FRAs.
 */
public class DiscountingFRACalculator implements FRACalculator {

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
   * Creates a calculator for a FRA.
   *
   * @param security the fra to calculate values for, not null
   * @param bundle the multicurve bundle, including the curves, not null
   * @param fraConverter converter for transforming a fra into its InstrumentDefinition form, not null
   * @param valuationTime the ZonedDateTime, not null
   */
  public DiscountingFRACalculator(FRASecurity security,
                                  MulticurveProviderDiscount bundle,
                                  FRASecurityConverter fraConverter,
                                  ZonedDateTime valuationTime) {
    ArgumentChecker.notNull(security, "security");
    ArgumentChecker.notNull(fraConverter, "fraConverter");
    ArgumentChecker.notNull(valuationTime, "valuationTime");
    _derivative = createInstrumentDerivative(security, fraConverter, valuationTime);
    _bundle = ArgumentChecker.notNull(bundle, "bundle");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVDC));
  }

  @Override
  public Result<Double> calculateRate() {
    return Result.success(calculateResult(PRDC));
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _bundle);
  }

  private InstrumentDerivative createInstrumentDerivative(FRASecurity security,
                                                          FRASecurityConverter fraConverter,
                                                          ZonedDateTime valuationTime) {
    InstrumentDefinition<?> definition = security.accept(fraConverter);
    return definition.toDerivative(valuationTime);
  }
}
