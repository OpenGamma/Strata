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
 * The type of an Exchange Traded Derivative (ETD) settlement.
 * <p>
 * This is used for Flex options.
 */
public enum EtdSettlementType implements NamedEnum {

  /** Cash settlement. */
  CASH("C"),
  /** Physical settlement. */
  PHYSICAL("E"),
  /** Derivative. */
  DERIVATIVE("D"),
  /** Notional Settlement. */
  NOTIONAL("N"),
  /** Payment-versus-Payment. */
  PAYMENT_VS_PAYMENT("P"),
  /** Stock. */
  STOCK("S"),
  /** Cascade. */
  CASCADE("T"),
  /** Alternate. */
  ALTERNATE("A");

  // helper for name conversions
  private static final EnumNames<EtdSettlementType> NAMES = EnumNames.of(EtdSettlementType.class);

  /**
   * The single letter code used for the settlement type.
   */
  private final String code;

  private EtdSettlementType(String code) {
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
  public static EtdSettlementType of(String name) {
    return NAMES.parse(name);
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
