/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The tier (seniority) of a CDS.
 */
public class CdsTier extends TypedString<CdsTier> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The CDS tier for junior subordinated or upper tier 2 debt (Banks).
   */
  public static final CdsTier JUNIOR_SUBORDINATED = new CdsTier("JRSUBUT2");

  /**
   * The CDS tier for preference shares, or tier 1 capital debt (Banks).
   */
  public static final CdsTier PREFERNCE_SHARES = new CdsTier("PREFT1");

  /**
   * The CDS tier for secured debt (corporate/financial) or domestic currency sovereign debt (government).
   */
  public static final CdsTier SECURED_DEBT = new CdsTier("SECDOM");

  /**
   * The CDS tier for senior loss absorbing capacity debt.
   */
  public static final CdsTier SENIOR_LOSS_ABSORBING_CAPACITY = new CdsTier("SNRLAC");

  /**
   * The CDS tier for senior unsecured debt (corporate/financial) or foreign currency sovereign debt (government).
   */
  public static final CdsTier SENIOR_UNSECURED = new CdsTier("SNRFOR");

  /**
   * The CDS tier for subordinated or lower tier 2 debt (Banks).
   */
  public static final CdsTier SUBORDINATED = new CdsTier("SUBLT2");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param name  the name
   * @return a type instance with the specified name
   */
  @FromString
  public static CdsTier of(String name) {
    return new CdsTier(name);
  }

  /**
   * Creates an instance.
   *
   * @param name  the name
   */
  private CdsTier(String name) {
    super(name);
  }

  // resolve after deserialization
  private Object readResolve() {
    return of(getName());
  }

}
