/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.joda.beans.BeanBuilder;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.BufferingBeanBuilder;
import org.joda.beans.impl.StandaloneMetaProperty;
import org.joda.beans.ser.DefaultDeserializer;
import org.joda.beans.ser.SerDeserializer;

/**
 * Deserialize {@code ImmutableHolidayCalendar} handling old format.
 */
final class ImmutableHolidayCalendarDeserializer extends DefaultDeserializer {

  /** Singleton instance. */
  public static final SerDeserializer INSTANCE = new ImmutableHolidayCalendarDeserializer();

  private static final MetaProperty<HolidayCalendarId> ID = ImmutableHolidayCalendar.meta().id();
  private static final MetaProperty<Integer> WEEKENDS = ImmutableHolidayCalendar.meta().weekends();
  private static final MetaProperty<Integer> START_YEAR = ImmutableHolidayCalendar.meta().startYear();
  private static final MetaProperty<int[]> LOOKUP = ImmutableHolidayCalendar.meta().lookup();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final MetaProperty<Set<String>> HOLIDAYS =
      (MetaProperty) StandaloneMetaProperty.of("holidays", ImmutableHolidayCalendar.meta(), Set.class);
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final MetaProperty<Set<String>> WEEKEND_DAYS =
      (MetaProperty) StandaloneMetaProperty.of("weekendDays", ImmutableHolidayCalendar.meta(), Set.class);

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   */
  private ImmutableHolidayCalendarDeserializer() {
  }

  @Override
  public BeanBuilder<?> createBuilder(Class<?> beanType, MetaBean metaBean) {
    return BufferingBeanBuilder.of(metaBean);
  }

  @Override
  public MetaProperty<?> findMetaProperty(Class<?> beanType, MetaBean metaBean, String propertyName) {
    try {
      return metaBean.metaProperty(propertyName);
    } catch (NoSuchElementException ex) {
      if (HOLIDAYS.name().equals(propertyName)) {
        return HOLIDAYS;
      }
      if (WEEKEND_DAYS.name().equals(propertyName)) {
        return WEEKEND_DAYS;
      }
      throw ex;
    }
  }

  @Override
  public Object build(Class<?> beanType, BeanBuilder<?> builder) {
    ConcurrentMap<MetaProperty<?>, Object> buffer = ((BufferingBeanBuilder<?>) builder).getBuffer();
    HolidayCalendarId id = builder.get(ID);
    if (buffer.containsKey(HOLIDAYS) && buffer.containsKey(WEEKEND_DAYS)) {
      Set<String> holidays = builder.get(HOLIDAYS);
      Set<String> weekendDays = builder.get(WEEKEND_DAYS);
      return ImmutableHolidayCalendar.of(
          id,
          holidays.stream().map(LocalDate::parse).collect(toImmutableSet()),
          weekendDays.stream().map(DayOfWeek::valueOf).collect(toImmutableSet()));
    } else {
      int weekends = builder.get(WEEKENDS);
      int startYear = builder.get(START_YEAR);
      int[] lookup = builder.get(LOOKUP);
      return new ImmutableHolidayCalendar(id, weekends, startYear, lookup, false);
    }
  }

}
