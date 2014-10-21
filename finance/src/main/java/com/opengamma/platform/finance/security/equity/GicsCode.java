/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.security.equity;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * Representation of a GICS code.
 * <p>
 * A Global Industry Classification Standard code (GICS) is an 8 digit code
 * used to identify the sectors and industries that a company operates in.
 * <p>
 * The 8 digits are divided into 4 digit-pairs representing a description hierarchy:
 * <ul>
 * <li>Sector
 * <li>Industry-group
 * <li>Industry
 * <li>Sub-Industry
 * </ul>
 * For example, "Highways and Railtracks" is defined as follows:
 * <ul>
 * <li>Sector - Industrial - code 20
 * <li>Industry group - Transportation - code 2030
 * <li>Industry - Transportation infrastructure - code 203050
 * <li>Sub-Industry - Highways and Railtracks - code 20305020
 * </ul>
 * <p>
 * This class is immutable and thread-safe.
 */
public final class GicsCode
    implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /** Pattern for the code. */
  private static final Pattern FORMAT = Pattern.compile("([1-9][0-9]){1,4}");

  /**
   * The code.
   */
  private final String code;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code GicsCode} instance from the combined code.
   * <p>
   * The code specified must follow the GICS code standard, being a number
   * between 1 and 99999999 inclusive where no two digit part is 0.
   * The number is not validated against known values.
   * 
   * @param code  the value from 10 to 99999999 inclusive
   * @return the GICS instance, not null
   * @throws IllegalArgumentException if the value is invalid
   */
  @FromString
  public static GicsCode of(String code) {
    return new GicsCode(code);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance with a specific code.
   * 
   * @param code  the GICS code, from 10 to 99999999
   */
  private GicsCode(String code) {
    this.code = ArgChecker.matches(FORMAT, code, "code");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the full code.
   * <p>
   * The combined code will consist of the sector, group, industry and sub-industry parts.
   * The returned length will be 2, 4, 6, or 8 characters long.
   * For example, if the code represents only a sector then the value will be from 10 to 99.
   * 
   * @return the combined code, from 10 to 99999999 inclusive
   */
  @ToString
  public String getCode() {
    return code;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the sector code.
   * <p>
   * The sector code is the most important part of the classification.
   * It is the first two digits of the code.
   * 
   * @return the sector code, from 10 to 99
   */
  public String getSector() {
    return getCode().length() >= 2 ? getCode().substring(0, 2) : "";
  }

  /**
   * Gets the industry-group code.
   * <p>
   * The industry-group code is the second most important part of the classification.
   * It is the first four digits of the code.
   * 
   * @return the industry-group code, from 1010 to 9999, empty if no industry-group
   */
  public String getIndustryGroup() {
    return getCode().length() >= 4 ? getCode().substring(0, 4) : "";
  }

  /**
   * Gets the industry code.
   * <p>
   * The industry code is the third most important part of the classification.
   * It is the first six digits of the code.
   * 
   * @return the industry code, from 101010 to 999999, empty if no industry
   */
  public String getIndustry() {
    return getCode().length() >= 6 ? getCode().substring(0, 6) : "";
  }

  /**
   * Gets the sub-industry code.
   * <p>
   * The group code is the least important part of the classification.
   * It is the first eight digits of the code.
   * 
   * @return the sub-industry code, from 10101010 to 99999999
   */
  public String getSubIndustry() {
    return getCode().length() == 8 ? getCode() : "";
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the code is a complete 8 digit sub-industry code.
   * 
   * @return true if complete 8 digit code
   */
  public boolean isComplete() {
    return getCode().length() == 8;
  }

  /**
   * Checks if the code is a partial code of less than 8 digits.
   * 
   * @return true if less than the complete 8 digit code
   */
  public boolean isPartial() {
    return getCode().length() < 8;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this code to another based on the combined code.
   * 
   * @param obj  the other code, null returns false
   * @return true of equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof GicsCode) {
      GicsCode other = (GicsCode) obj;
      return getCode().equals(other.getCode());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getCode().hashCode();
  }

  /**
   * Returns a string description of the code, which includes the code and a description.
   * 
   * @return the string version of the code, not null
   */
  @Override
  public String toString() {
    return code;
  }

}
