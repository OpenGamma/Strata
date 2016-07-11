/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.option;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The type of a strike.
 * <p>
 * The strike of option instruments is represented in different ways.
 * For example, the strike types include delta, moneyness, log-moneyness, and strike itself.
 */
public final class StrikeType
    extends TypedString<StrikeType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * The type of a simple strike.
   * @see SimpleStrike
   */
  public static final StrikeType STRIKE = of("Strike");
  /**
   * The type of a strike based on absolute delta.
   * @see DeltaStrike
   */
  public static final StrikeType DELTA = of("Delta");
  /**
   * The type of a strike based on moneyness, defined as {@code strike/forward}.
   * @see MoneynessStrike
   */
  public static final StrikeType MONEYNESS = of("Moneyness");
  /**
   * The type of a strike based on log-moneyness, defined as the {@code ln(strike/forward)}.
   * @see LogMoneynessStrike
   */
  public static final StrikeType LOG_MONEYNESS = of("LogMoneyness");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Strike types may contain any character, but must not be empty.
   *
   * @param name  the name of the field
   * @return the type with the specified name
   */
  @FromString
  public static StrikeType of(String name) {
    return new StrikeType(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the field
   */
  private StrikeType(String name) {
    super(name);
  }

}
