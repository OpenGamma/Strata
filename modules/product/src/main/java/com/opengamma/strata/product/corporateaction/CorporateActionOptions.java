/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.ExtendedEnum;

public final class CorporateActionOptions {

  static final ExtendedEnum<CorporateActionOption> ENUM_LOOKUP = ExtendedEnum.of(CorporateActionOption.class);

  public static final CorporateActionOption CASH = CorporateActionOption.of(StandardCorporateActionOptions.CASH.getName());

  private CorporateActionOptions() {
  }

}
