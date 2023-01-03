/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;

/**
 * Utilities related to bond futures.
 */
public class BondFuturesUtils {

  /** The bond pricer */
  public static final DiscountingFixedCouponBondProductPricer PRICER_BOND =
      DiscountingFixedCouponBondProductPricer.DEFAULT;
  /** The rounding conventions for conversion factors: EUREX Germany. */
  public static final Rounding ROUNDING_EUREX_DE = Rounding.ofDecimalPlaces(6);
  /** The rounding conventions for conversion factors: ICE United Kingdom. */
  public static final Rounding ROUNDING_ICE_UK = Rounding.ofDecimalPlaces(7);
  /** The rounding conventions for conversion factors: CME United States. */
  public static final Rounding ROUNDING_CME_US = Rounding.ofDecimalPlaces(4);

  // Private constructor
  private BondFuturesUtils() {
  }

  /**
   * Returns the EUREX bond futures conversion factor for a given German bond.
   * 
   * @param bond  the bond
   * @param deliveryDate  the delivery date
   * @param notionalCoupon  the notional coupon for the futures; typically 6%
   * @return the factor
   */
  public static double conversionFactorEurexDE(
      ResolvedFixedCouponBond bond,
      LocalDate deliveryDate,
      double notionalCoupon) {

    double dirtyPrice = PRICER_BOND.dirtyPriceFromYield(bond, deliveryDate, notionalCoupon);
    double cleanPrice = PRICER_BOND.cleanPriceFromDirtyPrice(bond, deliveryDate, dirtyPrice);
    double factorRaw = cleanPrice;
    return ROUNDING_EUREX_DE.round(factorRaw);
  }

  /**
   * Returns the ICE bond futures conversion factor for a given U.K. bond.
   * 
   * @param bond  the bond
   * @param deliveryDate  the delivery date
   * @param notionalCoupon  the notional coupon for the futures; typically 4%
   * @return the factor
   */
  public static double priceFactorIceUK(
      ResolvedFixedCouponBond bond,
      LocalDate deliveryDate,
      double notionalCoupon) {

    double dirtyPrice = PRICER_BOND.dirtyPriceFromYield(bond, deliveryDate, notionalCoupon);
    double cleanPrice = PRICER_BOND.cleanPriceFromDirtyPrice(bond, deliveryDate, dirtyPrice);
    double factorRaw = cleanPrice;
    return ROUNDING_ICE_UK.round(factorRaw);
  }

  /**
   * Returns the CME bond futures conversion factor for a given U.S. short bond (i.e. underlying of TU, 3YR, FV).
   * <p>
   * The factor depends on the number of whole months between n-year after delivery and the maturity, 
   * with n the number of whole years from the first day of the delivery month to the maturity (or call) date of 
   * the bond or note.
   * 
   * @param bond  the bond
   * @param deliveryDate  the first day of the delivery month
   * @param notionalCoupon  the notional coupon for the futures; typically 6%
   * @return the factor
   */
  public static double conversionFactorCmeUsShort(
      ResolvedFixedCouponBond bond,
      LocalDate deliveryDate,
      double notionalCoupon) {

    double factorOnPeriod = 1.0d / (1.0d + 0.5 * notionalCoupon);
    double coupon = bond.getFixedRate();
    LocalDate maturity = bond.getUnadjustedEndDate();
    long n = ChronoUnit.YEARS.between(deliveryDate, maturity);
    LocalDate referenceDate = deliveryDate.plusYears(n);
    long z = ChronoUnit.MONTHS.between(referenceDate, maturity);
    long v = (z < 7) ? z : z - 6;
    double a = Math.pow(factorOnPeriod, v / 6.0d);
    double b = coupon / 2.0d * (6.0d - v) / 6.0d;
    double c = (z < 7) ? Math.pow(factorOnPeriod, 2 * n) : Math.pow(factorOnPeriod, 2 * n + 1);
    double d = coupon / notionalCoupon * (1.0d - c);
    double factorRaw = a * (0.5 * coupon + c + d) - b;
    return ROUNDING_CME_US.round(factorRaw);
  }

  /**
   * Returns the ICE bond futures conversion factor for a given U.S. long bond (i.e. underlying of US, TY).
   * <p>
   * The factor depends on the number of whole months between n-year after delivery and the maturity rounded down 
   * to the nearest quarter, with n the number of whole years from the first day of the delivery month to the 
   * maturity (or call) date of the bond or note.
   * 
   * @param bond  the bond
   * @param deliveryDate  the delivery date
   * @param notionalCoupon  the notional coupon for the futures; typically 6%
   * @return the factor
   */
  public static double conversionFactorCmeUsLong(
      ResolvedFixedCouponBond bond,
      LocalDate deliveryDate,
      double notionalCoupon) {

    double factorOnPeriod = 1.0d / (1.0d + 0.5 * notionalCoupon);
    double coupon = bond.getFixedRate();
    LocalDate maturity = bond.getUnadjustedEndDate();
    long n = ChronoUnit.YEARS.between(deliveryDate, maturity);
    LocalDate referenceDate = deliveryDate.plusYears(n);
    long z = (long) (Math.floor(ChronoUnit.MONTHS.between(referenceDate, maturity) / 3.0d) * 3L);
    long v = (z < 7) ? z : 3;
    double a = Math.pow(factorOnPeriod, v / 6.0d);
    double b = coupon / 2.0d * (6.0d - v) / 6.0d;
    double c = (z < 7) ? Math.pow(factorOnPeriod, 2 * n) : Math.pow(factorOnPeriod, 2 * n + 1);
    double d = coupon / notionalCoupon * (1.0d - c);
    double factorRaw = a * (0.5 * coupon + c + d) - b;
    return ROUNDING_CME_US.round(factorRaw);
  }

}
