/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.time.YearMonth;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityId;

/**
 * An instrument representing an exchange traded derivative (ETD).
 */
public interface EtdSecurity
    extends Security, SecuritizedProduct {

  @Override
  public default SecurityId getSecurityId() {
    return Security.super.getSecurityId();
  }

  @Override
  public default Currency getCurrency() {
    return Security.super.getCurrency();
  }

  @Override
  public default ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.of();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the ID of the contract specification from which this security is derived.
   * 
   * @return the ID
   */
  public abstract EtdContractSpecId getContractSpecId();

  /**
   * Gets the type of the contract - future or option.
   * 
   * @return the type, future or option
   */
  public abstract EtdType getType();

  /**
   * Gets the year-month of the expiry.
   * <p>
   * Expiry will occur on a date implied by the variant of the ETD.
   * 
   * @return the year-month
   */
  public abstract YearMonth getExpiry();

  /**
   * Gets the variant of ETD.
   * <p>
   * This captures the variant of the ETD. The most common variant is 'Monthly'.
   * Other variants are 'Weekly', 'Daily' and 'Flex'.
   * <p>
   * When building, this defaults to 'Monthly'.
   * 
   * @return the variant
   */
  public abstract EtdVariant getVariant();

}
