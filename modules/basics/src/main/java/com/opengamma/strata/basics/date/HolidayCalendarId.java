/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.filteringOptional;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.named.Named;

/**
 * An identifier for a holiday calendar.
 * <p>
 * This identifier is used to obtain a {@link HolidayCalendar} from {@link ReferenceData}.
 * The holiday calendar itself is used to determine whether a day is a business day or not.
 * <p>
 * Identifiers for common holiday calendars are provided in {@link HolidayCalendarIds}.
 */
public final class HolidayCalendarId
    implements ReferenceDataId<HolidayCalendar>, Resolvable<HolidayCalendar>, Named, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /** Name splitter. */
  private static final Splitter SPLITTER_PLUS = Splitter.on('+');
  /** Name joiner. */
  private static final Joiner JOINER_PLUS = Joiner.on('+');
  /** Name splitter. */
  private static final Splitter SPLITTER_WIGGLE = Splitter.on('~');
  /** Name joiner. */
  private static final Joiner JOINER_WIGGLE = Joiner.on('~');
  /** Instance cache. */
  private static final ConcurrentHashMap<String, HolidayCalendarId> CACHE = new ConcurrentHashMap<>();

  /**
   * The identifier, expressed as a normalized unique name.
   */
  private final String name;
  /**
   * The hash code.
   */
  private final transient int hashCode;
  /**
   * The resolver function.
   * Implementations of this function must only call {@link ReferenceData#queryValueOrNull(ReferenceDataId)}.
   */
  private final transient BiFunction<HolidayCalendarId, ReferenceData, HolidayCalendar> resolver;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * <p>
   * The name uniquely identifies the calendar.
   * The {@link HolidayCalendar} is resolved from {@link ReferenceData} when required.
   * <p>
   * It is possible to combine two or more calendars using the '+' symbol.
   * For example, 'GBLO+USNY' will combine the separate 'GBLO' and 'USNY' calendars.
   * The resulting identifier will have the individual identifiers normalized into alphabetical order.
   * 
   * @param uniqueName  the unique name
   * @return the identifier
   */
  @FromString
  public static HolidayCalendarId of(String uniqueName) {
    HolidayCalendarId id = CACHE.get(uniqueName);
    return id != null ? id : create(uniqueName);
  }

  // create a new instance atomically, broken out to aid inlining
  private static HolidayCalendarId create(String name) {
    // parse names once and build resolver function to aid performance
    // name BBB+CCC+AAA changed to sorted form of AAA+BBB+CCC
    // dedicated resolver function created
    if (name.indexOf('~') >= 0) {
      List<HolidayCalendarId> ids = SPLITTER_WIGGLE.splitToList(name).stream()
          .map(n -> HolidayCalendarId.of(n))
          .distinct()
          .sorted(comparing(HolidayCalendarId::getName))
          .collect(toList());
      if (ids.contains(HolidayCalendarIds.NO_HOLIDAYS)) {
        return HolidayCalendarIds.NO_HOLIDAYS;
      }
      String normalizedName = JOINER_WIGGLE.join(ids);
      return create(name, ids, normalizedName, HolidayCalendar::linkedWith);
    } else if (name.indexOf('+') >= 0) {
      List<HolidayCalendarId> ids = SPLITTER_PLUS.splitToList(name).stream()
          .filter(n -> !n.equals(HolidayCalendarIds.NO_HOLIDAYS.getName()))
          .map(n -> HolidayCalendarId.of(n))
          .distinct()
          .sorted(comparing(HolidayCalendarId::getName))
          .collect(toList());
      String normalizedName = JOINER_PLUS.join(ids);
      return create(name, ids, normalizedName, HolidayCalendar::combinedWith);
    } else {
      return CACHE.computeIfAbsent(name, n -> new HolidayCalendarId(name));
    }
  }

  // creates a new complex id
  private static HolidayCalendarId create(
      String name,
      List<HolidayCalendarId> ids,
      String normalizedName,
      BinaryOperator<HolidayCalendar> fn) {

    BiFunction<HolidayCalendarId, ReferenceData, HolidayCalendar> resolver = (id, refData) -> {
      HolidayCalendar cal = refData.queryValueOrNull(id);
      if (cal != null) {
        return cal;
      }
      cal = HolidayCalendars.NO_HOLIDAYS;
      for (HolidayCalendarId splitId : ids) {
        HolidayCalendar splitCal = refData.queryValueOrNull(splitId);
        if (splitCal == null) {
          throw new ReferenceDataNotFoundException(Messages.format(
              "Reference data not found for '{}' of type 'HolidayCalendarId' when finding '{}'", splitId, id));
        }
        cal = fn.apply(cal, splitCal);
      }
      return cal;
    };
    // cache under the normalized and non-normalized names
    HolidayCalendarId id = CACHE.computeIfAbsent(normalizedName, n -> new HolidayCalendarId(normalizedName, resolver));
    CACHE.putIfAbsent(name, id);
    return id;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the default calendar for a currency.
   * <p>
   * This uses data from {@code HolidayCalendarDefaultData.ini} to provide a default.
   * 
   * @param currency  the currency to find the default for
   * @return the holiday calendar
   * @throws IllegalArgumentException if there is no default for the currency
   */
  public static HolidayCalendarId defaultByCurrency(Currency currency) {
    return HolidayCalendarIniLookup.INSTANCE.defaultByCurrency(currency);
  }

  /**
   * Tries to find a default calendar for a currency.
   * <p>
   * This uses data from {@code HolidayCalendarDefaultData.ini} to provide a default.
   *
   * @param currency  the currency to find the default for
   * @return the holiday calendar, empty if no calendar found
   */
  public static Optional<HolidayCalendarId> findDefaultByCurrency(Currency currency) {
    return HolidayCalendarIniLookup.INSTANCE.findDefaultByCurrency(currency);
  }

  /**
   * Gets the default calendar for a pair of currencies.
   * <p>
   * This uses data from {@code HolidayCalendarDefaultData.ini} to provide a default.
   * <p>
   * If no calendar is found, the 'NoHolidays' calendar is used as the default.
   *
   * @param currencyPair the currency pair to find the defaults for
   * @return the holiday calendar
   */
  public static HolidayCalendarId defaultByCurrencyPair(CurrencyPair currencyPair) {
    return currencyPair.toSet().stream()
        .map(HolidayCalendarId::findDefaultByCurrency)
        .flatMap(filteringOptional())
        .reduce(HolidayCalendarIds.NO_HOLIDAYS, HolidayCalendarId::combinedWith);
  }

  //-------------------------------------------------------------------------
  // creates an identifier for a single calendar
  private HolidayCalendarId(String normalizedName) {
    this.name = normalizedName;
    this.hashCode = normalizedName.hashCode();
    this.resolver = (id, refData) -> refData.queryValueOrNull(this);
  }

  // creates an identifier for a combined calendar
  private HolidayCalendarId(
      String normalizedName,
      BiFunction<HolidayCalendarId, ReferenceData, HolidayCalendar> resolver) {

    this.name = normalizedName;
    this.hashCode = normalizedName.hashCode();
    this.resolver = resolver;
  }

  // resolve after deserialization
  private Object readResolve() {
    return of(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this calendar.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public String getName() {
    return name;
  }

  /**
   * Gets the type of data this identifier refers to.
   * <p>
   * A {@code HolidayCalendarId} refers to a {@code HolidayCalendar}.
   *
   * @return the type of the reference data this identifier refers to
   */
  @Override
  public Class<HolidayCalendar> getReferenceDataType() {
    return HolidayCalendar.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this identifier to a holiday calendar using the specified reference data.
   * <p>
   * This returns an instance of {@link HolidayCalendar} that can perform calculations.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data, used to resolve the reference
   * @return the resolved holiday calendar
   * @throws ReferenceDataNotFoundException if the identifier is not found
   */
  @Override
  public HolidayCalendar resolve(ReferenceData refData) {
    return refData.getValue(this);
  }

  @Override
  public HolidayCalendar queryValueOrNull(ReferenceData refData) {
    return resolver.apply(this, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this holiday calendar identifier with another.
   * <p>
   * The resulting calendar will declare a day as a business day if it is a
   * business day in both source calendars.
   * 
   * @param other  the other holiday calendar identifier
   * @return the combined holiday calendar identifier
   */
  public HolidayCalendarId combinedWith(HolidayCalendarId other) {
    if (this == other) {
      return this;
    }
    if (this == HolidayCalendarIds.NO_HOLIDAYS) {
      return ArgChecker.notNull(other, "other");
    }
    if (other == HolidayCalendarIds.NO_HOLIDAYS) {
      return this;
    }
    return HolidayCalendarId.of(name + '+' + other.name);
  }

  /**
   * Combines this holiday calendar identifier with another.
   * <p>
   * The resulting calendar will declare a day as a business day if it is a
   * business day in either source calendar.
   * 
   * @param other  the other holiday calendar identifier
   * @return the combined holiday calendar identifier
   */
  public HolidayCalendarId linkedWith(HolidayCalendarId other) {
    if (this == other) {
      return this;
    }
    if (this == HolidayCalendarIds.NO_HOLIDAYS || other == HolidayCalendarIds.NO_HOLIDAYS) {
      return HolidayCalendarIds.NO_HOLIDAYS;
    }
    return HolidayCalendarId.of(name + '~' + other.name);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   * <p>
   * The comparison checks the name.
   * 
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    // could use (obj == this), but this code seems to be a little faster
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    HolidayCalendarId that = (HolidayCalendarId) obj;
    return name.equals(that.name);
  }

  /**
   * Returns a suitable hash code for the identifier.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Returns the name of the identifier.
   *
   * @return the name
   */
  @Override
  public String toString() {
    return name;
  }

}
