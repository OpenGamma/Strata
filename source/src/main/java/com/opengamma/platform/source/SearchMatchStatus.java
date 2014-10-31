/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * The match status of a search result.
 * <p>
 * Indicates whether the results being returned match all of
 * the specified search criteria or only some subset of them.
 * Partial matches will then be subject to filtering to
 * get the correct set of matches.
 */
public enum SearchMatchStatus {

  /**
   * Status indicating that the returned results satisfy
   * all the specified search criteria. These results will
   * not be subject to further filtering.
   */
  FULL,
  /**
   * Status indicating that the returned results do not
   * satisfy all the specified search criteria. These results
   * will be subject to further filtering.
   */
  PARTIAL;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   *
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SearchMatchStatus of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Returns the formatted unique name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
