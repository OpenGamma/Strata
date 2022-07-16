package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

public enum CorporateActionStatus implements NamedEnum {

  PRELIMINARY_UNCONFIRMED("PREU"),

  PRELIMINARY("PREC"),

  COMPLETE("COMP");

  private static final EnumNames<CorporateActionStatus> NAMES = EnumNames.ofManualToString(CorporateActionStatus.class);

  private final String name;

  CorporateActionStatus(String name) {
    this.name = name;
  }

  @FromString
  public static CorporateActionStatus of(String name) {
    return NAMES.parse(name);
  }

  @ToString
  public String toString() {
    return this.name;
  }
}
