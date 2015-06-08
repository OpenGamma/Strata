package com.opengamma.strata.function.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.AccrualOnDefaultFormulae;
import com.opengamma.analytics.financial.credit.isdastandardmodel.AnalyticCDSPricer;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.FastCreditCurveBuilder;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurveBuilder;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.PriceType;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.finance.credit.ExpandedCdsTrade;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;
import com.opengamma.strata.pricer.PricingException;

import java.time.LocalDate;
import java.time.Period;

import static com.opengamma.strata.function.credit.Converters.translateDayCount;
import static com.opengamma.strata.function.credit.Converters.translateStubType;

public class CdsAnalyticsWrapper {

  /**
   * DayCount used with calculating time during curve calibration
   * The model expects ACT_365F, but this value is not on the trade or the convention
   */
  private final static DayCount s_curveDayCount = DayCounts.ACT_365F;

  /**
   * If protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * The model expects this but it is not a property of the trade or convention
   * protectionFromStartOfDay If true the protection is from the start of day and the effective accrual start
   * and end dates are one day less. The exception is the final accrual end date which should have one day added
   * (if  protectionFromStartOfDay = true) in the final CDSCouponDes to compensate for this, so the
   * accrual end date is just the CDS maturity. The effect of having protectionFromStartOfDay = true
   * is to add an extra day of protection.
   */
  private final static boolean s_protectStart = true;

  /**
   * ISDA Standard model implementation in analytics
   */
  private final AnalyticCDSPricer _calculator;

  public CdsAnalyticsWrapper() {
    _calculator = new AnalyticCDSPricer();
  }

  public MultiCurrencyAmount price(
      LocalDate asOfDate,
      ExpandedCdsTrade trade,
      CurveYieldPlaceholder yieldCurve,
      CurveCreditPlaceholder creditCurve,
      double recoveryRate
  ) {
    CDSAnalytic cdsAnalytic = toAnalytic(trade, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = toIsdaDiscountCurve(asOfDate, yieldCurve);
    ISDACompliantCreditCurve creditCurveAnalytics = toIsdaCreditCurve(asOfDate, creditCurve, yieldCurveAnalytics, recoveryRate);
    double coupon = trade.getCoupon();
    double pv = _calculator.pv(
        cdsAnalytic,
        yieldCurveAnalytics,
        creditCurveAnalytics,
        coupon,
        PriceType.DIRTY
    );

    int sign = trade.getBuySellProtection().isBuy() ? 1 : -1;
    double notional = trade.getNotional();
    double adjusted = pv * notional * sign;
    double upfrontFeeAmount = priceUpfrontFee(asOfDate, trade, yieldCurveAnalytics) * sign;
    double adjustedPlusFee = adjusted + upfrontFeeAmount;
    CurrencyAmount currencyAmount = CurrencyAmount.of(trade.getCurrency(), adjustedPlusFee);
    return MultiCurrencyAmount.of(currencyAmount);
  }

  /**
   * The fee is always calculated as being payable by the protection buyer.
   */
  private double priceUpfrontFee(LocalDate asOfDate, ExpandedCdsTrade trade, ISDACompliantYieldCurve yieldCurve) {
    double feeAmount = trade.getUpfrontFeeAmount();
    if (Double.isNaN(feeAmount)) {
      return 0D; // fee missing
    }
    LocalDate feeDate = trade.getUpfrontFeePaymentDate();
    if (feeDate.isBefore(asOfDate)) {
      return 0D; // fee already paid
    }
    DayCount feeDaycount = trade.getAccrualDayCount();
    double feeSettleYearFraction = feeDaycount.yearFraction(asOfDate, feeDate);
    double discountFactor = yieldCurve.getDiscountFactor(feeSettleYearFraction);
    return discountFactor * feeAmount;
  }

  private ISDACompliantYieldCurve toIsdaDiscountCurve(LocalDate asOfDate, CurveYieldPlaceholder yieldCurve) {
    try {
      // model does not use floating leg of underlying IRS
      IsdaYieldCurveConvention curveConvention = yieldCurve.getCurveConvention();
      Period swapInterval = curveConvention.getFixedPaymentFrequency().getPeriod();
      DayCount mmDayCount = curveConvention.getMmDayCount();
      DayCount swapDayCount = curveConvention.getFixedDayCount();

      BusinessDayConvention convention = curveConvention.getBadDayConvention();
      HolidayCalendar holidayCalendar = curveConvention.getHolidayCalendar();

      LocalDate spotDate = curveConvention.getSpotDateAsOf(asOfDate);

      return new ISDACompliantYieldCurveBuild(
          asOfDate,
          spotDate,
          yieldCurve.getYieldCurveInstruments(),
          yieldCurve.getYieldCurvePoints(),
          translateDayCount(mmDayCount),
          translateDayCount(swapDayCount),
          swapInterval,
          translateDayCount(s_curveDayCount),
          convention,
          holidayCalendar
      ).build(yieldCurve.getParRates());
    } catch (Exception e) {
      throw new PricingException("Error converting the Isda Discount Curve: " + e.getMessage(), e);
    }
  }

  private ISDACompliantCreditCurve toIsdaCreditCurve(
      LocalDate asOfDate,
      CurveCreditPlaceholder curveCurve,
      ISDACompliantYieldCurve yieldCurve,
      double recoveryRate
  ) {
    try {
      StandardCdsConvention cdsConvention = curveCurve.getCdsConvention();
      return new FastCreditCurveBuilder(
          AccrualOnDefaultFormulae.OrignalISDA,
          ISDACompliantCreditCurveBuilder.ArbitrageHandling.Fail
      ).calibrateCreditCurve(
          asOfDate,
          StandardCdsConvention.calcUnadjustedStepInDate(
              asOfDate,
              cdsConvention.getStepIn()
          ),
          StandardCdsConvention.calcAdjustedSettleDate(
              asOfDate,
              cdsConvention.calcBusinessAdjustment(),
              cdsConvention.getSettleLag()
          ),
          StandardCdsConvention.calcAdjustedStartDate(
              asOfDate,
              cdsConvention.calcBusinessAdjustment()
          ),
          curveCurve.getCreditCurveEndDatePoints(asOfDate),
          curveCurve.getFractionalParSpreads(),
          cdsConvention.isPayAccOnDefault(),
          cdsConvention.getPaymentFrequency().getPeriod(),
          translateStubType(cdsConvention.getStubConvention()),
          s_protectStart,
          yieldCurve,
          recoveryRate
      );
    } catch (Exception e) {
      throw new PricingException("Error converting the Isda Credit Curve: " + e.getMessage(), e);
    }
  }

  // TODO TradeToDerivativesConverter?
  private CDSAnalytic toAnalytic(ExpandedCdsTrade trade, double recoveryRate) {
    try {
      return new CDSAnalytic(
          trade.getTradeDate(),
          trade.getStepInDate(),
          trade.getCashSettleDate(),
          trade.getAccStartDate(),
          trade.getEndDate(),
          trade.isPayAccOnDefault(),
          trade.getPaymentInterval(),
          translateStubType(trade.getStubConvention()),
          s_protectStart,
          recoveryRate,
          trade.getBusinessdayAdjustmentConvention(),
          trade.getCalendar(),
          translateDayCount(trade.getAccrualDayCount()),
          translateDayCount(s_curveDayCount)
      );
    } catch (Exception e) {
      throw new PricingException("Error converting the trade to an analytic: " + e.getMessage(), e);
    }
  }


}
