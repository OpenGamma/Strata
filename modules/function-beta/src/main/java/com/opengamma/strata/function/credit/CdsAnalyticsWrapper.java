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
import com.opengamma.strata.finance.credit.CdsModelTrade;
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
   */
  private final static boolean s_protectStart = false;

  /**
   * ISDA Standard model implementation in analytics
   */
  private final AnalyticCDSPricer _calculator;

  public CdsAnalyticsWrapper() {
    _calculator = new AnalyticCDSPricer();
  }

  public MultiCurrencyAmount price(
      LocalDate asOfDate,
      CdsModelTrade trade,
      CurveYieldPlaceholder yieldCurve,
      CurveCreditPlaceholder creditCurve,
      double recoveryRate
  ) {
    CDSAnalytic cdsAnalytic = toAnalytic(asOfDate, trade, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = toIsdaDiscountCurve(asOfDate, yieldCurve);
    ISDACompliantCreditCurve creditCurveAnalytics = toIsdaCreditCurve(asOfDate, creditCurve, yieldCurveAnalytics, recoveryRate);
    double coupon = trade.coupon();
    double pv = _calculator.pv(
        cdsAnalytic,
        yieldCurveAnalytics,
        creditCurveAnalytics,
        coupon,
        PriceType.DIRTY
    );

    int sign = trade.buySellProtection().isBuy() ? 1 : -1;
    double notional = trade.notional();
    double adjusted = pv * notional * sign;
    double upfrontFeeAmount = priceUpfrontFee(asOfDate, trade, yieldCurveAnalytics) * sign;
    double adjustedPlusFee = adjusted + upfrontFeeAmount;
    CurrencyAmount currencyAmount = CurrencyAmount.of(trade.currency(), adjustedPlusFee);
    return MultiCurrencyAmount.of(currencyAmount);
  }

  /**
   * The fee is always calculated as being payable by the protection buyer.
   */
  private double priceUpfrontFee(LocalDate asOfDate, CdsModelTrade trade, ISDACompliantYieldCurve yieldCurve) {
    double feeAmount = trade.upfrontFeeAmount();
    LocalDate feeDate = trade.upfrontFeePaymentDate();
    if (feeAmount == 0D || feeDate.isBefore(asOfDate)) {
      return 0D; // fee missing, zero or already paid
    }
    DayCount feeDaycount = trade.accrualDayCount();
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
      return new FastCreditCurveBuilder(AccrualOnDefaultFormulae.OrignalISDA, ISDACompliantCreditCurveBuilder.ArbitrageHandling.Fail)
          .calibrateCreditCurve(
              asOfDate,
              cdsConvention.calcStepInDate(asOfDate),
              asOfDate,
              cdsConvention.calcUnadjustedAccrualStartDate(asOfDate),
              curveCurve.getCreditCurvePoints(asOfDate),
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

  private CDSAnalytic toAnalytic(LocalDate asOfDate, CdsModelTrade trade, double recoveryRate) {
    try {
      return new CDSAnalytic(
          trade.tradeDate(),
          trade.stepInDate(),
          asOfDate,
          trade.accStartDate(),
          trade.endDate(),
          trade.payAccOnDefault(),
          trade.paymentInterval(),
          translateStubType(trade.stubConvention()),
          s_protectStart,
          recoveryRate,
          trade.businessdayAdjustmentConvention(),
          trade.calendar(),
          translateDayCount(trade.accrualDayCount()),
          translateDayCount(s_curveDayCount)
      );
    } catch (Exception e) {
      throw new PricingException("Error converting the trade to an analytic: " + e.getMessage(), e);
    }
  }


}
