package com.opengamma.strata.finance.credit;

import com.google.common.base.Preconditions;
import com.opengamma.strata.collect.type.TypedString;
import org.joda.convert.FromString;

public class REDCode extends TypedString<REDCode> {
  private static final long serialVersionUID = 1L;

  @FromString
  public static REDCode of(String name) {
    Preconditions.checkArgument(
        name.length() == 6 || name.length() == 9,
        "RED Code must be exactly 6 or 9 characters"
    );
    return new REDCode(name);
  }

  private REDCode(String name) {
    super(name);
  }

}
