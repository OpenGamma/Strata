/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The option expiry type, 'American', 'European' or 'Asian'.
 */
public enum EtdOptionType implements NamedEnum {

  /**
   * American option.
   * Can be exercised on any date during its life.
   */
  AMERICAN("A"),
  /**
   * European option.
   * Can be exercised only on a single date.
   */
  EUROPEAN("E"),
  /**
   * Asian option.
   * Payoff depends on the average price over a certain period of time.
   * Sometimes referred as a Traded Average Price Option (TAPO).
   */
  ASIAN("T");

  // helper for name conversions
  private static final EnumNames<EtdOptionType> NAMES = EnumNames.of(EtdOptionType.class);

  /**
   * The single letter code used for the settlement type.
   */
  private final String code;

  private EtdOptionType(String code) {
    this.code = code;
  }

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
  public static EtdOptionType of(String name) {
    return NAMES.parse(name);
  }

  /**
   * Obtains an instance from the specified code.
   * 
   * @param code  the code to parse
   * @return the type
   * @throws IllegalArgumentException if the code is not known
   */
  static EtdOptionType parseCode(String code) {
    switch (code) {
      case "A":
        return AMERICAN;
      case "E":
        return EUROPEAN;
      case "T":
        return ASIAN;
      default:
        throw new IllegalArgumentException("Unknown EtdOptionType code: " + code);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the short code for the type.
   * 
   * @return the short code
   */
  public String getCode() {
    return code;
  }

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
