/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The type of an Exchange Traded Derivative (ETD) settlement.
 * <p>
 * This is used for Flex options.
 */
public enum EtdSettlementType {

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

  /**
   * The single letter code used for the settlement type.
   */
  private final String code;

  private EtdSettlementType(String code) {
    this.code = code;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static EtdSettlementType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
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
