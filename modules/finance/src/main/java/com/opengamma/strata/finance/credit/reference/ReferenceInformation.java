/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.reference;

/**
 * Interface representing the underlying of credit default swap
 * (e.g. Single Name Obligation, Index, etc)
 */
public interface ReferenceInformation {

  /**
   * @return value to determine whether the CDS is single name, index, etc.
   */
  ReferenceInformationType getType();

}
