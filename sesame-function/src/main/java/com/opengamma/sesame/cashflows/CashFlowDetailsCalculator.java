/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cashflows;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.analytics.financial.instrument.payment.PaymentDefinition;
import com.opengamma.analytics.financial.interestrate.AnnuityFixingDatesVisitor;
import com.opengamma.analytics.financial.interestrate.AnnuityFixingYearFractionsVisitor;
import com.opengamma.analytics.financial.interestrate.AnnuityGearingsVisitor;
import com.opengamma.analytics.financial.interestrate.AnnuityIndexTenorsVisitor;
import com.opengamma.analytics.financial.interestrate.AnnuityPaymentDatesVisitor;
import com.opengamma.analytics.financial.interestrate.AnnuitySpreadsVisitor;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.interestrate.annuity.derivative.Annuity;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityAccrualDatesVisitor;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityFixedRatesVisitor;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityNotionalsVisitor;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityPaymentAmountsVisitor;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityPaymentFractionsVisitor;
import com.opengamma.analytics.financial.interestrate.cashflow.AnnuityPaymentTimesVisitor;
import com.opengamma.analytics.financial.interestrate.payments.derivative.Payment;
import com.opengamma.analytics.financial.interestrate.swap.derivative.Swap;
import com.opengamma.analytics.financial.interestrate.swap.derivative.SwapFixedCoupon;
import com.opengamma.analytics.financial.interestrate.swap.provider.AnnuityDiscountFactorsVisitor;
import com.opengamma.analytics.financial.interestrate.swap.provider.AnnuityForwardRatesVisitor;
import com.opengamma.analytics.financial.interestrate.swap.provider.AnnuityProjectedPaymentsVisitor;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.security.irs.PayReceiveType;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator to expose the cash flows of the swap legs.
 */
//TODO this class should be in com.opengamma.analytics.financial.provider.calculator, it is included
// in sesame so that it can reviewed in a single pull request
public final class CashFlowDetailsCalculator extends InstrumentDerivativeVisitorAdapter<CashFlowDetailsProvider, SwapLegCashFlows> {

  /**
   * The unique instance of the calculator.
   */
  private static final CashFlowDetailsCalculator INSTANCE = new CashFlowDetailsCalculator();

  /**
   * Gets the calculator instance.
   * @return The calculator.
   */
  public static CashFlowDetailsCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   */
  private CashFlowDetailsCalculator() {
  }

  private SwapLegCashFlows getCashFlows(final Swap<?, ?> derivative, final CashFlowDetailsProvider provider) {
    AnnuityDefinition<? extends PaymentDefinition> legDefinition;
    Annuity<? extends Payment> legDerivative;
    ZonedDateTime valuationTime = provider.getZonedDateTime();
    MulticurveProviderInterface bundle = provider.getMulticurveProviderInterface();
    //TODO assumption is made that the first leg of the derivative corresponds to the first leg of the definition
    boolean isFirstLegPay = derivative.getFirstLeg().getNthPayment(0).getReferenceAmount() < 0;

    if (provider.getType() == PayReceiveType.PAY) {
      legDefinition = isFirstLegPay ? provider.getDefinition().getFirstLeg() : provider.getDefinition().getSecondLeg();
      legDerivative = isFirstLegPay ? derivative.getFirstLeg() : derivative.getSecondLeg();
    } else {
      legDefinition = isFirstLegPay ? provider.getDefinition().getSecondLeg() : provider.getDefinition().getFirstLeg();
      legDerivative = isFirstLegPay ? derivative.getSecondLeg() : derivative.getFirstLeg();
    }

    CurrencyAmount[] notionals = legDefinition.accept(AnnuityNotionalsVisitor.getInstance(), valuationTime.toLocalDate());
    Pair<LocalDate[], LocalDate[]> accrualDates = legDefinition.accept(AnnuityAccrualDatesVisitor.getInstance(), valuationTime.toLocalDate());
    double[] paymentTimes = legDerivative.accept(AnnuityPaymentTimesVisitor.getInstance());
    double[] paymentFractions = legDerivative.accept(AnnuityPaymentFractionsVisitor.getInstance());
    CurrencyAmount[] paymentAmounts = legDerivative.accept(AnnuityPaymentAmountsVisitor.getInstance());
    Double[] fixedRates = legDerivative.accept(AnnuityFixedRatesVisitor.getInstance());
    double[] discountFactors = legDerivative.accept(AnnuityDiscountFactorsVisitor.getInstance(), bundle);
    if (provider.getFixed()) {
      FixedLegCashFlows details = new FixedLegCashFlows(accrualDates.getFirst(),
                                                        accrualDates.getSecond(),
                                                        discountFactors,
                                                        paymentTimes,
                                                        paymentFractions,
                                                        paymentAmounts,
                                                        notionals,
                                                        fixedRates);
      return details;
    }

    Pair<LocalDate[], LocalDate[]> fixingDates = legDefinition.accept(AnnuityFixingDatesVisitor.getInstance(), valuationTime);
    Double[] fixingYearFractions = legDefinition.accept(AnnuityFixingYearFractionsVisitor.getInstance(), valuationTime);
    Double[] forwardRates = legDerivative.accept(AnnuityForwardRatesVisitor.getInstance(), bundle);
    LocalDate[] paymentDates = legDefinition.accept(AnnuityPaymentDatesVisitor.getInstance(), valuationTime);
    CurrencyAmount[] projectedAmounts = legDerivative.accept(AnnuityProjectedPaymentsVisitor.getInstance(), bundle);
    double[] spreads = legDefinition.accept(AnnuitySpreadsVisitor.getInstance(), valuationTime);
    double[] gearings = legDefinition.accept(AnnuityGearingsVisitor.getInstance(), valuationTime);
    Tenor[] indexTenors = legDefinition.accept(AnnuityIndexTenorsVisitor.getInstance(), valuationTime);

    FloatingLegCashFlows details = new FloatingLegCashFlows(accrualDates.getFirst(),
                                                            accrualDates.getSecond(),
                                                            paymentFractions,
                                                            fixingDates.getFirst(),
                                                            fixingDates.getSecond(),
                                                            fixingYearFractions,
                                                            forwardRates,
                                                            fixedRates,
                                                            paymentDates,
                                                            paymentTimes,
                                                            discountFactors,
                                                            paymentAmounts,
                                                            projectedAmounts,
                                                            notionals,
                                                            spreads,
                                                            gearings,
                                                            indexTenors);
    return details;
  }

  @Override
  public SwapLegCashFlows visitFixedCouponSwap(final SwapFixedCoupon<?> derivative, final CashFlowDetailsProvider provider) {
    return getCashFlows(derivative, provider);
  }

  @Override
  public SwapLegCashFlows visitSwap(final Swap<?, ?> derivative, final CashFlowDetailsProvider provider) {
    return getCashFlows(derivative, provider);
  }

}
