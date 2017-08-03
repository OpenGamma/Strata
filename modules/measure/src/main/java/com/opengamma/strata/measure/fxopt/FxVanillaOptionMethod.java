/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.util.Optional;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * The method to use for pricing FX vanilla options.
 * <p>
 * This provides the ability to use different methods for pricing FX options.
 * The Black and Vanna-Volga methods are supported.
 * <p>
 * This enum implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public enum FxVanillaOptionMethod implements NamedEnum, CalculationParameter {

  /**
   * The Black (lognormal) model.
   * This uses Black volatilities - {@link BlackFxOptionVolatilities}.
   */
  BLACK,
  /**
   * The Vanna-Volga model.
   * This uses Black volatilities based on a smile - {@link BlackFxOptionSmileVolatilities}.
   */
  VANNA_VOLGA;

  // helper for name conversions
  private static final EnumNames<FxVanillaOptionMethod> NAMES = EnumNames.of(FxVanillaOptionMethod.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FxVanillaOptionMethod of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  @Override
  public Optional<CalculationParameter> filter(CalculationTarget target, Measure measure) {
    if (target instanceof FxVanillaOptionTrade) {
      return Optional.of(this);
    }
    return Optional.empty();
  };

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
