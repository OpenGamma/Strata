/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A rate swap leg defined using a parameterized schedule and calculation.
 * <p>
 * This defines a single swap leg paying a rate, such as an interest rate.
 * The rate may be fixed or floating, see {@link FixedRateCalculation},
 * {@link IborRateCalculation} and {@link OvernightRateCalculation}.
 * <p>
 * Interest is calculated based on <i>accrual periods</i> which follow a regular schedule
 * with optional initial and final stubs. Coupon payments are based on <i>payment periods</i>
 * which are typically the same as the accrual periods.
 * If the payment period is longer than the accrual period then compounding may apply.
 * The schedule of periods is defined using {@link PeriodicSchedule}, {@link PaymentSchedule},
 * {@link NotionalSchedule} and {@link ResetSchedule}.
 * <p>
 * If the schedule needs to be manually specified, or there are other unusual calculation
 * rules then the {@link RatePeriodSwapLeg} class should be used instead.
 * 
 * @param <T>  the type of rate calculation
 */
public interface ParameterizedSwapLeg<T extends RateCalculation>
    extends SwapLeg {

  /**
   * Obtains a swap leg.
   * <p>
   * The calculation is used to select the correct leg type.
   * 
   * @param payReceive  whether the leg is pay or receive
   * @param accrualSchedule  the accrual schedule
   * @param paymentSchedule  the payment schedule
   * @param notionalSchedule  the notional schedule
   * @param calculation  the calculation
   * @return the leg
   */
  public static ParameterizedSwapLeg<? extends RateCalculation> of(
      PayReceive payReceive,
      PeriodicSchedule accrualSchedule,
      PaymentSchedule paymentSchedule,
      NotionalSchedule notionalSchedule,
      RateCalculation calculation) {

    if (calculation instanceof FixedRateCalculation) {
      return FixedRateSwapLeg.builder()
          .payReceive(payReceive)
          .accrualSchedule(accrualSchedule)
          .paymentSchedule(paymentSchedule)
          .notionalSchedule(notionalSchedule)
          .calculation((FixedRateCalculation) calculation)
          .build();
    }
    if (calculation instanceof IborRateCalculation) {
      return IborRateSwapLeg.builder()
          .payReceive(payReceive)
          .accrualSchedule(accrualSchedule)
          .paymentSchedule(paymentSchedule)
          .notionalSchedule(notionalSchedule)
          .calculation((IborRateCalculation) calculation)
          .build();
    }
    if (calculation instanceof OvernightRateCalculation) {
      return OvernightRateSwapLeg.builder()
          .payReceive(payReceive)
          .accrualSchedule(accrualSchedule)
          .paymentSchedule(paymentSchedule)
          .notionalSchedule(notionalSchedule)
          .calculation((OvernightRateCalculation) calculation)
          .build();
    }
    if (calculation instanceof InflationRateCalculation) {
      return InflationRateSwapLeg.builder()
          .payReceive(payReceive)
          .accrualSchedule(accrualSchedule)
          .paymentSchedule(paymentSchedule)
          .notionalSchedule(notionalSchedule)
          .calculation((InflationRateCalculation) calculation)
          .build();
    }
    throw new IllegalArgumentException("Unknwon calculation type: " + calculation.getClass().getName());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * 
   * @return whether to pay or receive
   */
  @Override
  public abstract PayReceive getPayReceive();

  /**
   * Gets the accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   * 
   * @return the accrual schedule
   */
  public abstract PeriodicSchedule getAccrualSchedule();

  /**
   * Gets the payment schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   * 
   * @return the payment schedule
   */
  public abstract PaymentSchedule getPaymentSchedule();

  /**
   * Gets the notional schedule.
   * <p>
   * The notional amount schedule, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   * 
   * @return the notional schedule
   */
  public abstract NotionalSchedule getNotionalSchedule();

  /**
   * Gets the interest rate accrual calculation.
   * <p>
   * Different kinds of swap leg are determined by the subclass used here.
   * See {@link FixedRateCalculation}, {@link IborRateCalculation},
   * {@link OvernightRateCalculation} and {@link InflationRateCalculation}.
   * 
   * @return the calculation
   */
  public abstract T getCalculation();

  //-------------------------------------------------------------------------
  @Override
  public default void collectIndices(ImmutableSet.Builder<Index> builder) {
    getCalculation().collectIndices(builder);
    getNotionalSchedule().getFxReset().ifPresent(fxReset -> builder.add(fxReset.getIndex()));
  }

  /**
   * Converts this swap leg to the equivalent {@code ResolvedSwapLeg}.
   * <p>
   * An {@link ResolvedSwapLeg} represents the same data as this leg, but with
   * a complete schedule of dates defined using {@link RatePaymentPeriod}.
   * 
   * @return the equivalent resolved swap leg
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid swap schedule or definition
   */
  @Override
  public default ResolvedSwapLeg resolve(ReferenceData refData) {
    DayCount dayCount = getCalculation().getDayCount();
    Schedule resolvedAccruals = getAccrualSchedule().createSchedule(refData);
    Schedule resolvedPayments = getPaymentSchedule().createSchedule(resolvedAccruals, refData);
    List<RateAccrualPeriod> accrualPeriods = getCalculation().createAccrualPeriods(resolvedAccruals, resolvedPayments, refData);
    List<NotionalPaymentPeriod> payPeriods = getPaymentSchedule().createPaymentPeriods(
        resolvedAccruals, resolvedPayments, accrualPeriods, dayCount, getNotionalSchedule(), getPayReceive(), refData);
    LocalDate startDate = accrualPeriods.get(0).getStartDate();
    return ResolvedSwapLeg.builder()
        .type(getType())
        .payReceive(getPayReceive())
        .paymentPeriods(payPeriods)
        .paymentEvents(getNotionalSchedule().createEvents(payPeriods, startDate, refData))
        .build();
  }

}
