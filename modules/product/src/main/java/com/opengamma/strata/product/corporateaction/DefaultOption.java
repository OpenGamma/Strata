
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

public enum DefaultOption implements NamedEnum {

  YES("Y"),

  NO("N");

  private static final EnumNames<DefaultOption> NAMES = EnumNames.ofManualToString(DefaultOption.class);

  private final String name;

  DefaultOption(String name) {
    this.name = name;
  }

  @FromString
  public static DefaultOption of(String name) {
    return NAMES.parse(name);
  }

  @Override @ToString
  public String toString() {
    return this.name;
  }

  public boolean isDefault(){
    return this == YES;
  }
}
