package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.util.Epsilon;
import com.opengamma.strata.product.credit.cds.CreditCouponPaymentPeriod;
import com.opengamma.strata.product.credit.cds.ResolvedCds;

public class IsdaCdsProductPricer {

  /**
   * Default implementation.
   */
  public static final IsdaCdsProductPricer DEFAULT = new IsdaCdsProductPricer(AccrualOnDefaultFormulae.ORIGINAL_ISDA);

  /**
   * The formula
   */
  private final AccrualOnDefaultFormulae formula;

  /**
   * The omega parameter.
   */
  private final double omega;

  /**
   * Which formula to use for the accrued on default calculation.  
   * @param formula Options are the formula given in the ISDA model (version 1.8.2 and lower); the proposed fix by Markit (given as a comment in  
   * version 1.8.2, or the mathematically correct formula 
   */
  public IsdaCdsProductPricer(AccrualOnDefaultFormulae formula) {
    this.formula = ArgChecker.notNull(formula, "formula");
    if (formula == AccrualOnDefaultFormulae.ORIGINAL_ISDA) {
      omega = 1d / 730d;
    } else {
      omega = 0d;
    }
  }

  public CurrencyAmount presentValue(ResolvedCds cds, CreditRatesProvider ratesProvider, ReferenceData refData) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(refData, "refData");
    if (cds.getProtectionEndDate().isBefore(ratesProvider.getValuationDate())) { //short cut already expired CDSs
      return CurrencyAmount.of(cds.getCurrency(), 0d);
    }
    StandardId legalEntity = cds.getLegalEntityId();
    Currency currency = cds.getCurrency();
    LocalDate cashSettleDate = cds.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate stepinDate = cds.getStepinDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate effectiveStartDate = cds.getEffectiveStartDate(stepinDate);
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntity);
    ArgChecker.isTrue(recoveryRates instanceof ConstantRecoveryRates, "recoveryRates must be ConstantRecoveryRates");
    double recoveryRate = recoveryRates.recoveryRate(cds.getProtectionEndDate()); // constant recovery rate

    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    ArgChecker.isTrue(discountFactors instanceof IsdaCompliantZeroRateDiscountFactors,
        "discountFactors must be IsdaCompliantZeroRateDiscountFactors");
    CreditDiscountFactors survivalProbabilities = ratesProvider.survivalProbabilities(legalEntity, currency);
    ArgChecker.isTrue(survivalProbabilities instanceof IsdaCompliantZeroRateDiscountFactors,
        "survivalProbabilities must be IsdaCompliantZeroRateDiscountFactors");
    IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors = (IsdaCompliantZeroRateDiscountFactors) discountFactors;
    IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities = (IsdaCompliantZeroRateDiscountFactors) survivalProbabilities;
    ArgChecker.isTrue(isdaDiscountFactors.getDayCount().equals(isdaSurvivalProbabilities.getDayCount()),
        "day count conventions of discounting curve and credit curve must be the same");

    double price =
        protectionLeg(cds, isdaDiscountFactors, isdaSurvivalProbabilities, cashSettleDate, effectiveStartDate, recoveryRate) -
            dirtyRiskyAnnuity(cds, isdaDiscountFactors, isdaSurvivalProbabilities, stepinDate, cashSettleDate,
                effectiveStartDate) * cds.getCoupon() +
            cds.accruedInterest(stepinDate);

    return CurrencyAmount.of(cds.getCurrency(), cds.getBuySell().normalize(cds.getNotional()) * price);
  }

  //TODO par spread, RPV01, sensitivities

  public double protectionLeg(ResolvedCds cds, CreditRatesProvider ratesProvider, ReferenceData refData) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(refData, "refData");
    if (cds.getProtectionEndDate().isBefore(ratesProvider.getValuationDate())) { //short cut already expired CDSs
      return 0d;
    }
    StandardId legalEntity = cds.getLegalEntityId();
    Currency currency = cds.getCurrency();
    LocalDate cashSettleDate = cds.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate stepinDate = cds.getStepinDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate effectiveStartDate = cds.getEffectiveStartDate(stepinDate);
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntity);
    ArgChecker.isTrue(recoveryRates instanceof ConstantRecoveryRates, "recoveryRates must be ConstantRecoveryRates");
    double recoveryRate = recoveryRates.recoveryRate(cds.getProtectionEndDate()); // constant recovery rate

    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    ArgChecker.isTrue(discountFactors instanceof IsdaCompliantZeroRateDiscountFactors,
        "discountFactors must be IsdaCompliantZeroRateDiscountFactors");
    CreditDiscountFactors survivalProbabilities = ratesProvider.survivalProbabilities(legalEntity, currency);
    ArgChecker.isTrue(survivalProbabilities instanceof IsdaCompliantZeroRateDiscountFactors,
        "survivalProbabilities must be IsdaCompliantZeroRateDiscountFactors");
    IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors = (IsdaCompliantZeroRateDiscountFactors) discountFactors;
    IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities = (IsdaCompliantZeroRateDiscountFactors) survivalProbabilities;
    ArgChecker.isTrue(isdaDiscountFactors.getDayCount().equals(isdaSurvivalProbabilities.getDayCount()),
        "day count conventions of discounting curve and credit curve must be the same");

    return protectionLeg(cds, isdaDiscountFactors, isdaSurvivalProbabilities, cashSettleDate, effectiveStartDate, recoveryRate);
  }

  public double dirtyRiskyAnnuity(ResolvedCds cds, CreditRatesProvider ratesProvider, ReferenceData refData) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(refData, "refData");
    if (cds.getProtectionEndDate().isBefore(ratesProvider.getValuationDate())) { //short cut already expired CDSs
      return 0d;
    }
    StandardId legalEntity = cds.getLegalEntityId();
    Currency currency = cds.getCurrency();
    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    ArgChecker.isTrue(discountFactors instanceof IsdaCompliantZeroRateDiscountFactors,
        "discountFactors must be IsdaCompliantZeroRateDiscountFactors");
    CreditDiscountFactors survivalProbabilities = ratesProvider.survivalProbabilities(legalEntity, currency);
    ArgChecker.isTrue(survivalProbabilities instanceof IsdaCompliantZeroRateDiscountFactors,
        "survivalProbabilities must be IsdaCompliantZeroRateDiscountFactors");
    IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors = (IsdaCompliantZeroRateDiscountFactors) discountFactors;
    IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities = (IsdaCompliantZeroRateDiscountFactors) survivalProbabilities;
    ArgChecker.isTrue(isdaDiscountFactors.getDayCount().equals(isdaSurvivalProbabilities.getDayCount()),
        "day count conventions of discounting curve and credit curve must be the same");

    LocalDate cashSettleDate = cds.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate stepinDate = cds.getStepinDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate effectiveStartDate = cds.getEffectiveStartDate(stepinDate);

    return dirtyRiskyAnnuity(cds, isdaDiscountFactors, isdaSurvivalProbabilities, stepinDate, cashSettleDate, effectiveStartDate);
  }

  //-------------------------------------------------------------------------
  private double protectionLeg(
      ResolvedCds cds,
      IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors,
      IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities,
      LocalDate cashSettleDate,
      LocalDate effectiveStartDate,
      double recoveryRate) {

    double[] integrationSchedule = DoublesScheduleGenerator.getIntegrationsPoints(
        isdaDiscountFactors.relativeYearFraction(effectiveStartDate),
        isdaDiscountFactors.relativeYearFraction(cds.getProtectionEndDate()),
        isdaDiscountFactors.getParameterKeys(),
        isdaSurvivalProbabilities.getParameterKeys());

    double pv = 0d;
    double ht0 = isdaSurvivalProbabilities.zeroRateYearFraction(integrationSchedule[0]);
    double rt0 = isdaDiscountFactors.zeroRateYearFraction(integrationSchedule[0]);
    double b0 = Math.exp(-ht0 - rt0);
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
      double dPV = 0d;
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

    // roll to the cash settle date
    double df = isdaDiscountFactors.discountFactor(cashSettleDate);
    pv /= df;

    return pv;
  }

  private double dirtyRiskyAnnuity(
      ResolvedCds cds,
      IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors,
      IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities,
      LocalDate stepinDate,
      LocalDate cashSettleDate,
      LocalDate effectiveStartDate) {

    double pv = 0d;
    for (CreditCouponPaymentPeriod coupon : cds.getPeriodicPayments()) {
      if (stepinDate.isBefore(coupon.getEndDate())) {
        double q = isdaSurvivalProbabilities.discountFactor(coupon.getEffectiveEndDate());
        double p = isdaDiscountFactors.discountFactor(coupon.getPaymentDate());
        pv += coupon.getYearFraction() * p * q;
      }
    }

    if (cds.getPaymentOnDefault().isAccruedInterest()) {
      //This is needed so that the code is consistent with ISDA C when the Markit `fix' is used. For forward starting CDS (accStart > trade-date),
      //and more than one coupon, the C code generates an extra integration point (a node at protection start and one the day before) - normally
      //the second point could be ignored (since is doesn't correspond to a node of the curves, nor is it the start point), but the Markit fix is 
      //mathematically incorrect, so this point affects the result.  
      LocalDate start = cds.getPeriodicPayments().size() == 1 ? effectiveStartDate : cds.getAccrualStartDate();
      double[] integrationSchedule = DoublesScheduleGenerator.getIntegrationsPoints(
          isdaDiscountFactors.relativeYearFraction(start),
          isdaDiscountFactors.relativeYearFraction(cds.getProtectionEndDate()),
          isdaDiscountFactors.getParameterKeys(),
          isdaSurvivalProbabilities.getParameterKeys());
      for (CreditCouponPaymentPeriod coupon : cds.getPeriodicPayments()) {
        pv += calculateSinglePeriodAccrualOnDefault(
            coupon, effectiveStartDate, integrationSchedule, isdaDiscountFactors, isdaSurvivalProbabilities);
      }
    }

    // roll to the cash settle date
    double df = isdaDiscountFactors.discountFactor(cashSettleDate);
    pv /= df;

    return pv;
  }

  private double calculateSinglePeriodAccrualOnDefault(
      CreditCouponPaymentPeriod coupon,
      LocalDate effectiveStartDate,
      double[] integrationSchedule,
      IsdaCompliantZeroRateDiscountFactors isdaDiscountFactors,
      IsdaCompliantZeroRateDiscountFactors isdaSurvivalProbabilities) {

    LocalDate start =
        coupon.getEffectiveStartDate().isBefore(effectiveStartDate) ? effectiveStartDate : coupon.getEffectiveStartDate();
    if (!start.isBefore(coupon.getEffectiveEndDate())) {
      return 0d; //this coupon has already expired 
    }

    double[] knots = DoublesScheduleGenerator.truncateSetInclusive(isdaDiscountFactors.relativeYearFraction(start),
        isdaDiscountFactors.relativeYearFraction(coupon.getEffectiveEndDate()), integrationSchedule);

    double t = knots[0];
    double ht0 = isdaSurvivalProbabilities.zeroRateYearFraction(t);
    double rt0 = isdaDiscountFactors.zeroRateYearFraction(t);
    double b0 = Math.exp(-rt0 - ht0); // this is the risky discount factor

    double effStart = isdaDiscountFactors.relativeYearFraction(coupon.getEffectiveStartDate());
    double t0 = t - effStart + omega;
    double pv = 0d;
    final int nItems = knots.length;
    for (int j = 1; j < nItems; ++j) {
      t = knots[j];
      double ht1 = isdaSurvivalProbabilities.zeroRateYearFraction(t);
      double rt1 = isdaDiscountFactors.zeroRateYearFraction(t);
      double b1 = Math.exp(-rt1 - ht1);

      double dt = knots[j] - knots[j - 1];

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      double tPV;
      if (formula == AccrualOnDefaultFormulae.MARKIT_FIX) {
        if (Math.abs(dhrt) < 1e-5) {
          tPV = dht * dt * b0 * Epsilon.epsilonP(-dhrt);
        } else {
          tPV = dht * dt / dhrt * ((b0 - b1) / dhrt - b1);
        }
      } else {
        double t1 = t - effStart + omega;
        if (Math.abs(dhrt) < 1e-5) {
          tPV = dht * b0 * (t0 * epsilon(-dhrt) + dt * Epsilon.epsilonP(-dhrt));
        } else {
          tPV = dht / dhrt * (t0 * b0 - t1 * b1 + dt / dhrt * (b0 - b1));
        }
        t0 = t1;
      }

      pv += tPV;
      ht0 = ht1;
      rt0 = rt1;
      b0 = b1;
    }

    double yearFractionCurve =
        isdaDiscountFactors.getDayCount().relativeYearFraction(coupon.getStartDate(), coupon.getEndDate());
//    System.out.println(coupon.getYearFraction() / yearFractionCurve + "\t" + pv);
    return coupon.getYearFraction() * pv / yearFractionCurve;
  }

}
