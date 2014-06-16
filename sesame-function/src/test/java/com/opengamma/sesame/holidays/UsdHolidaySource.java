package com.opengamma.sesame.holidays;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableSet;
import com.opengamma.core.AbstractSource;
import com.opengamma.core.holiday.Holiday;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.HolidayType;
import com.opengamma.core.holiday.impl.SimpleHoliday;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.money.Currency;


public class UsdHolidaySource extends AbstractSource<Holiday> implements HolidaySource {

  /**
   * Map of exception dates and whether they are working or non-working.
   */
  private final ConcurrentMap<LocalDate, Boolean> _nonWorkingDay = new ConcurrentHashMap<>();

  public UsdHolidaySource() {
    int startYear = 2013;
    int endYear = 2063;
    for (int year = startYear; year <= endYear; year++) {
      addNonWorkingDay(LocalDate.of(year, 1, 1));
      addNonWorkingDay(LocalDate.of(year, 7, 4));
      addNonWorkingDay(LocalDate.of(year, 12, 25));
    }
    addNonWorkingDay(LocalDate.of(2015, 1, 19));
    addNonWorkingDay(LocalDate.of(2015, 2, 16));
    addNonWorkingDay(LocalDate.of(2015, 5, 25));
    addNonWorkingDay(LocalDate.of(2015, 9, 7));
    addNonWorkingDay(LocalDate.of(2015, 10, 12));
    addNonWorkingDay(LocalDate.of(2015, 11, 11));
    addNonWorkingDay(LocalDate.of(2015, 11, 26));
    addNonWorkingDay(LocalDate.of(2016, 1, 18));
    addNonWorkingDay(LocalDate.of(2016, 2, 15));
    addNonWorkingDay(LocalDate.of(2016, 5, 30));
    addNonWorkingDay(LocalDate.of(2016, 9, 5));
    addNonWorkingDay(LocalDate.of(2016, 10, 10));
    addNonWorkingDay(LocalDate.of(2016, 11, 11));
    addNonWorkingDay(LocalDate.of(2016, 11, 24));
    addNonWorkingDay(LocalDate.of(2016, 12, 26));
    addNonWorkingDay(LocalDate.of(2017, 1, 2));
    addNonWorkingDay(LocalDate.of(2017, 1, 16));
    addNonWorkingDay(LocalDate.of(2017, 2, 20));
    addNonWorkingDay(LocalDate.of(2017, 5, 29));
    addNonWorkingDay(LocalDate.of(2017, 9, 4));
    addNonWorkingDay(LocalDate.of(2017, 10, 9));
    addNonWorkingDay(LocalDate.of(2017, 11, 23));
  }

  private void addNonWorkingDay(LocalDate date) {
    _nonWorkingDay.put(date, true);
  }

  private boolean isHoliday(LocalDate dateToCheck) {
    DayOfWeek day = dateToCheck.getDayOfWeek();
    if (day.equals(DayOfWeek.SATURDAY) || day.equals(DayOfWeek.SUNDAY)) {
      return true;
    }
    return _nonWorkingDay.containsKey(dateToCheck);
  }

  @Override
  public boolean isHoliday(LocalDate dateToCheck, Currency currency) {
    return isHoliday(dateToCheck);
  }

  @Override
  public boolean isHoliday(LocalDate dateToCheck, HolidayType holidayType, ExternalIdBundle regionOrExchangeIds) {
    return isHoliday(dateToCheck);
  }

  @Override
  public boolean isHoliday(LocalDate dateToCheck, HolidayType holidayType, ExternalId regionOrExchangeId) {
    return isHoliday(dateToCheck);
  }

  @Override
  public Holiday get(UniqueId uniqueId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Holiday get(ObjectId objectId, VersionCorrection versionCorrection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Holiday> get(HolidayType holidayType,
                                 ExternalIdBundle regionOrExchangeIds) {

    SimpleHoliday holiday = new SimpleHoliday(_nonWorkingDay.keySet());
    holiday.setType(holidayType);
    holiday.setRegionExternalId(regionOrExchangeIds.iterator().next());
    return ImmutableSet.<Holiday>of(holiday);
  }

  @Override
  public Collection<Holiday> get(Currency currency) {
    SimpleHoliday holiday = new SimpleHoliday(_nonWorkingDay.keySet());
    holiday.setType(HolidayType.CURRENCY);
    holiday.setCurrency(Currency.USD);
    return ImmutableSet.<Holiday>of(holiday);
  }
}
