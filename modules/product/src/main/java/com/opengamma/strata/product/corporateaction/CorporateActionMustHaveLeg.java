package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.product.SecurityId;

public interface CorporateActionMustHaveLeg {


  //DPDPDP in med eventtype har
  public abstract SecurityId getSecurityId(); //DPDPDP

  public abstract double getQuantityNeeded();

  public abstract CorporateActionOption getOption();

  public abstract DefaultOption getDefaultOption();


}
