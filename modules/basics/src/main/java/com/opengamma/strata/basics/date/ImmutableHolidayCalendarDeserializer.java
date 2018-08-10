/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

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

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.date.ImmutableHolidayCalendar.Meta;

/**
 * Deserialize {@code ImmutableHolidayCalendar} handling old format.
 */
final class ImmutableHolidayCalendarDeserializer extends DefaultDeserializer {

  private static final Meta META_BEAN = ImmutableHolidayCalendar.meta();
  private static final MetaProperty<HolidayCalendarId> ID = META_BEAN.id();
  private static final MetaProperty<Integer> WEEKENDS = META_BEAN.weekends();
  private static final MetaProperty<Integer> START_YEAR = META_BEAN.startYear();
  private static final MetaProperty<int[]> LOOKUP = META_BEAN.lookup();

  @SuppressWarnings({"rawtypes", "unchecked", "serial"})
  private static final MetaProperty<Set<LocalDate>> HOLIDAYS =
      (MetaProperty) StandaloneMetaProperty.of(
          "holidays", META_BEAN, Set.class, new TypeToken<Set<LocalDate>>() {}.getType());
  @SuppressWarnings({"rawtypes", "unchecked", "serial"})
  private static final MetaProperty<Set<DayOfWeek>> WEEKEND_DAYS =
      (MetaProperty) StandaloneMetaProperty.of(
          "weekendDays", META_BEAN, Set.class, new TypeToken<Set<DayOfWeek>>() {}.getType());

  //-------------------------------------------------------------------------
  // restricted constructor
  ImmutableHolidayCalendarDeserializer() {
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
      Set<LocalDate> holidays = builder.get(HOLIDAYS);
      Set<DayOfWeek> weekendDays = builder.get(WEEKEND_DAYS);
      return ImmutableHolidayCalendar.of(id, holidays, weekendDays);
    } else {
      int weekends = builder.get(WEEKENDS);
      int startYear = builder.get(START_YEAR);
      int[] lookup = builder.get(LOOKUP);
      return new ImmutableHolidayCalendar(id, weekends, startYear, lookup, false);
    }
  }

}
