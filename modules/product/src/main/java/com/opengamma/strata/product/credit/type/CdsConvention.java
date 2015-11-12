/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.credit.CdsDatesLogic;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

/**
 * A market convention for credit default swap (CDS) trades.
 * <p>
 * This defines the market convention for CDS trades in different regions and currencies.
 * <p>
 * To manually create a convention, see {@link ImmutableCdsConvention}.
 * To register a specific convention, see {@code CdsConvention.ini}.
 */
public interface CdsConvention
    extends TradeConvention, Named {

  /**
   * Obtains a convention from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CdsConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<CdsConvention> extendedEnum() {
    return CdsConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the day count convention.
   * 
   * @return the day count convention
   */
  public abstract DayCount getDayCount();

  /**
   * Gets the business day adjustment.
   * 
   * @return the business day adjustment
   */
  public abstract BusinessDayAdjustment getBusinessDayAdjustment();

  /**
   * Gets the payment frequency.
   * 
   * @return the payment frequency
   */
  public abstract Frequency getPaymentFrequency();

  /**
   * Gets the roll convention.
   * 
   * @return the roll convention
   */
  public abstract RollConvention getRollConvention();

  /**
   * Gets whether the accrued premium is paid in the event of a default.
   * 
   * @return whether the accrued premium is paid in the event of a default
   */
  public abstract boolean isPayAccruedOnDefault();

  /**
   * Gets the stub convention.
   * 
   * @return the stub convention
   */
  public abstract StubConvention getStubConvention();

  /**
   * Gets the number of step-in days.
   * <p>
   * This is the date from which the issuer is deemed to be on risk.
   * 
   * @return the number of step-in days
   */
  public abstract int getStepInDays();

  /**
   * Gets the settlement lag in days.
   * <p>
   * This is the number of days after the start date that any upfront fees are paid.
   * 
   * @return the settlement lag in days
   */
  public abstract int getSettleLagDays();

  //-----------------------------------------------------------------------
  /**
   * Creates a CDS from the convention.
   * <p>
   * A single name CDS can be specified using {@link SingleNameReferenceInformation}.
   * An index CDS can be specified using {@link IndexReferenceInformation}.
   * 
   * @param startDate  the date that the CDS starts
   * @param endDate  the date that the CDS ends
   * @param buySell  whether protection is being bought or sold
   * @param notional  the notional amount
   * @param coupon  the coupon amount
   * @param referenceInformation  the reference information of the CDS
   * @param upfrontFeeAmount  the amount of the upfront fee
   * @param upfrontFeePaymentDate  the payment date of the upfront fee
   * @return the CDS trade
   */
  public abstract CdsTrade toTrade(
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      ReferenceInformation referenceInformation,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate);

  //-------------------------------------------------------------------------
  /**
   * Used in curve point calculation.
   *
   * @param valuationDate  the date of the curve calibration
   * @param period  the term for this point
   * @return unadjusted maturity date
   */
  public default LocalDate calculateUnadjustedMaturityDateFromValuationDate(LocalDate valuationDate, Period period) {
    return calculateUnadjustedMaturityDate(valuationDate, getPaymentFrequency(), period);
  }

  /**
   * Gets the unadjusted maturity date.
   * <p>
   * Standard maturity dates are unadjusted, always Mar/Jun/Sep/Dec 20th.
   * For example, from February 2009 the 1y standard CDS contract would protect the buyer until 20 March 2010.
   * 
   * @param valuationDate  the valuation date
   * @param paymentFrequency  the payment frequency
   * @param period  the term for this point
   * @return unadjusted accrual maturity date
   */
  public static LocalDate calculateUnadjustedMaturityDate(
      LocalDate valuationDate,
      Frequency paymentFrequency,
      Period period) {

    return calculateUnadjustedAccrualStartDate(valuationDate)
        .plus(period)
        .plus(paymentFrequency.getPeriod());
  }

  /**
   * Gets the previous CDS date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted accrual start date
   */
  public static LocalDate calculateUnadjustedAccrualStartDate(LocalDate valuationDate) {
    return CdsDatesLogic.getPrevCdsDate(valuationDate);
  }

  /**
   * Gets the adjusted start date.
   * 
   * @param valuationDate  the valuation date
   * @return adjusted start date
   */
  public default LocalDate calculateAdjustedStartDate(LocalDate valuationDate) {
    return getBusinessDayAdjustment().adjust(
        calculateUnadjustedAccrualStartDate(valuationDate));
  }

  /**
   * Gets the adjusted settlement date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted settle date
   */
  public default LocalDate calculateAdjustedSettleDate(LocalDate valuationDate) {
    DaysAdjustment daysAdjustment = DaysAdjustment.ofBusinessDays(
        getSettleLagDays(), getBusinessDayAdjustment().getCalendar(), getBusinessDayAdjustment());
    return daysAdjustment.adjust(valuationDate);
  }

  /**
   * Gets the unadjusted step-in date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted step in date
   */
  public default LocalDate calculateUnadjustedStepInDate(LocalDate valuationDate) {
    return valuationDate.plusDays(getStepInDays());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
