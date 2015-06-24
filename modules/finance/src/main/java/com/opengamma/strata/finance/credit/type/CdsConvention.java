/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsDatesLogic;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.FeeLeg;
import com.opengamma.strata.finance.credit.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.PeriodicPayments;
import com.opengamma.strata.finance.credit.ReferenceInformation;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.SingleNameReferenceInformation;
import com.opengamma.strata.finance.credit.SinglePayment;

/**
 * A market convention for how CDS trades are structured in different regions and currencies.
 */
public interface CdsConvention
    extends Named {

  /**
   * Looks up the convention corresponding to a given name.
   * 
   * @param uniqueName  the unique name of the convention
   * @return the resolved convention
   */
  @FromString
  static CdsConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum lookup from name to instance.
   * 
   * @return the extended enum lookup
   */
  static ExtendedEnum<CdsConvention> extendedEnum() {
    return CdsConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * 
   * @return the currency
   */
  Currency getCurrency();

  /**
   * Gets the day count convention.
   * 
   * @return the day count convention
   */
  DayCount getDayCount();

  /**
   * Gets the business day adjustment.
   * 
   * @return the business day adjustment
   */
  BusinessDayAdjustment getBusinessDayAdjustment();

  /**
   * Gets the payment frequency.
   * 
   * @return the payment frequency
   */
  Frequency getPaymentFrequency();

  /**
   * Gets the roll convention.
   * 
   * @return the roll convention
   */
  RollConvention getRollConvention();

  /**
   * Gets whether the accrued premium is paid in the event of a default.
   * 
   * @return whether the accrued premium is paid in the event of a default
   */
  boolean getPayAccruedOnDefault();

  /**
   * Gets the stub convention.
   * 
   * @return the stub convention
   */
  StubConvention getStubConvention();

  /**
   * Gets the number of step-in days.
   * <p>
   * This is the date from which the issuer is deemed to be risky.
   * 
   * @return the number of step-in days
   */
  int getStepIn();

  /**
   * Gets the settlement lag in days.
   * <p>
   * This is the number of days after the start date that any upfront fees are paid.
   * 
   * @return the settlement lag in days
   */
  int getSettleLag();

  //-------------------------------------------------------------------------
  @ToString
  @Override
  String getName();

  //-------------------------------------------------------------------------
  /**
   * Creates a single-name CDS trade from the convention.
   * 
   * @param startDate  the date that the CDS starts
   * @param endDate  the date that the CDS ends
   * @param buySell  whether protection is being bought or sold
   * @param notional  the notional amount
   * @param coupon  the coupon amount
   * @param referenceEntityId  the identifier of the reference entity
   * @param seniorityLevel  the seniority level
   * @param restructuringClause  the restructuring clause
   * @param upfrontFeeAmount  the amount of the upfront fee
   * @param upfrontFeePaymentDate  the payment date of the upfront fee
   * @return the single-name CDS
   */
  default CdsTrade toSingleNameTrade(
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      StandardId referenceEntityId,
      SeniorityLevel seniorityLevel,
      RestructuringClause restructuringClause,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate) {

    return toTrade(
        startDate,
        endDate,
        buySell,
        notional,
        coupon,
        SingleNameReferenceInformation.of(
            referenceEntityId,
            seniorityLevel,
            getCurrency(),
            restructuringClause),
        upfrontFeeAmount,
        upfrontFeePaymentDate);
  }

  /**
   * Creates an index CDS from the convention.
   * 
   * @param startDate  the date that the CDS starts
   * @param endDate  the date that the CDS ends
   * @param buySell  whether protection is being bought or sold
   * @param notional  the notional amount
   * @param coupon  the coupon amount
   * @param indexId  the identifier of the index
   * @param indexSeries  the index series
   * @param indexAnnexVersion  the index annex version
   * @param upfrontFeeAmount  the amount of the upfront fee
   * @param upfrontFeePaymentDate  the payment date of the upfront fee
   * @return the index CDS trade
   */
  default CdsTrade toIndexTrade(
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      StandardId indexId,
      int indexSeries,
      int indexAnnexVersion,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate) {

    return toTrade(
        startDate,
        endDate,
        buySell,
        notional,
        coupon,
        IndexReferenceInformation.of(indexId, indexSeries, indexAnnexVersion),
        upfrontFeeAmount,
        upfrontFeePaymentDate);
  }

  /**
   * Creates a CDS from the convention.
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
  default CdsTrade toTrade(
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      ReferenceInformation referenceInformation,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate) {

    return CdsTrade.builder()
        .product(Cds.builder()
            .startDate(startDate)
            .endDate(endDate)
            .buySellProtection(buySell)
            .businessDayAdjustment(getBusinessDayAdjustment())
            .referenceInformation(referenceInformation)
            .feeLeg(
                FeeLeg.of(
                    SinglePayment.of(
                        getCurrency(),
                        upfrontFeeAmount,
                        upfrontFeePaymentDate),
                    PeriodicPayments.of(
                        CurrencyAmount.of(getCurrency(), notional),
                        coupon,
                        getDayCount(),
                        getPaymentFrequency(),
                        getStubConvention(),
                        getRollConvention())))
            .payAccruedOnDefault(true)
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  // TODO: move these static methods elsewhere
  /**
   * Used in curve point calculation.
   *
   * @param valuationDate  the date of the curve calibration
   * @param period  the term for this point
   * @return unadjusted maturity date
   */
  default LocalDate getUnadjustedMaturityDateFromValuationDate(LocalDate valuationDate, Period period) {
    return getUnadjustedMaturityDate(valuationDate, getPaymentFrequency(), period);
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
  static LocalDate getUnadjustedMaturityDate(
      LocalDate valuationDate,
      Frequency paymentFrequency,
      Period period) {

    return getUnadjustedAccrualStartDate(valuationDate)
        .plus(period)
        .plus(paymentFrequency.getPeriod());
  }

  /**
   * Gets the previous CDS date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted accrual start date
   */
  static LocalDate getUnadjustedAccrualStartDate(LocalDate valuationDate) {
    return CdsDatesLogic.getPrevCdsDate(valuationDate);
  }

  /**
   * Gets the adjusted start date.
   * 
   * @param valuationDate  the valuation date
   * @return adjusted start date
   */
  default LocalDate getAdjustedStartDate(LocalDate valuationDate) {
    return getBusinessDayAdjustment().adjust(
        getUnadjustedAccrualStartDate(valuationDate));
  }

  /**
   * Gets the adjusted settlement date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted settle date
   */
  default LocalDate getAdjustedSettleDate(LocalDate valuationDate) {
    DaysAdjustment daysAdjustment = DaysAdjustment.ofBusinessDays(
        getSettleLag(), getBusinessDayAdjustment().getCalendar(), getBusinessDayAdjustment());
    return daysAdjustment.adjust(valuationDate);
  }

  /**
   * Gets the unadjusted step-in date.
   * 
   * @param valuationDate  the valuation date
   * @return unadjusted step in date
   */
  default LocalDate getUnadjustedStepInDate(LocalDate valuationDate) {
    return valuationDate.plusDays(getStepIn());
  }

}
