/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cashflows;

import java.util.List;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.analytics.financial.instrument.payment.PaymentDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapDefinition;
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
public final class CashFlowDetailsCalculator extends InstrumentDerivativeVisitorAdapter<CashFlowDetailsProvider, SwapLegCashFlows> {

  @Override
  public SwapLegCashFlows visitFixedCouponSwap(SwapFixedCoupon<?> derivative, CashFlowDetailsProvider provider) {
    return getCashFlows(derivative, provider);
  }

  @Override
  public SwapLegCashFlows visitSwap(Swap<?, ?> derivative, CashFlowDetailsProvider provider) {
    return getCashFlows(derivative, provider);
  }

  private SwapLegCashFlows getCashFlows(Swap<?, ?> swapDerivative, CashFlowDetailsProvider provider) {
    AnnuityDefinition<? extends PaymentDefinition> legDefinition;
    SwapDefinition swapDefinition = provider.getDefinition();
    Annuity<? extends Payment> legDerivative;
    ZonedDateTime valuationTime = provider.getZonedDateTime();
    MulticurveProviderInterface bundle = provider.getMulticurveProviderInterface();

    boolean isDerivativeFirstLegPay = swapDerivative.getFirstLeg().getNthPayment(0).getReferenceAmount() < 0;
    boolean isDefinitionFirstLegPay = swapDefinition.getFirstLeg().getNthPayment(0).getReferenceAmount() < 0;

    if (provider.getType() == PayReceiveType.PAY) {
      legDefinition = isDefinitionFirstLegPay ? swapDefinition.getFirstLeg() : swapDefinition.getSecondLeg();
      legDerivative = isDerivativeFirstLegPay ? swapDerivative.getFirstLeg() : swapDerivative.getSecondLeg();
    } else {
      legDefinition = isDefinitionFirstLegPay ? swapDefinition.getSecondLeg() : swapDefinition.getFirstLeg();
      legDerivative = isDerivativeFirstLegPay ? swapDerivative.getSecondLeg() : swapDerivative.getFirstLeg();
    }

    //sanitize the derivative/definition arrays
    List<Double> paymentTimes = Lists.newArrayList(Doubles.asList(legDerivative.accept(AnnuityPaymentTimesVisitor.getInstance())));
    List<Double> paymentFractions = Lists.newArrayList(Doubles.asList(legDerivative.accept(AnnuityPaymentFractionsVisitor.getInstance())));
    List<Double> discountFactors = Lists.newArrayList(Doubles.asList(legDerivative.accept(AnnuityDiscountFactorsVisitor.getInstance(), bundle)));
    List<CurrencyAmount> notionals = Lists.newArrayList(legDefinition.accept(AnnuityNotionalsVisitor.getInstance(), valuationTime.toLocalDate()));
    List<Double> fixedRates = Lists.newArrayList(legDerivative.accept(AnnuityFixedRatesVisitor.getInstance()));
    List<CurrencyAmount> paymentAmounts = Lists.newArrayList(legDerivative.accept(AnnuityPaymentAmountsVisitor.getInstance()));

    Pair<LocalDate[], LocalDate[]> accrualDates = legDefinition.accept(AnnuityAccrualDatesVisitor.getInstance(), valuationTime.toLocalDate());
    List<LocalDate> accrualStartDates = Lists.newArrayList(accrualDates.getFirst());
    List<LocalDate> accrualEndDates = Lists.newArrayList(accrualDates.getSecond());

    if (provider.isFixed()) {
      return new FixedLegCashFlows(accrualStartDates,
                                   accrualEndDates,
                                   discountFactors,
                                   paymentTimes,
                                   paymentFractions,
                                   paymentAmounts,
                                   notionals,
                                   fixedRates);
    }

    //sanitize the derivative/definition arrays
    List<Double> fixingYearFractions = Lists.newArrayList(legDefinition.accept(AnnuityFixingYearFractionsVisitor.getInstance(), valuationTime));
    List<Double> forwardRates = Lists.newArrayList(legDerivative.accept(AnnuityForwardRatesVisitor.getInstance(), bundle));
    List<Double> spreads = Lists.newArrayList(Doubles.asList(legDefinition.accept(AnnuitySpreadsVisitor.getInstance(), valuationTime)));
    List<Double> gearings = Lists.newArrayList(Doubles.asList(legDefinition.accept(AnnuityGearingsVisitor.getInstance(), valuationTime)));
    List<LocalDate> paymentDates = Lists.newArrayList(legDefinition.accept(AnnuityPaymentDatesVisitor.getInstance(), valuationTime));
    List<CurrencyAmount> projectedAmounts = Lists.newArrayList(legDerivative.accept(AnnuityProjectedPaymentsVisitor.getInstance(), bundle));
    List<Tenor> indexTenors = Lists.newArrayList(legDefinition.accept(AnnuityIndexTenorsVisitor.getInstance(), valuationTime));

    Pair<LocalDate[], LocalDate[]> fixingDates = legDefinition.accept(AnnuityFixingDatesVisitor.getInstance(), valuationTime);
    List<LocalDate> fixingStartDates = Lists.newArrayList(fixingDates.getFirst());
    List<LocalDate> fixingEndDates = Lists.newArrayList(fixingDates.getSecond());

    return new FloatingLegCashFlows(accrualStartDates,
                                    accrualEndDates,
                                    paymentFractions,
                                    fixingStartDates,
                                    fixingEndDates,
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
  }

}
