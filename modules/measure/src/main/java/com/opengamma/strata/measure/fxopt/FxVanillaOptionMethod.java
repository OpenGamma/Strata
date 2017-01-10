/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.util.Optional;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.collect.ArgChecker;
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
public enum FxVanillaOptionMethod implements CalculationParameter {

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

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FxVanillaOptionMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
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
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
