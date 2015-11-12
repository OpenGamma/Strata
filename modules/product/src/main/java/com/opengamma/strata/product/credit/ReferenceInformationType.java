/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * Defines the type of the CDS underlying that protection applies to.
 */
public enum ReferenceInformationType {

  /**
   * A single-name CDS.
   */
  SINGLE_NAME,
  /**
   * A CDS index.
   */
  INDEX

}
