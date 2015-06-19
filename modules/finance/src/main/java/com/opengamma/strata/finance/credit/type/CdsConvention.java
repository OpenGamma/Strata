/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsDatesLogic;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
import com.opengamma.strata.finance.credit.fee.PeriodicPayments;
import com.opengamma.strata.finance.credit.fee.SinglePayment;
import com.opengamma.strata.finance.credit.reference.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.reference.ReferenceInformation;
import com.opengamma.strata.finance.credit.reference.SingleNameReferenceInformation;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import java.time.LocalDate;
import java.time.Period;

public interface CdsConvention extends Named {

  @FromString
  static CdsConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  static ExtendedEnum<CdsConvention> extendedEnum() {
    return CdsConventions.ENUM_LOOKUP;
  }

  Currency getCurrency();

  DayCount getDayCount();

  BusinessDayConvention getDayConvention();

  Frequency getPaymentFrequency();

  RollConvention getRollConvention();

  boolean getPayAccOnDefault();

  HolidayCalendar getCalendar();

  StubConvention getStubConvention();

  int getStepIn();

  int getSettleLag();

  //-------------------------------------------------------------------------

  default CdsTrade toSingleNameTrade(
      StandardId id,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      StandardId referenceEntityId,
      SeniorityLevel seniorityLevel,
      RestructuringClause restructuringClause,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate
  ) {
    return toTrade(
        id,
        startDate,
        endDate,
        buySell,
        notional,
        coupon,
        SingleNameReferenceInformation.of(
            referenceEntityId,
            seniorityLevel,
            getCurrency(),
            restructuringClause
        ),
        upfrontFeeAmount,
        upfrontFeePaymentDate
    );
  }

  default CdsTrade toIndexTrade(
      StandardId id,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      StandardId indexId,
      int indexSeries,
      int indexAnnexVersion,
      RestructuringClause restructuringClause,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate
  ) {
    return toTrade(
        id,
        startDate,
        endDate,
        buySell,
        notional,
        coupon,
        IndexReferenceInformation.of(
            indexId,
            indexSeries,
            indexAnnexVersion,
            restructuringClause
        ),
        upfrontFeeAmount,
        upfrontFeePaymentDate
    );
  }

  default CdsTrade toTrade(
      StandardId id,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double coupon,
      ReferenceInformation referenceInformation,
      double upfrontFeeAmount,
      LocalDate upfrontFeePaymentDate
  ) {
    return CdsTrade.of(
        TradeInfo
            .builder()
            .id(id)
            .build(),
        Cds.builder()
            .startDate(startDate)
            .endDate(endDate)
            .buySellProtection(buySell)
            .businessDayConvention(getDayConvention())
            .holidayCalendar(getCalendar())
            .referenceInformation(referenceInformation)
            .feeLeg(
                FeeLeg.of(
                    SinglePayment.of(
                        getCurrency(),
                        upfrontFeeAmount,
                        upfrontFeePaymentDate
                    ),
                    PeriodicPayments.of(
                        CurrencyAmount.of(getCurrency(), notional),
                        coupon,
                        getDayCount(),
                        getPaymentFrequency(),
                        getStubConvention(),
                        getRollConvention()
                    )
                )
            )
            .payAccOnDefault(true)
            .build()
    );
  }

  default BusinessDayAdjustment calcBusinessAdjustment() {
    return BusinessDayAdjustment.of(
        getDayConvention(),
        getCalendar()
    );
  }

  /**
   * Used in curve point calculation
   *
   * @param valuationDate asOfDate for curve calibration
   * @param period        term for this point
   * @return unadjusted maturity date
   */
  default LocalDate calcUnadjustedMaturityDateFromValuationDateOf(LocalDate valuationDate, Period period) {
    return calcUnadjustedMaturityDate(valuationDate, getPaymentFrequency(), period);
  }

  /**
   * Find previous IMM date
   */
  static LocalDate calcUnadjustedAccrualStartDate(LocalDate valuationDate) {
    return CdsDatesLogic.getPrevCdsDate(valuationDate);
  }

  /**
   * Standard maturity dates are unadjusted – always Mar/Jun/Sep/Dec 20th.
   * Example: As of Feb09, the 1y standard CDS contract would protect the buyer through Sat 20Mar10.
   */
  static LocalDate calcUnadjustedMaturityDate(
      LocalDate valuationDate,
      Frequency paymentFrequency,
      Period period
  ) {
    return calcUnadjustedAccrualStartDate(valuationDate)
        .plus(period)
        .plus(paymentFrequency.getPeriod());
  }

  /**
   * public so we can use in curve building
   */
  default LocalDate calcAdjustedStartDate(LocalDate valuationDate) {
    return calcBusinessAdjustment().adjust(
        calcUnadjustedAccrualStartDate(valuationDate)
    );
  }

  /**
   * public so we can use in curve building
   */
  default LocalDate calcAdjustedSettleDate(LocalDate valuationDate) {
    BusinessDayAdjustment businessAdjustment = calcBusinessAdjustment();
    DaysAdjustment daysAdjustment = DaysAdjustment.ofBusinessDays(
        getSettleLag(), businessAdjustment.getCalendar(), businessAdjustment);

    return daysAdjustment.adjust(valuationDate);
  }

  /**
   * public so we can use in curve building
   */
  default LocalDate calcUnadjustedStepInDate(LocalDate valuationDate) {
    return valuationDate.plusDays(getStepIn());
  }


  @ToString
  @Override
  String getName();
}
