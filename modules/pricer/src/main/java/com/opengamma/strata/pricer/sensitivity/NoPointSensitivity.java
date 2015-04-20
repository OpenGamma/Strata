/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An empty implementation of the point sensitivity builder, used where there is no sensitivity.
 */
final class NoPointSensitivity
    implements PointSensitivityBuilder {

  /**
   * Singleton instance.
   */
  static final PointSensitivityBuilder INSTANCE = new NoPointSensitivity();

  /**
   * Restricted constructor.
   */
  private NoPointSensitivity() {
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder withCurrency(Currency currency) {
    return this;
  }

  @Override
  public PointSensitivityBuilder mapSensitivity(DoubleUnaryOperator operator) {
    return this;
  }

  @Override
  public PointSensitivityBuilder combinedWith(PointSensitivityBuilder other) {
    return ArgChecker.notNull(other, "other");
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination;
  }

  @Override
  public String toString() {
    return "NoPointSensitivity";
  }

}
