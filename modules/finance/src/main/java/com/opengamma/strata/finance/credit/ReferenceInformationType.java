/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.id.StandardId;

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
  INDEX;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance for a single-name.
   * 
   * @param referenceEntityId  the identifier of the single-name entity that protection applies to
   * @param currency  the currency of the single-name
   * @param seniority  the applicable seniority
   * @param restructuringClause  the restructuring clause
   * @return the reference
   */
  public static ReferenceInformation singleName(
      StandardId referenceEntityId,
      Currency currency,
      SeniorityLevel seniority,
      RestructuringClause restructuringClause) {

    return SingleNameReferenceInformation.of(
        referenceEntityId,
        seniority,
        currency,
        restructuringClause);
  }

  /**
   * Creates an instance for a CDS index.
   * 
   * @param indexId  the identifier of the index that protection applies to
   * @param indexSeries  the series of the index
   * @param indexAnnexVersion  the version of the index
   * @param restructuringClause  the restructuring clause
   * @return the reference
   */
  public static ReferenceInformation index(
      StandardId indexId,
      int indexSeries,
      int indexAnnexVersion,
      RestructuringClause restructuringClause) {

    return IndexReferenceInformation.of(
        indexId,
        indexSeries,
        indexAnnexVersion,
        restructuringClause);
  }

}
