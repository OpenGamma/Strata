/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The contract code for an Exchange Traded Derivative (ETD).
 * <p>
 * This is the code supplied by the exchange for use in clearing and margining, such as in SPAN.
 */
public final class EtdContractCode
    extends TypedString<EtdContractCode> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param name  the name
   * @return a type instance with the specified name
   */
  @FromString
  public static EtdContractCode of(String name) {
    return new EtdContractCode(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private EtdContractCode(String name) {
    super(name);
  }

}
