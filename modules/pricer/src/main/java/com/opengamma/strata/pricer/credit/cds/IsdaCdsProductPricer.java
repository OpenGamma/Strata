package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;

import java.time.LocalDate;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.credit.cds.ResolvedCds;

public class IsdaCdsProductPricer {

  public double protectionLeg(
      ResolvedCds cds,
      CreditRatesProvider ratesProvider,
      LocalDate referenceDate) { // TODO mention reference date == cash settle date

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    if (cds.getEndDate().isBefore(referenceDate)) { //short cut already expired CDSs
      return 0d;
    }
    
    StandardId legalEntity = cds.getLegalEntityId();
    Currency currency = cds.getCurrency();

    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntity);
    ArgChecker.isTrue(recoveryRates instanceof ConstantRecoveryRates);
    double recoveryRate = recoveryRates.recoveryRate(referenceDate);

    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    ArgChecker.isTrue(discountFactors instanceof IsdaCompliantZeroRateDiscountFactors,
        "discountFactors must be IsdaCompliantZeroRateDiscountFactors");
    CreditDiscountFactors survivalProbabilities =
        ratesProvider.survivalProbabilities(legalEntity, currency).getSurvivalProbabilities(); // TODO
    ArgChecker.isTrue(survivalProbabilities instanceof IsdaCompliantZeroRateDiscountFactors,
        "survivalProbabilities must be IsdaCompliantZeroRateDiscountFactors");

    IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors = (IsdaCompliantZeroRateDiscountFactors) discountFactors;
    IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities = (IsdaCompliantZeroRateDiscountFactors) survivalProbabilities;

    LocalDate effectiveStartDate =
        cds.getStartDate().isBefore(ratesProvider.getValuationDate()) ? ratesProvider.getValuationDate() : cds.getStartDate();
    if (cds.isProtectStart()) {
      effectiveStartDate = effectiveStartDate.minusDays(1);
    }

    double[] integrationSchedule = DoublesScheduleGenerator.getIntegrationsPoints(
        isdaDiscountFactors.relativeYearFraction(effectiveStartDate),
        isdaDiscountFactors.relativeYearFraction(cds.getEndDate()),
        isdaDiscountFactors.getParameterKeys(),
        isdaSurvivalProbabilities.getParameterKeys()); // TODO two curve should be based on the same dayCount

    // 6.7972602739726025
    double ht0 = isdaSurvivalProbabilities.zeroRateYearFraction(integrationSchedule[0]);
    double rt0 = isdaDiscountFactors.zeroRateYearFraction(integrationSchedule[0]);
    double b0 = Math.exp(-ht0 - rt0); // risky discount factor

    double pv = 0.0;
    int n = integrationSchedule.length;
    for (int i = 1; i < n; ++i) {

      double ht1 = isdaSurvivalProbabilities.zeroRateYearFraction(integrationSchedule[i]);
      double rt1 = isdaDiscountFactors.zeroRateYearFraction(integrationSchedule[i]);
      double b1 = Math.exp(-ht1 - rt1);

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      // The formula has been modified from ISDA (but is equivalent) to avoid log(exp(x)) and explicitly
      // calculating the time step - it also handles the limit
      double dPV;
      if (Math.abs(dhrt) < 1e-5) {
        dPV = dht * b0 * epsilon(-dhrt);
      } else {
        dPV = (b0 - b1) * dht / dhrt;
      }

      pv += dPV;
      ht0 = ht1;
      rt0 = rt1;
      b0 = b1;
    }
    pv *= (1d - recoveryRate);

    // roll to the valuation date
    double df = isdaDiscountFactors.discountFactor(referenceDate);
    pv /= df;

    return pv;
  }

}
