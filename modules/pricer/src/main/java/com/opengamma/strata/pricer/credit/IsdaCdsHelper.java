/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.*;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.finance.credit.type.CdsConvention;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveUnderlyingType;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.pricer.PricingException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Stream;

/**
 * Helper for interacting with the underlying Analytics layer for CDS pricing.
 * <p>
 * Translation from Strata business objects such as DayCount and StubMethod is done here.
 * The translation of underlying types for the yield curve is performed here.
 * Par rate representations of the curves are calibrated and converted to ISDA calibrated curves.
 * Present value of the expanded CDS product (single name or index) is calculated here.
 */
public class IsdaCdsHelper {

  /**
   * DayCount used with calculating time during curve calibration.
   * <p>
   * The model expects ACT_365F, but this value is not on the trade or the convention.
   */
  private final static DayCount CURVE_DAY_COUNT = DayCounts.ACT_365F;
  /**
   * When protection starts, at the start or end of the day.
   * <p>
   * If true, then protections starts at the beginning of the day, otherwise it is at the end.
   * The model expects this, but it is not a property of the trade or convention.
   * If true the protection is from the start of day and the effective accrual start and end dates are one day less.
   * The exception is the final accrual end date which should have one day added
   * (if  protectionFromStartOfDay = true) in the final CDSCouponDes to compensate for this, so the
   * accrual end date is just the CDS maturity. The effect of having protectionFromStartOfDay = true
   * is to add an extra day of protection.
   */
  private final static boolean PROTECT_START = true;
  /**
   * ISDA Standard model implementation in analytics.
   */
  private final static AnalyticCDSPricer CALCULATOR = new AnalyticCDSPricer();

  //-------------------------------------------------------------------------

  /**
   * Calculate present value on the specified valuation date.
   *
   * @param valuationDate date that present value is calculated on, also date that curves will be calibrated to
   * @param product       expanded CDS product
   * @param yieldCurve    par rates representation of the ISDA yield curve
   * @param creditCurve   par rates representation of the ISDA credit curve
   * @param recoveryRate  recovery rate for the reference entity/issue
   * @return the present value of the expanded CDS product
   */
  public static CurrencyAmount price(
      LocalDate valuationDate,
      ExpandedCds product,
      NodalCurve yieldCurve,
      NodalCurve creditCurve,
      double recoveryRate,
      double scalingFactor) {

    // setup
    CDSAnalytic cdsAnalytic = toAnalytic(valuationDate, product, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = ISDACompliantYieldCurve.makeFromRT(yieldCurve.getXValues(), yieldCurve.getYValues());
    ISDACompliantCreditCurve creditCurveAnalytics = ISDACompliantCreditCurve.makeFromRT(creditCurve.getXValues(), creditCurve.getYValues());

    // calculate
    double coupon = product.getCoupon();
    double pv = CALCULATOR.pv(cdsAnalytic, yieldCurveAnalytics, creditCurveAnalytics, coupon, PriceType.DIRTY, 0d);

    // create result
    int sign = product.getBuySellProtection().isBuy() ? 1 : -1;
    double notional = product.getNotional();
    double factor = scalingFactor;
    double adjusted = pv * notional * sign * factor;
    double upfrontFeeAmount = priceUpfrontFee(
        valuationDate, product.getUpfrontFeeAmount(), product.getUpfrontFeePaymentDate(), yieldCurveAnalytics) * sign;
    double adjustedPlusFee = adjusted + upfrontFeeAmount;
    return CurrencyAmount.of(product.getCurrency(), adjustedPlusFee);
  }

  //-------------------------------------------------------------------------
  // The fee is always calculated as being payable by the protection buyer.
  // If the seller should pay the fee, then a negative amount is used.
  private static double priceUpfrontFee(
      LocalDate valuationDate,
      OptionalDouble amount,
      Optional<LocalDate> paymentDate,
      ISDACompliantYieldCurve yieldCurve) {

    if (!amount.isPresent()) {
      return 0d; // no fee
    }
    if (!paymentDate.get().isAfter(valuationDate)) {
      return 0d; // fee already paid
    }
    double feeSettleYearFraction = CURVE_DAY_COUNT.yearFraction(valuationDate, paymentDate.get());
    double discountFactor = yieldCurve.getDiscountFactor(feeSettleYearFraction);
    return discountFactor * amount.getAsDouble();
  }

  /**
   * Calculate par spread on the specified valuation date.
   *
   * @param valuationDate date that par spread is calculated on, also date that curves will be calibrated to
   * @param product       expanded CDS product
   * @param yieldCurve    par rates representation of the ISDA yield curve
   * @param creditCurve   par rates representation of the ISDA credit curve
   * @param recoveryRate  recovery rate for the reference entity/issue
   * @return the par spread of the expanded CDS product
   */
  public static double parSpread(LocalDate valuationDate,
                                 ExpandedCds product,
                                 NodalCurve yieldCurve,
                                 NodalCurve creditCurve,
                                 double recoveryRate) {
    // setup
    CDSAnalytic cdsAnalytic = toAnalytic(valuationDate, product, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = ISDACompliantYieldCurve.makeFromRT(
        yieldCurve.getXValues(), yieldCurve.getYValues());
    ISDACompliantCreditCurve creditCurveAnalytics = ISDACompliantCreditCurve.makeFromRT(
        creditCurve.getXValues(), creditCurve.getYValues());

    return CALCULATOR.parSpread(cdsAnalytic, yieldCurveAnalytics, creditCurveAnalytics);

  }

  // Converts the interest rate curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static ISDACompliantYieldCurve createIsdaDiscountCurve(
      LocalDate valuationDate,
      IsdaYieldCurveParRates yieldCurve) {

    try {
      // model does not use floating leg of underlying IRS
      IsdaYieldCurveConvention curveConvention = yieldCurve.getCurveConvention();
      Period swapInterval = curveConvention.getFixedPaymentFrequency().getPeriod();
      DayCount mmDayCount = curveConvention.getMmDayCount();
      DayCount swapDayCount = curveConvention.getFixedDayCount();

      BusinessDayConvention convention = curveConvention.getBadDayConvention();
      HolidayCalendar holidayCalendar = curveConvention.getHolidayCalendar();

      LocalDate spotDate = curveConvention.getSpotDateAsOf(valuationDate);

      ISDAInstrumentTypes[] types =
          Stream.of(yieldCurve.getYieldCurveInstruments())
              .map(IsdaCdsHelper::mapInstrumentType)
              .toArray(ISDAInstrumentTypes[]::new);

      ISDACompliantYieldCurveBuild builder = new ISDACompliantYieldCurveBuild(
          valuationDate,
          spotDate,
          types,
          yieldCurve.getYieldCurvePoints(),
          translateDayCount(mmDayCount),
          translateDayCount(swapDayCount),
          swapInterval,
          translateDayCount(CURVE_DAY_COUNT),
          convention,
          holidayCalendar);
      return builder.build(yieldCurve.getParRates());

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Discount Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the credit curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static ISDACompliantCreditCurve createIsdaCreditCurve(
      LocalDate valuationDate,
      IsdaCreditCurveParRates curveCurve,
      ISDACompliantYieldCurve yieldCurve,
      double recoveryRate) {

    try {
      CdsConvention cdsConvention = curveCurve.getCdsConvention();
      FastCreditCurveBuilder builder = new FastCreditCurveBuilder(
          AccrualOnDefaultFormulae.OrignalISDA, ISDACompliantCreditCurveBuilder.ArbitrageHandling.Fail);
      return builder.calibrateCreditCurve(
          valuationDate,
          cdsConvention.getUnadjustedStepInDate(valuationDate),
          cdsConvention.getAdjustedSettleDate(valuationDate),
          cdsConvention.getAdjustedStartDate(valuationDate),
          curveCurve.getEndDatePoints(),
          curveCurve.getParRates(),
          cdsConvention.getPayAccruedOnDefault(),
          cdsConvention.getPaymentFrequency().getPeriod(),
          translateStubType(cdsConvention.getStubConvention()),
          PROTECT_START,
          yieldCurve,
          recoveryRate);

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Credit Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the credit curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static ISDACompliantCreditCurve createIsdaCreditCurve(
      LocalDate valuationDate,
      IsdaCreditCurveParRates curveCurve,
      NodalCurve yieldCurve,
      double recoveryRate) {

    try {
      ISDACompliantYieldCurve yieldCurveAnalytics = ISDACompliantYieldCurve.makeFromRT(yieldCurve.getXValues(), yieldCurve.getYValues());
      CdsConvention cdsConvention = curveCurve.getCdsConvention();
      FastCreditCurveBuilder builder = new FastCreditCurveBuilder(
          AccrualOnDefaultFormulae.OrignalISDA, ISDACompliantCreditCurveBuilder.ArbitrageHandling.Fail);
      return builder.calibrateCreditCurve(
          valuationDate,
          cdsConvention.getUnadjustedStepInDate(valuationDate),
          cdsConvention.getAdjustedSettleDate(valuationDate),
          cdsConvention.getAdjustedStartDate(valuationDate),
          curveCurve.getEndDatePoints(),
          curveCurve.getParRates(),
          cdsConvention.getPayAccruedOnDefault(),
          cdsConvention.getPaymentFrequency().getPeriod(),
          translateStubType(cdsConvention.getStubConvention()),
          PROTECT_START,
          yieldCurveAnalytics,
          recoveryRate);

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Credit Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the expanded CDS product to the corresponding analytics form.
  private static CDSAnalytic toAnalytic(LocalDate valuationDate, ExpandedCds product, double recoveryRate) {
    try {
      return new CDSAnalytic(
          valuationDate,
          valuationDate.plusDays(1),
          valuationDate,
          product.getStartDate(),
          product.getEndDate(),
          product.isPayAccruedOnDefault(),
          product.getPaymentInterval(),
          translateStubType(product.getStubConvention()),
          PROTECT_START,
          recoveryRate,
          product.getBusinessDayAdjustment().getConvention(),
          product.getBusinessDayAdjustment().getCalendar(),
          translateDayCount(product.getAccrualDayCount()),
          translateDayCount(CURVE_DAY_COUNT));

    } catch (Exception ex) {
      throw new PricingException("Error converting the trade to an analytic: " + ex.getMessage(), ex);
    }
  }

  //-------------------------------------------------------------------------
  // Converts type of interest curve underlying to the corresponding analytics value.
  private static ISDAInstrumentTypes mapInstrumentType(IsdaYieldCurveUnderlyingType input) {
    switch (input) {
      case ISDA_MONEY_MARKET:
        return ISDAInstrumentTypes.MoneyMarket;
      case ISDA_SWAP:
        return ISDAInstrumentTypes.Swap;
      default:
        throw new IllegalStateException("Unexpected underlying type: " + input);
    }
  }

  // Converts day count to corresponding analytics value.
  private static com.opengamma.analytics.convention.daycount.DayCount translateDayCount(DayCount from) {
    switch (from.getName()) {
      case "Act/365F":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_365F;
      case "30E/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.THIRTY_E_360;
      case "30/360 ISDA":
        return com.opengamma.analytics.convention.daycount.DayCounts.THIRTY_E_360;
      case "Act/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_360;
      default:
        throw new IllegalStateException("Unknown daycount " + from);
    }
  }

  // Converts stub type to corresponding analytics value.
  private static com.opengamma.analytics.financial.credit.isdastandardmodel.StubType translateStubType(StubConvention from) {
    switch (from) {
      case SHORT_INITIAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.FRONTSHORT;
      case LONG_INITIAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.FRONTLONG;
      case SHORT_FINAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.BACKSHORT;
      case LONG_FINAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.BACKLONG;
      default:
        throw new IllegalStateException("Unknown stub convention: " + from);
    }
  }

}
