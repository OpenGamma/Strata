/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.corporateaction.CorporateActionInfo;
import com.opengamma.strata.product.corporateaction.CorporateActionLeg;
import com.opengamma.strata.product.corporateaction.CorporateActionOption;

import java.util.Optional;

/**
 * A corporate action with additional structured information.
 * <p>
 * A corporate action is any activity that brings material change to an organization and impacts its
 * stakeholders, including shareholders, both common and preferred, as well as bondholders.
 * <p>
 * These events can be either mandatory or voluntary. Mandatory corporate actions are automatically
 * applied to the investments involved while voluntary corporate actions require a shareholders response to be applied.
 * Interest payments, redemption, stock splits, acquisitions and company name changes are examples of mandatory corporate
 * actions, while tender offers, optional dividends and rights issues are examples of voluntary corporate actions.
 * <p>
 * The reference to {@link CorporateActionInfo} captures structured information common to different types
 * of corporate action.
 * <p>
 * For global market practice around corporate actions see the Securities market practice groups (SMPG) documentation
 * https://www.smpg.info/index.php?id=5&tx_filelist_filelist%5Bpath%5D=%2Fdocuments%2F1_Corporate%20Actions%20WG%2FA_Final%20Market%20Practices%2F&tx_filelist_filelist%5Baction%5D=list&tx_filelist_filelist%5Bcontroller%5D=File&cHash=e37f6d0b83d9ced5df744c77af4cc1ef
 * <p>
 * Implementations of this interface must be immutable beans.
 */

public interface CorporateAction
    extends SecuritizedProduct {

  public abstract CorporateActionInfo getCorporateActionInfo();

  public default StandardId getCorporateActionReference(){
    return getCorporateActionInfo().getId().get(); //DPDPDP
  }

  public abstract ImmutableList<CorporateActionLeg> getCorporateActionLegs();

  @Override
  public default SecurityId getSecurityId() {
    return getCorporateActionLegs().get(0).getCorporateActionMustHaveLeg().getSecurityId();
  }

  public default Optional<CorporateActionLeg> getCorporateActionLeg(CorporateActionOption corporateActionOption) {
    return getCorporateActionLegs().stream()
        .filter(leg -> leg.getCorporateActionMustHaveLeg().getOption() == corporateActionOption)
        .findFirst();
  }




}
