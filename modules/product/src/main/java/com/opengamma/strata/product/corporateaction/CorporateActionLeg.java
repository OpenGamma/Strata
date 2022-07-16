package com.opengamma.strata.product.corporateaction;

import com.google.common.collect.ImmutableList;

public interface CorporateActionLeg {


  public abstract CorporateActionMustHaveLeg getCorporateActionMustHaveLeg();

  public abstract ImmutableList<CorporateActionWillGetLeg> getCorporateActionWillGetLegs();
}
