/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * An index of foreign exchange rates.
 * <p>
 * An FX rate is the conversion rate between two currencies.
 * An FX index is the rate as published by a specific organization, typically
 * at a well-known time-of-day.
 * <p>
 * The index is defined by two dates.
 * The fixing date is the date on which the index is to be observed.
 * The maturity date is the date on which delivery of the implied exchange occurs.
 * <p>
 * The most common implementations are provided in {@link FxIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface FxIndex
    extends Index, Named {

  /**
   * Obtains an instance from the specified unique name.
   * <p>
   * If the unique name can be parsed as a currency pair, an FX index will be looked up on the pair.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FxIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    Optional<FxIndex> fxIndexOpt = extendedEnum().find(uniqueName);
    if (fxIndexOpt.isPresent()) {
      return fxIndexOpt.get();
    }
    try {
      CurrencyPair currencyPair = CurrencyPair.parse(uniqueName);
      return FxIndex.of(currencyPair);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unable to create FX index from " + uniqueName);
    }
  }

  /**
   * Obtains an instance from the specified currency pair.
   * <p>
   * If a currency pair does not have an implementation, an FX index will be created.
   *
   * @param currencyPair the currency pair
   * @return the index
   */
  public static FxIndex of(CurrencyPair currencyPair) {
    ArgChecker.notNull(currencyPair, "currencyPair");
    return extendedEnum().lookupAll().values().stream()
        .filter(index -> index.getCurrencyPair().equals(currencyPair))
        .min(Comparator.comparing(FxIndex::getName))
        .orElseGet(() -> createFxIndex(currencyPair));
  }

  /**
   * Creates a FX index for the provided currency pair.
   * <p>
   * The FX index will be default to the combined holiday calendars for the currency pair.
   * The maturity day offset will default to 2 days.
   *
   * @param currencyPair the currency pair
   * @return the index
   */
  public static FxIndex createFxIndex(CurrencyPair currencyPair) {
    HolidayCalendarId calendarId = HolidayCalendarId.defaultByCurrencyPair(currencyPair);
    return ImmutableFxIndex.builder()
        .name(currencyPair.toString())
        .currencyPair(currencyPair)
        .fixingCalendar(calendarId)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, calendarId))
        .build();
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<FxIndex> extendedEnum() {
    return FxIndices.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency pair of the index.
   * 
   * @return the currency pair of the index
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Gets the adjustment applied to the maturity date to obtain the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two business days after the fixing date.
   * 
   * @return the fixing date offset
   */
  public abstract DaysAdjustment getFixingDateOffset();

  /**
   * Gets the adjustment applied to the fixing date to obtain the maturity date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two business days after the fixing date.
   * 
   * @return the maturity date offset
   */
  public abstract DaysAdjustment getMaturityDateOffset();

  /**
   * Gets the calendar that determines which dates are fixing dates.
   * <p>
   * The rate will be fixed on each business day in this calendar.
   * 
   * @return the calendar used to determine the fixing dates of the index
   */
  public abstract HolidayCalendarId getFixingCalendar();

  //-------------------------------------------------------------------------
  /**
   * Calculates the maturity date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * <p>
   * The maturity date is also known as the <i>value date</i>.
   * 
   * @param fixingDate  the fixing date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the maturity date
   */
  public abstract LocalDate calculateMaturityFromFixing(LocalDate fixingDate, ReferenceData refData);

  /**
   * Calculates the fixing date from the maturity date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * <p>
   * The maturity date is also known as the <i>value date</i>.
   * 
   * @param maturityDate  the maturity date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the fixing date
   */
  public abstract LocalDate calculateFixingFromMaturity(LocalDate maturityDate, ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Resolves this index using the specified reference data, returning a function.
   * <p>
   * This returns a {@link Function} that converts fixing dates to observations.
   * It binds the holiday calendar, looked up from the reference data, into the result.
   * As such, there is no need to pass the reference data in again.
   * <p>
   * This method is intended for use when looping to create multiple instances
   * of {@code FxIndexObservation}. Implementations of the method are intended
   * to optimize, avoiding repeated calls to resolve the holiday calendar
   * 
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return a function that converts fixing date to observation
   */
  public abstract Function<LocalDate, FxIndexObservation> resolve(ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
