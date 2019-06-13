/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;

import com.google.common.base.CharMatcher;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.product.common.ExchangeId;

/**
 * The code for a group of ETD contracts, as defined an exchange.
 * <p>
 * This is used to group similar ETD contracts together for the purpose of allocating risk.
 * <p>
 * This code is unique within, and defined by, an exchange.
 * A unique identifier is formed when this code is combined with an {@link ExchangeId},
 * see {@link EtdContractGroupId}.
 */
public final class EtdContractGroupCode
    extends TypedString<EtdContractGroupCode> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Matcher for checking the name.
   * It must only contain printable ASCII characters except ':' and '~'.
   */
  private static final CharMatcher NAME_MATCHER =
      CharMatcher.inRange(' ', '}').and(CharMatcher.isNot(':'));

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
  public static EtdContractGroupCode of(String name) {
    return new EtdContractGroupCode(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private EtdContractGroupCode(String name) {
    super(name, NAME_MATCHER, "Code must only contain printable ASCII characters except ':' and '~'");
  }

  // resolve after deserialization
  private Object readResolve() {
    return of(getName());
  }

}
