/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import org.joda.convert.FromString;

import com.google.common.base.CharMatcher;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.TypedString;

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
   * Type used when each value is a forward rate - 'ForwardRate'.
   */
  public static final ValueType FORWARD_RATE = of("ForwardRate");
  /**
   * Type used when each value is a discount factor - 'DiscountFactor'.
   */
  public static final ValueType DISCOUNT_FACTOR = of("DiscountFactor");
  /**
   * Type used when each value is a dividend yield - 'DividendYield'.
   */
  public static final ValueType DIVIDEND_YIELD = of("DividendYield");
  /**
   * Type used when each value is a price index, as used for inflation products - 'PriceIndex'.
   */
  public static final ValueType PRICE_INDEX = of("PriceIndex");
  /**
   * Type used when each value is a recovery rate - 'RecoveryRate'.
   */
  public static final ValueType RECOVERY_RATE = of("RecoveryRate");

  /**
   * Type used when each value is a Black model implied volatility - 'BlackVolatility'.
   */
  public static final ValueType BLACK_VOLATILITY = of("BlackVolatility");
  /**
   * Type used when each value is a Normal (Bachelier) model implied volatility - 'NormalVolatility'.
   */
  public static final ValueType NORMAL_VOLATILITY = of("NormalVolatility");
  /**
   * Type used when each value is a local volatility - 'LocalVolatility'.
   */
  public static final ValueType LOCAL_VOLATILITY = of("LocalVolatility");
  /**
   * Type used when each value is a Price - 'Price'.
   */
  public static final ValueType PRICE = of("Price");
  /**
   * Type used when each value is a strike - 'Strike'.
   */
  public static final ValueType STRIKE = of("Strike");
  /**
   * Type used when each value is simple-moneyness, i.e. the value refers to strike minus forward - 'SimpleMoneyness'.
   */
  public static final ValueType SIMPLE_MONEYNESS = of("SimpleMoneyness");
  /**
   * Type used when each value is log-moneyness, i.e. the value refers to log of strike divided by forward - 'LogMoneyness'.
   */
  public static final ValueType LOG_MONEYNESS = of("LogMoneyness");
  /**
   * Type used when each value is the SABR alpha parameter - 'SabrAlpha'.
   */
  public static final ValueType SABR_ALPHA = of("SabrAlpha");
  /**
   * Type used when each value is the SABR beta parameter - 'SabrBeta'.
   */
  public static final ValueType SABR_BETA = of("SabrBeta");
  /**
   * Type used when each value is the SABR rho parameter - 'SabrRho'.
   */
  public static final ValueType SABR_RHO = of("SabrRho");
  /**
   * Type used when each value is the SABR nu parameter - 'SabrNu'.
   */
  public static final ValueType SABR_NU = of("SabrNu");
  /**
   * Type used when each value is the SABR shift parameter - 'SabrShift'.
   */
  public static final ValueType SABR_SHIFT = of("SabrShift");
  /**
   * Type used when each value is a risk reversal - 'RiskReversal'.
   */
  public static final ValueType RISK_REVERSAL = of("RiskReversal");
  /**
   * Type used when each value is a strangle - 'Strangle'.
   */
  public static final ValueType STRANGLE = of("Strangle");
  /**
   * Type used when each value is a correlation - 'CORRELATION'.
   */
  public static final ValueType CORRELATION = of("CORRELATION");
  /**
   * Type used when each value is the market quote e.g. a par rate for bonds = 'MarketQuote'.
   */
  public static final ValueType MARKET_QUOTE = of("MarketQuote");
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
  public static ValueType of(String name) {
    return new ValueType(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the field
   */
  private ValueType(String name) {
    super(name, NAME_MATCHER, "Value type must only contain the characters A-Z, a-z, 0-9 and -");
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
