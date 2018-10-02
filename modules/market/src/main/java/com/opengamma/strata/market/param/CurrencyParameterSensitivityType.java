/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import org.joda.convert.FromString;

import com.google.common.base.CharMatcher;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.market.ValueType;

/**
 * The type of a sensitivity.
 * <p>
 * There are many possible types of sensitivity, and this type can be used to identify them.
 */
public final class CurrencyParameterSensitivityType
    extends TypedString<CurrencyParameterSensitivityType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Matcher for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final CharMatcher NAME_MATCHER =
      CharMatcher.inRange('A', 'Z')
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('0', '9'))
          .or(CharMatcher.is('-'))
          .precomputed();

  //-------------------------------------------------------------------------
  /**
   * Type used when each sensitivity is a zero rate delta - 'ZeroRateDelta'.
   * This is the first order derivative of {@link ValueType#ZERO_RATE}.
   */
  public static final CurrencyParameterSensitivityType ZERO_RATE_DELTA = of("ZeroRateDelta");
  /**
   * Type used when each sensitivity is a zero rate gamma - 'ZeroRateGamma'.
   * This is the second order derivative of {@link ValueType#ZERO_RATE}.
   */
  public static final CurrencyParameterSensitivityType ZERO_RATE_GAMMA = of("ZeroRateGamma");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Value types must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the field
   * @return a field with the specified name
   */
  @FromString
  public static CurrencyParameterSensitivityType of(String name) {
    return new CurrencyParameterSensitivityType(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the field
   */
  private CurrencyParameterSensitivityType(String name) {
    super(name, NAME_MATCHER, "Sensitivity type must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
