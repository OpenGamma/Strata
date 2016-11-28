/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.time.LocalDate;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Definition of caplet volatilities calibration.
 */
public interface IborCapletFloorletDefinition {

  /**
   * Gets the name of these volatilities.
   * 
   * @return the name
   */
  public abstract IborCapletFloorletVolatilitiesName getName();

  /**
   * Gets the Ibor index for which the data is valid.
   * 
   * @return the Ibor index
   */
  public abstract IborIndex getIndex();

  /**
   * Gets the day count to use.
   * 
   * @return the day count
   */
  public abstract DayCount getDayCount();

  /**
   * Creates surface metadata.
   * 
   * @param capFloorData  the cap/floor data
   * @return the surface metadata
   */
  public abstract SurfaceMetadata createMetadata(RawOptionData capFloorData);

  /**
   * Creates a standard cap from start date, end date and strike. 
   * 
   * @param startDate  the start date
   * @param endDate  the end date
   * @param strike  the strike
   * @return the cap
   */
  public default IborCapFloorLeg createCap(LocalDate startDate, LocalDate endDate, double strike) {
    IborIndex index = getIndex();
    return IborCapFloorLeg.builder()
        .calculation(IborRateCalculation.of(index))
        .capSchedule(ValueSchedule.of(strike))
        .currency(index.getCurrency())
        .notional(ValueSchedule.ALWAYS_1)
        .paymentSchedule(
            PeriodicSchedule.of(
                startDate,
                endDate,
                Frequency.of(index.getTenor().getPeriod()),
                BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, index.getFixingCalendar()),
                StubConvention.NONE,
                RollConventions.NONE))
        .payReceive(PayReceive.RECEIVE)
        .build();
  }

}
