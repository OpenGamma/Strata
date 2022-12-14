/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining how yield is computed for a bill.
 */
public enum BillYieldConvention implements NamedEnum {

  /**
   * Discount.
   */
  DISCOUNT("Discount") {
    @Override
    public double priceFromYield(double yield, double accrualFactor) {
      return 1.0d - accrualFactor * yield;
    }
    @Override
    public ValueDerivatives priceFromYieldAd(double yield, double accrualFactor) {
      return ValueDerivatives.of(1d - accrualFactor * yield, DoubleArray.of(-accrualFactor));
    }

    @Override
    public double yieldFromPrice(double price, double accrualFactor) {
      return (1.0d - price) / accrualFactor;
    }

    @Override
    public ValueDerivatives yieldFromPriceAd(double price, double accrualFactor) {
      return ValueDerivatives.of((1d - price) / accrualFactor, DoubleArray.of(-1d / accrualFactor));
    }
  },

  /**
   * France CD: interest at maturity.
   */
  FRANCE_CD("France-CD") {
    @Override
    public double priceFromYield(double yield, double accrualFactor) {
      return 1.0d / (1.0d + accrualFactor * yield);
    }
    @Override
    public ValueDerivatives priceFromYieldAd(double yield, double accrualFactor) {
      return ValueDerivatives.of(
          1.0d / (1.0d + accrualFactor * yield),
          DoubleArray.of(-accrualFactor / Math.pow((1d + accrualFactor * yield), 2)));
    }

    @Override
    public double yieldFromPrice(double price, double accrualFactor) {
      return (1.0d / price - 1) / accrualFactor;
    }

    @Override
    public ValueDerivatives yieldFromPriceAd(double price, double accrualFactor) {
      return ValueDerivatives.of(
          (1d / price - 1d) / accrualFactor,
          DoubleArray.of(-1d / (price * price * accrualFactor)));
    }
  },

  /**
   * Interest at maturity.
   */
  INTEREST_AT_MATURITY("Interest-At-Maturity") {
    @Override
    public double priceFromYield(double yield, double accrualFactor) {
      return 1.0d / (1.0d + accrualFactor * yield);
    }
    @Override
    public ValueDerivatives priceFromYieldAd(double yield, double accrualFactor) {
      return ValueDerivatives.of(
          1d / (1d + accrualFactor * yield),
          DoubleArray.of(-accrualFactor / Math.pow(1d + accrualFactor * yield, 2)));
    }

    @Override
    public double yieldFromPrice(double price, double accrualFactor) {
      return (1.0d / price - 1) / accrualFactor;
    }

    @Override
    public ValueDerivatives yieldFromPriceAd(double price, double accrualFactor) {
      return ValueDerivatives.of(
          (1d / price - 1d) / accrualFactor,
          DoubleArray.of(-1d / (accrualFactor * price * price)));
    }
  },

  /**
   * Japanese T-Bills.
   */
  JAPAN_BILLS("Japan-Bills") {
    @Override
    public double priceFromYield(double yield, double accrualFactor) {
      return 1.0d / (1.0d + accrualFactor * yield);
    }
    @Override
    public ValueDerivatives priceFromYieldAd(double yield, double accrualFactor) {
      return  ValueDerivatives.of(
          1d / (1d + accrualFactor * yield),
          DoubleArray.of(-accrualFactor / Math.pow(1d + accrualFactor * yield, 2)));
    }

    @Override
    public double yieldFromPrice(double price, double accrualFactor) {
      return (1.0d / price - 1) / accrualFactor;
    }

    @Override
    public ValueDerivatives yieldFromPriceAd(double price, double accrualFactor) {
      return  ValueDerivatives.of(
          (1d / price - 1d) / accrualFactor,
          DoubleArray.of(-1d / (price * price * accrualFactor)));
    }
  };

  // helper for name conversions
  private static final EnumNames<BillYieldConvention> NAMES =
      EnumNames.ofManualToString(BillYieldConvention.class);

  // name
  private final String name;

  // create
  private BillYieldConvention(String name) {
    this.name = name;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static BillYieldConvention of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return name;
  }

  /**
   * Computes the price from a yield and a accrual factor.
   * 
   * @param yield  the yield
   * @param accrualFactor  the accrual factor
   * @return the price
   */
  public abstract double priceFromYield(double yield, double accrualFactor);

  /**
   * Computes the yield from a price and a accrual factor.
   * 
   * @param price  the price
   * @param accrualFactor  the accrual factor
   * @return the yield
   */
  public abstract double yieldFromPrice(double price, double accrualFactor);

  /**
   * Computes the price from a yield and an accrual factor and its derivative wrt the yield.
   *
   * @param yield  the yield
   * @param accrualFactor  the accrual factor
   * @return the price and derivative
   */
  public abstract ValueDerivatives priceFromYieldAd(double yield, double accrualFactor);

  /**
   * Computes the yield from a price and an accrual factor and its derivative wrt the price.
   *
   * @param price the price
   * @param accrualFactor the accrual factor
   * @return the yield and derivative
   */
  public abstract ValueDerivatives yieldFromPriceAd(double price, double accrualFactor);

}
