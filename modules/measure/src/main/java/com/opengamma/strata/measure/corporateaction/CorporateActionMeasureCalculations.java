/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.corporateaction;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.corporateaction.AnnouncementCorporateActionPosition;
import com.opengamma.strata.product.corporateaction.CorporateActionWillGetCashLeg;
import com.opengamma.strata.product.corporateaction.CorporateActionOptions;

/**
 * Multi-scenario measure calculations for simple security trades and positions.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class CorporateActionMeasureCalculations {

  // restricted constructor
  private CorporateActionMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyAmount getGrossCurrencyAmount(
      AnnouncementCorporateActionPosition corporateActionAnnouncementPosition) {

    CorporateActionWillGetCashLeg cashResultingLeg =
        (CorporateActionWillGetCashLeg) corporateActionAnnouncementPosition.getProduct().getCorporateActionLeg(CorporateActionOptions.CASH).get()
            .getCorporateActionWillGetLegs().get(0);
    CurrencyAmount amountPerShare =
        cashResultingLeg
            .getCurrencyAmount();

    return amountPerShare.multipliedBy(new Double(corporateActionAnnouncementPosition.getQuantity()).longValue());
  }


}
