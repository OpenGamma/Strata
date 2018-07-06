/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.time.LocalDate;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Defines the computation of a rate from a single Overnight index.
 */
public interface OvernightRateComputation
    extends RateComputation {

  /**
   * Obtains an instance.
   * 
   * @param index  the index
   * @param startDate  the start date
   * @param endDate  the end date
   * @param rateCutOffDays  the rate cutoff days
   * @param accrualMethod  the accrual method
   * @param referenceData  the reference data
   * @return the instance
   */
  public static OvernightRateComputation of(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate,
      int rateCutOffDays,
      OvernightAccrualMethod accrualMethod,
      ReferenceData referenceData) {

    switch (accrualMethod) {
      case COMPOUNDED:
        return OvernightCompoundedRateComputation.of(index, startDate, endDate, rateCutOffDays, referenceData);
      case AVERAGED:
        return OvernightAveragedRateComputation.of(index, startDate, endDate, rateCutOffDays, referenceData);
      case AVERAGED_DAILY:
        return OvernightAveragedDailyRateComputation.of(index, startDate, endDate, referenceData);
      default:
        throw new IllegalArgumentException(Messages.format("unsupported Overnight accrual method, {}", accrualMethod));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the Overnight index.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-SONIA'.
   * 
   * @return the overnight index
   */
  public abstract OvernightIndex getIndex();

  /**
   * Obtains the resolved calendar that the index uses.
   * 
   * @return the fixing calendar
   */
  public abstract HolidayCalendar getFixingCalendar();

  /**
   * Obtains the fixing date associated with the start date of the accrual period.
   * <p>
   * This is also the first fixing date.
   * The overnight rate is observed from this date onwards.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   * 
   * @return the start date
   */
  public abstract LocalDate getStartDate();

  /**
   * Obtains the fixing date associated with the end date of the accrual period.
   * <p>
   * The overnight rate is observed until this date.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   * 
   * @return the end date
   */
  public abstract LocalDate getEndDate();

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  public default LocalDate calculatePublicationFromFixing(LocalDate fixingDate) {
    return getFixingCalendar().shift(getFixingCalendar().nextOrSame(fixingDate), getIndex().getPublicationDateOffset());
  }

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  public default LocalDate calculateEffectiveFromFixing(LocalDate fixingDate) {
    return getFixingCalendar().shift(getFixingCalendar().nextOrSame(fixingDate), getIndex().getEffectiveDateOffset());
  }

  /**
   * Calculates the maturity date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the maturity date
   */
  public default LocalDate calculateMaturityFromFixing(LocalDate fixingDate) {
    return getFixingCalendar().shift(getFixingCalendar().nextOrSame(fixingDate), getIndex().getEffectiveDateOffset() + 1);
  }

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  public default LocalDate calculateFixingFromEffective(LocalDate effectiveDate) {
    return getFixingCalendar().shift(getFixingCalendar().nextOrSame(effectiveDate), -getIndex().getEffectiveDateOffset());
  }

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  public default LocalDate calculateMaturityFromEffective(LocalDate effectiveDate) {
    return getFixingCalendar().shift(getFixingCalendar().nextOrSame(effectiveDate), 1);
  }

  /**
   * Creates an observation object for the specified fixing date.
   * 
   * @param fixingDate  the fixing date
   * @return the index observation
   */
  public default OvernightIndexObservation observeOn(LocalDate fixingDate) {
    LocalDate publicationDate = calculatePublicationFromFixing(fixingDate);
    LocalDate effectiveDate = calculateEffectiveFromFixing(fixingDate);
    LocalDate maturityDate = calculateMaturityFromEffective(effectiveDate);
    return OvernightIndexObservation.builder()
        .index(getIndex())
        .fixingDate(fixingDate)
        .publicationDate(publicationDate)
        .effectiveDate(effectiveDate)
        .maturityDate(maturityDate)
        .yearFraction(getIndex().getDayCount().yearFraction(effectiveDate, maturityDate))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public default void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(getIndex());
  }

}
