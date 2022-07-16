/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import org.joda.convert.FromString;
import org.joda.convert.ToString;


public interface CorporateActionEventType extends Named {

  @FromString
  public static CorporateActionEventType of(String uniqueName) {
    return extendedEnum().lookup(uniqueName);
  }

  public static ExtendedEnum<CorporateActionEventType> extendedEnum() {
    return CorporateActionEventTypes.ENUM_LOOKUP;
  }

  @ToString
  @Override
  public abstract String getName();
}
