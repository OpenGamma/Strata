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
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.pricer.PricingException;

import java.time.LocalDate;
import java.time.Period;

import static com.opengamma.strata.function.credit.Converters.translateDayCount;
import static com.opengamma.strata.function.credit.Converters.translateStubType;

public class CdsAnalyticsWrapper {

  private final AnalyticCDSPricer _calculator;

  public CdsAnalyticsWrapper() {
    _calculator = new AnalyticCDSPricer();
  }

  public MultiCurrencyAmount price(
      LocalDate asOfDate,
      CdsTrade trade,
      CurveYieldPlaceholder yieldCurve,
      CurveCreditPlaceholder creditCurve,
      double recoveryRate
  ) {
    CDSAnalytic cdsAnalytic = toAnalytic(asOfDate, trade, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = toIsdaDiscountCurve(asOfDate, yieldCurve);
    ISDACompliantCreditCurve creditCurveAnalytics = toIsdaCreditCurve(asOfDate, creditCurve, yieldCurveAnalytics, recoveryRate);
    double coupon = trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedRate();
    double pv = _calculator.pv(
        cdsAnalytic,
        yieldCurveAnalytics,
        creditCurveAnalytics,
        coupon,
        PriceType.DIRTY
    );

    int sign = trade.getProduct().getGeneralTerms().getBuySellProtection().isBuy() ? 1 : -1;
    CurrencyAmount notional = trade.getProduct().getFeeLeg().getPeriodicPayments().getCalculationAmount();
    double adjusted = pv * notional.getAmount() * sign;
    CurrencyAmount currencyAmount = CurrencyAmount.of(notional.getCurrency(), adjusted);
    return MultiCurrencyAmount.of(currencyAmount);
  }

  private ISDACompliantYieldCurve toIsdaDiscountCurve(LocalDate asOfDate, CurveYieldPlaceholder yieldCurve) {
    try {
      // model does not use floating leg of underlying IRS
      IsdaYieldCurveConvention curveConvention = yieldCurve.getCurveConvention();
      Period swapInterval = curveConvention.getFixedPaymentFrequency().getPeriod();
      DayCount mmDayCount = curveConvention.getMmDayCount();
      DayCount swapDayCount = curveConvention.getFixedDayCount();
      DayCount curveDayCount = curveConvention.getCurveDayCount();

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
          translateDayCount(curveDayCount),
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
      boolean protectStart = false; // TODO where does this go?
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
              protectStart,
              yieldCurve,
              recoveryRate
          );
    } catch (Exception e) {
      throw new PricingException("Error converting the Isda Credit Curve: " + e.getMessage(), e);
    }
  }

  private CDSAnalytic toAnalytic(LocalDate asOfDate, CdsTrade trade, double recoveryRate) {
    try {
      return new CDSAnalytic(
          trade.modelTradeDate(),
          trade.modelStepInDate(),
          asOfDate,
          trade.modelAccStartDate(),
          trade.modelEndDate(),
          trade.modelPayAccOnDefault(),
          trade.modelPaymentInterval(),
          translateStubType(trade.modelStubConvention()),
          trade.modelProtectStart(),
          recoveryRate
      );
    } catch (Exception e) {
      throw new PricingException("Error converting the trade to an analytic: " + e.getMessage(), e);
    }
  }


}
