/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.impl.credit.isda.AccrualOnDefaultFormulae;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticCdsPricer;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsPriceType;
import com.opengamma.strata.pricer.impl.credit.isda.FastCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurveBuild;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaInstrumentTypes;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.type.CdsConvention;
import com.opengamma.strata.product.credit.type.IsdaYieldCurveConvention;

/**
 * Helper for interacting with the underlying Analytics layer for CDS pricing.
 * <p>
 * Translation from Strata business objects such as DayCount and StubMethod is done here.
 * The translation of underlying types for the yield curve is performed here.
 * Par rate representations of the curves are calibrated and converted to ISDA calibrated curves.
 * Present value of the expanded CDS product (single name or index) is calculated here.
 */
class IsdaCdsHelper {

  // hard-coded reference data
  public static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * DayCount used with calculating time during curve calibration.
   * <p>
   * The model expects ACT_365F, but this value is not on the trade or the convention.
   */
  private static final DayCount CURVE_DAY_COUNT = DayCounts.ACT_365F;
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
  private static final boolean PROTECT_START = true;
  /**
   * ISDA Standard model implementation in analytics.
   */
  private static final AnalyticCdsPricer CALCULATOR = new AnalyticCdsPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculate present value on the specified valuation date.
   *
   * @param valuationDate date that present value is calculated on, also date that curves will be calibrated to
   * @param product  the expanded CDS product
   * @param yieldCurve  the par rates representation of the ISDA yield curve
   * @param creditCurve  the par rates representation of the ISDA credit curve
   * @param recoveryRate  the recovery rate for the reference entity/issue
   * @param scalingFactor  the scaling factor
   * @return the present value of the expanded CDS product
   */
  public static CurrencyAmount price(
      LocalDate valuationDate,
      ResolvedCds product,
      NodalCurve yieldCurve,
      NodalCurve creditCurve,
      double recoveryRate,
      double scalingFactor) {

    // setup
    CdsAnalytic cdsAnalytic = toAnalytic(valuationDate, product, recoveryRate);
    IsdaCompliantYieldCurve yieldCurveAnalytics =
        IsdaCompliantYieldCurve.makeFromRT(yieldCurve.getXValues(), yieldCurve.getYValues());
    IsdaCompliantCreditCurve creditCurveAnalytics =
        IsdaCompliantCreditCurve.makeFromRT(creditCurve.getXValues(), creditCurve.getYValues());

    // calculate
    double coupon = product.getCoupon();
    double pv = CALCULATOR.pv(cdsAnalytic, yieldCurveAnalytics, creditCurveAnalytics, coupon, CdsPriceType.DIRTY, 0d);

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
      IsdaCompliantYieldCurve yieldCurve) {

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
   * @param product  the expanded CDS product
   * @param yieldCurve  the par rates representation of the ISDA yield curve
   * @param creditCurve  the par rates representation of the ISDA credit curve
   * @param recoveryRate  the recovery rate for the reference entity/issue
   * @return the par spread of the expanded CDS product
   */
  public static double parSpread(LocalDate valuationDate,
      ResolvedCds product,
      NodalCurve yieldCurve,
      NodalCurve creditCurve,
      double recoveryRate) {
    // setup
    CdsAnalytic cdsAnalytic = toAnalytic(valuationDate, product, recoveryRate);
    IsdaCompliantYieldCurve yieldCurveAnalytics =
        IsdaCompliantYieldCurve.makeFromRT(yieldCurve.getXValues(), yieldCurve.getYValues());
    IsdaCompliantCreditCurve creditCurveAnalytics =
        IsdaCompliantCreditCurve.makeFromRT(creditCurve.getXValues(), creditCurve.getYValues());

    return CALCULATOR.parSpread(cdsAnalytic, yieldCurveAnalytics, creditCurveAnalytics);

  }

  // Converts the interest rate curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static IsdaCompliantYieldCurve createIsdaDiscountCurve(
      LocalDate valuationDate,
      IsdaYieldCurveInputs yieldCurve) {

    try {
      // model does not use floating leg of underlying IRS
      IsdaYieldCurveConvention curveConvention = yieldCurve.getCurveConvention();
      Period swapInterval = curveConvention.getFixedPaymentFrequency().getPeriod();
      DayCount mmDayCount = curveConvention.getMoneyMarketDayCount();
      DayCount swapDayCount = curveConvention.getFixedDayCount();

      BusinessDayConvention convention = curveConvention.getBusinessDayConvention();
      HolidayCalendarId holidayCalendar = curveConvention.getHolidayCalendar();

      LocalDate spotDate = curveConvention.calculateSpotDateFromTradeDate(valuationDate, REF_DATA);

      IsdaInstrumentTypes[] types =
          Stream.of(yieldCurve.getYieldCurveInstruments())
              .map(IsdaCdsHelper::mapInstrumentType)
              .toArray(IsdaInstrumentTypes[]::new);

      IsdaCompliantYieldCurveBuild builder = new IsdaCompliantYieldCurveBuild(
          valuationDate,
          spotDate,
          types,
          yieldCurve.getYieldCurvePoints(),
          mmDayCount,
          swapDayCount,
          swapInterval,
          CURVE_DAY_COUNT,
          convention,
          holidayCalendar.resolve(REF_DATA));
      return builder.build(yieldCurve.getParRates());

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Discount Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the credit curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static IsdaCompliantCreditCurve createIsdaCreditCurve(
      LocalDate valuationDate,
      IsdaCreditCurveInputs curveCurve,
      IsdaCompliantYieldCurve yieldCurve,
      double recoveryRate) {

    try {
      CdsConvention cdsConvention = curveCurve.getCdsConvention();
      FastCreditCurveBuilder builder = new FastCreditCurveBuilder(
          AccrualOnDefaultFormulae.ORIGINAL_ISDA, IsdaCompliantCreditCurveBuilder.ArbitrageHandling.Fail);
      return builder.calibrateCreditCurve(
          valuationDate,
          cdsConvention.calculateUnadjustedStepInDate(valuationDate),
          cdsConvention.calculateAdjustedSettleDate(valuationDate, REF_DATA),
          cdsConvention.calculateAdjustedStartDate(valuationDate, REF_DATA),
          curveCurve.getEndDatePoints(),
          curveCurve.getParRates(),
          cdsConvention.isPayAccruedOnDefault(),
          cdsConvention.getPaymentFrequency().getPeriod(),
          cdsConvention.getStubConvention(),
          PROTECT_START,
          yieldCurve,
          recoveryRate);

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Credit Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the credit curve par rates to the corresponding analytics form.
  // Calibration is performed here.
  public static IsdaCompliantCreditCurve createIsdaCreditCurve(
      LocalDate valuationDate,
      IsdaCreditCurveInputs curveCurve,
      NodalCurve yieldCurve,
      double recoveryRate) {

    try {
      IsdaCompliantYieldCurve yieldCurveAnalytics = IsdaCompliantYieldCurve.makeFromRT(yieldCurve.getXValues(),
          yieldCurve.getYValues());
      CdsConvention cdsConvention = curveCurve.getCdsConvention();
      FastCreditCurveBuilder builder = new FastCreditCurveBuilder(
          AccrualOnDefaultFormulae.ORIGINAL_ISDA, IsdaCompliantCreditCurveBuilder.ArbitrageHandling.Fail);
      return builder.calibrateCreditCurve(
          valuationDate,
          cdsConvention.calculateUnadjustedStepInDate(valuationDate),
          cdsConvention.calculateAdjustedSettleDate(valuationDate, REF_DATA),
          cdsConvention.calculateAdjustedStartDate(valuationDate, REF_DATA),
          curveCurve.getEndDatePoints(),
          curveCurve.getParRates(),
          cdsConvention.isPayAccruedOnDefault(),
          cdsConvention.getPaymentFrequency().getPeriod(),
          cdsConvention.getStubConvention(),
          PROTECT_START,
          yieldCurveAnalytics,
          recoveryRate);

    } catch (Exception ex) {
      throw new PricingException("Error converting the ISDA Credit Curve: " + ex.getMessage(), ex);
    }
  }

  // Converts the expanded CDS product to the corresponding analytics form.
  private static CdsAnalytic toAnalytic(LocalDate valuationDate, ResolvedCds product, double recoveryRate) {
    try {
      return new CdsAnalytic(
          valuationDate,
          valuationDate.plusDays(1),
          valuationDate,
          product.getStartDate(),
          product.getEndDate(),
          product.isPayAccruedOnDefault(),
          product.getPaymentInterval(),
          product.getStubConvention(),
          PROTECT_START,
          recoveryRate,
          product.getBusinessDayAdjustment().getConvention(),
          product.getBusinessDayAdjustment().getCalendar().resolve(REF_DATA),
          product.getAccrualDayCount(),
          CURVE_DAY_COUNT);

    } catch (Exception ex) {
      throw new PricingException("Error converting the trade to an analytic: " + ex.getMessage(), ex);
    }
  }

  //-------------------------------------------------------------------------
  // Converts type of interest curve underlying to the corresponding analytics value.
  private static IsdaInstrumentTypes mapInstrumentType(IsdaYieldCurveUnderlyingType input) {
    switch (input) {
      case ISDA_MONEY_MARKET:
        return IsdaInstrumentTypes.MONEY_MARKET;
      case ISDA_SWAP:
        return IsdaInstrumentTypes.SWAP;
      default:
        throw new IllegalStateException("Unexpected underlying type: " + input);
    }
  }

}
