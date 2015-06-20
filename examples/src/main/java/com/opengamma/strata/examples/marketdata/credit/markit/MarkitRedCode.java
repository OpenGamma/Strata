/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

import com.google.common.base.Preconditions;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.type.TypedString;
import org.joda.convert.FromString;

/**
 * A simple string type to contain a 6 or 9 character Markit RED Code.
 * <p>
 * static utilities to convert from or to StandardIds with a fixed schema
 * <p>
 * http://www.markit.com/product/reference-data-cds
 */
public final class MarkitRedCode extends TypedString<MarkitRedCode> {
  private static final long serialVersionUID = 1L;

  public static final String MARKIT_REDCODE_SCHEME = "MarkitRedCode";

  private MarkitRedCode(String name) {
    super(name);
  }

  /**
   * MarkitRedCode representation of the id
   *
   * @param name RED code
   * @return Typed string MarkitRedCode
   */
  @FromString
  public static MarkitRedCode of(String name) {
    Preconditions.checkArgument(
        name.length() == 6 || name.length() == 9,
        "RED Code must be exactly 6 or 9 characters"
    );
    return new MarkitRedCode(name);
  }

  public StandardId toStandardId() {
    return StandardId.of(MARKIT_REDCODE_SCHEME, this.toString());
  }

  /**
   * Convert from a StandardId once the schema is validated
   *
   * @param id standard id identifying a RED code
   * @return MarkitRedCode representation of the id
   */
  public static MarkitRedCode from(StandardId id) {
    Preconditions.checkArgument(id.getScheme().equals(MARKIT_REDCODE_SCHEME));
    return MarkitRedCode.of(id.getValue());
  }

  /**
   * Creates a StandardId using the proper Markit RED code scheme
   *
   * @param name Markit RED code, 6 or 9 characters long
   * @return StandardId of the Markit RED code with proper scheme
   */
  public static StandardId id(String name) {
    Preconditions.checkArgument(
        name.length() == 6 || name.length() == 9,
        "RED Code must be exactly 6 or 9 characters"
    );
    return StandardId.of(MARKIT_REDCODE_SCHEME, name);
  }

}
