/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.general.reference;

/**
 * Interface representing the underlying of credit default swap
 * (e.g. Single Name Obligation, Index, etc)
 */
public interface ReferenceInformation {

  /**
   * @return value to determine whether the CDS is single name, index, etc.
   */
  ReferenceInformationType getType();

  /**
   * @return a string that can be used to key into the proper set of curves and recovery rate for pricing
   */
  String getMarketDataKeyName();

}
