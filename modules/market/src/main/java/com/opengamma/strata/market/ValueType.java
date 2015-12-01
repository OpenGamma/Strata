/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.type.TypedString;

/**
 * The type of a value.
 * <p>
 * The market data system contains many different kinds of value, and this type can be used to identify them.
 * <p>
 * For example, constants are provided for common financial concepts, such as discount factors,
 * zero rates and year fractions. The set of types is fully extensible.
 */
public final class ValueType
    extends TypedString<ValueType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /**
   * Type used when the meaning of each value is not known - 'Unknown'.
   */
  public static final ValueType UNKNOWN = of("Unknown");

  /**
   * Type used when each value is a year fraction relative to a base date - 'YearFraction'.
   */
  public static final ValueType YEAR_FRACTION = of("YearFraction");
  /**
   * Type used when each value is the number of months relative to a base month - 'Months'.
   */
  public static final ValueType MONTHS = of("Months");

  /**
   * Type used when each value is a zero rate - 'ZeroRate'.
   */
  public static final ValueType ZERO_RATE = of("ZeroRate");
  /**
   * Type used when each value is a discount factor - 'DiscountFactor'.
   */
  public static final ValueType DISCOUNT_FACTOR = of("DiscountFactor");
  /**
   * Type used when each value is a price index, as used for inflation products - 'PriceIndex'.
   */
  public static final ValueType PRICE_INDEX = of("PriceIndex");
  /**
   * Type used when each value is an ISDA credit curve value - 'IsdaCredit'.
   */
  public static final ValueType ISDA_CREDIT = of("IsdaCredit");

  /**
   * Type used when each value is a volatility - 'Volatility'.
   */
  public static final ValueType VOLATILITY = of("Volatility");
  /**
   * Type used when each value is a strike - 'Strike'.
   */
  public static final ValueType STRIKE = of("Strike");

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code ValueType} by name.
   * <p>
   * Value types must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the field
   * @return a field with the specified name
   */
  @FromString
  public static ValueType of(String name) {
    return new ValueType(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the field
   */
  private ValueType(String name) {
    super(name, NAME_PATTERN, "Value type must only contain the characters A-Z, a-z, 0-9 and -");
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that this instance equals the specified instance.
   * <p>
   * This returns normally if they are equal.
   * Otherwise, an exception is thrown.
   * 
   * @param other  the instance to check against
   * @param exceptionPrefix  the exception prefix
   */
  public void checkEquals(ValueType other, String exceptionPrefix) {
    if (!this.equals(other)) {
      throw new IllegalArgumentException(Messages.format(
          "{}, expected {} but was {}", exceptionPrefix, other, this));
    }
  }

}
