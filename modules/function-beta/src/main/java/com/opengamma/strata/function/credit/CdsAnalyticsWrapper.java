package com.opengamma.strata.function.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.AnalyticCDSPricer;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.PriceType;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.pricer.PricingException;

import static com.opengamma.strata.function.credit.Converters.translateStubType;

public class CdsAnalyticsWrapper {

  private final AnalyticCDSPricer _calculator;

  public CdsAnalyticsWrapper() {
    _calculator = new AnalyticCDSPricer();
  }

  public MultiCurrencyAmount price(
      CdsTrade trade,
      NodalCurve yieldCurve,
      NodalCurve creditCurve,
      double recoveryRate
  ) {
    CDSAnalytic cdsAnalytic = toAnalytic(trade, recoveryRate);
    ISDACompliantYieldCurve yieldCurveAnalytics = toIsdaDiscountCurve(yieldCurve, trade);
    ISDACompliantCreditCurve creditCurveAnalytics = toIsdaCreditCurve(creditCurve, trade, yieldCurveAnalytics, recoveryRate);
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

  private ISDACompliantYieldCurve toIsdaDiscountCurve(NodalCurve yieldCurve, CdsTrade trade) {
    try {
      return CdsCurves.yieldCurve(trade);
      //   return ISDACompliantYieldCurve.makeFromRT(discountCurve.getXValues(), discountCurve.getYValues());
    } catch (Exception e) {
      throw new PricingException("Error converting the Isda Discount Curve: " + e.getMessage(), e);
    }
  }

  private ISDACompliantCreditCurve toIsdaCreditCurve(NodalCurve creditCurve, CdsTrade trade, ISDACompliantYieldCurve yieldCurve, double recoveryRate) {
    try {
      return CdsCurves.creditCurve(trade, yieldCurve, recoveryRate);
      //   return ISDACompliantCreditCurve.makeFromRT(creditCurve.getXValues(), creditCurve.getYValues());
    } catch (Exception e) {
      throw new PricingException("Error converting the Isda Credit Curve: " + e.getMessage() , e);
    }
  }

  private CDSAnalytic toAnalytic(CdsTrade trade, double recoveryRate) {
    try {
      return new CDSAnalytic(
          trade.modelTradeDate(),
          trade.modelStepInDate(),
          trade.modelTradeDate(),
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
