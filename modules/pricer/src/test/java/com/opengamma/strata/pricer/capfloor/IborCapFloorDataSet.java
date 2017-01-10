/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Data set of Ibor cap/floor securities.
 */
public class IborCapFloorDataSet {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BUSINESS_ADJ = BusinessDayAdjustment.of(
      BusinessDayConventions.MODIFIED_FOLLOWING, EUTA);

  //-------------------------------------------------------------------------
  /**
   * Creates an Ibor cap/floor leg.
   * <p>
   * The Ibor index should be {@code EUR_EURIBOR_3M} or {@code EUR_EURIBOR_6M} to match the availability of the curve 
   * data in {@link IborCapletFloorletDataSet}. 
   * 
   * @param index  the index
   * @param startDate  the start date
   * @param endDate  the end date
   * @param strikeSchedule  the strike
   * @param notionalSchedule  the notional
   * @param putCall  cap or floor
   * @param payRec  pay or receive
   * @return the instance
   */
  public static ResolvedIborCapFloorLeg createCapFloorLeg(
      IborIndex index,
      LocalDate startDate,
      LocalDate endDate,
      ValueSchedule strikeSchedule,
      ValueSchedule notionalSchedule,
      PutCall putCall,
      PayReceive payRec) {

    IborCapFloorLeg leg =
        createCapFloorLegUnresolved(index, startDate, endDate, strikeSchedule, notionalSchedule, putCall, payRec);
    return leg.resolve(REF_DATA);
  }

  /**
   * Creates an Ibor cap/floor leg.
   * <p>
   * The Ibor index should be {@code EUR_EURIBOR_3M} or {@code EUR_EURIBOR_6M} to match the availability of the curve 
   * data in {@link IborCapletFloorletDataSet}. 
   * 
   * @param index  the index
   * @param startDate  the start date
   * @param endDate  the end date
   * @param strikeSchedule  the strike
   * @param notionalSchedule  the notional
   * @param putCall  cap or floor
   * @param payRec  pay or receive
   * @return the instance
   */
  public static IborCapFloorLeg createCapFloorLegUnresolved(
      IborIndex index,
      LocalDate startDate,
      LocalDate endDate,
      ValueSchedule strikeSchedule,
      ValueSchedule notionalSchedule,
      PutCall putCall,
      PayReceive payRec) {

    Frequency frequency = Frequency.of(index.getTenor().getPeriod());
    PeriodicSchedule paySchedule =
        PeriodicSchedule.of(startDate, endDate, frequency, BUSINESS_ADJ, StubConvention.NONE, RollConventions.NONE);
    IborRateCalculation rateCalculation = IborRateCalculation.of(index);
    if (putCall.isCall()) {
      return IborCapFloorLeg.builder()
          .calculation(rateCalculation)
          .capSchedule(strikeSchedule)
          .notional(notionalSchedule)
          .paymentSchedule(paySchedule)
          .payReceive(payRec)
          .build();
    }
    return IborCapFloorLeg.builder()
        .calculation(rateCalculation)
        .floorSchedule(strikeSchedule)
        .notional(notionalSchedule)
        .paymentSchedule(paySchedule)
        .payReceive(payRec)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Create a pay leg.
   * <p>
   * The pay leg created is periodic fixed rate payments without compounding.
   * The Ibor index is used to specify the payment frequency.
   * 
   * @param index  the Ibor index
   * @param startDate  the start date
   * @param endDate  the end date
   * @param fixedRate  the fixed rate
   * @param notional  the notional
   * @param payRec  pay or receive 
   * @return the instance
   */
  public static ResolvedSwapLeg createFixedPayLeg(
      IborIndex index,
      LocalDate startDate,
      LocalDate endDate,
      double fixedRate,
      double notional,
      PayReceive payRec) {

    SwapLeg leg = createFixedPayLegUnresolved(index, startDate, endDate, fixedRate, notional, payRec);
    return leg.resolve(REF_DATA);
  }

  /**
   * Create a pay leg.
   * <p>
   * The pay leg created is periodic fixed rate payments without compounding.
   * The Ibor index is used to specify the payment frequency.
   * 
   * @param index  the Ibor index
   * @param startDate  the start date
   * @param endDate  the end date
   * @param fixedRate  the fixed rate
   * @param notional  the notional
   * @param payRec  pay or receive 
   * @return the instance
   */
  public static SwapLeg createFixedPayLegUnresolved(
      IborIndex index,
      LocalDate startDate,
      LocalDate endDate,
      double fixedRate,
      double notional,
      PayReceive payRec) {

    Frequency frequency = Frequency.of(index.getTenor().getPeriod());
    PeriodicSchedule accSchedule =
        PeriodicSchedule.of(startDate, endDate, frequency, BUSINESS_ADJ, StubConvention.NONE, RollConventions.NONE);
    return RateCalculationSwapLeg.builder()
        .payReceive(payRec)
        .accrualSchedule(accSchedule)
        .calculation(
            FixedRateCalculation.of(fixedRate, ACT_360))
        .paymentSchedule(
            PaymentSchedule.builder().paymentFrequency(frequency).paymentDateOffset(DaysAdjustment.NONE).build())
        .notionalSchedule(
            NotionalSchedule.of(CurrencyAmount.of(EUR, notional)))
        .build();
  }

}
