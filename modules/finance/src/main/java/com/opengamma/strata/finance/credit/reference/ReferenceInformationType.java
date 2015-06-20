/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.reference;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.RestructuringClause;

/**
 * Supported types of underlyings for a credit default swap
 */
public enum ReferenceInformationType {
  SINGLE_NAME,
  INDEX;

  public static ReferenceInformation singleName(
      StandardId referenceEntityId,
      Currency currency,
      SeniorityLevel seniority,
      RestructuringClause restructuringClause
  ) {
    return SingleNameReferenceInformation.of(
        referenceEntityId,
        seniority,
        currency,
        restructuringClause
    );
  }

  public static ReferenceInformation index(
      StandardId indexId,
      int indexSeries,
      int indexAnnexVersion,
      RestructuringClause restructuringClause
  ) {
    return IndexReferenceInformation.of(
        indexId,
        indexSeries,
        indexAnnexVersion,
        restructuringClause
    );
  }
}
