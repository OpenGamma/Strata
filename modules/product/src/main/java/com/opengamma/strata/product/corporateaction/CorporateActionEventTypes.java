/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.ExtendedEnum;

public final class CorporateActionEventTypes {

  static final ExtendedEnum<CorporateActionEventType> ENUM_LOOKUP = ExtendedEnum.of(CorporateActionEventType.class);

  public static final CorporateActionEventType CASH_DIVIDEND = CorporateActionEventType.of(StandardCorporateActionEventTypes.CASH_DIVIDEND.getName());

  private CorporateActionEventTypes() {
  }
}
