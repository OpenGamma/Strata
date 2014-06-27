/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectMetaProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Test helper.
 * <p>
 * Provides additional classes to help with testing.
 */
public class TestHelper {

  /**
   * Asserts that the lambda-based code throws the specified exception.
   * <p>
   * For example:
   * <pre>
   *  assertThrows(() -> bean.property(""), NoSuchElementException.class);
   * </pre>
   * 
   * @param runner  the lambda containing the code to test, not null
   * @param expected  the expected exception, not null
   */
  public static void assertThrows(AssertRunnable runner, Class<? extends Throwable> expected) {
    assertNotNull(runner, "assertThrows() called with null AssertRunnable");
    assertNotNull(expected, "assertThrows() called with null expected Class");
    try {
      runner.run();
      fail("Expected " + expected.getSimpleName() + " but code succeeded normally");
    } catch (Throwable ex) {
      if (expected.isInstance(ex)) {
        return;
      }
      fail("Expected " + expected.getSimpleName() + " but received " + ex.getClass().getSimpleName());
    }
  }

  /**
   * Ignore any exception thrown by the lambda-based code.
   * <p>
   * For example:
   * <pre>
   *  ignoreThrows(() -> bean.property(""));
   * </pre>
   * 
   * @param runner  the lambda containing the code to test, not null
   */
  public static void ignoreThrows(AssertRunnable runner) {
    assertNotNull(runner, "assertThrows() called with null AssertRunnable");
    try {
      runner.run();
    } catch (Throwable ex) {
      // ignore
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Test a private no-arg constructor the primary purpose of increasing test coverage.
   * 
   * @param clazz  the class to test, not null
   */
  public static void coverPrivateConstructor(Class<?> clazz) {
    assertNotNull(clazz, "coverPrivateConstructor() called with null class");
    AtomicBoolean isPrivate = new AtomicBoolean(false);
    ignoreThrows(() -> {
      Constructor<?> con = clazz.getDeclaredConstructor();
      isPrivate.set(Modifier.isPrivate(con.getModifiers()));
      con.setAccessible(true);
      con.newInstance();
    });
    assertTrue(isPrivate.get(), "No-arg constructor must be private");
  }

  //-------------------------------------------------------------------------
  /**
   * Test an enum for the primary purpose of increasing test coverage.
   * 
   * @param clazz  the class to test, not null
   */
  public static <E extends Enum<E>>void coverEnum(Class<E> clazz) {
    assertNotNull(clazz, "coverEnum() called with null class");
    ignoreThrows(() -> clazz.getDeclaredMethod("values").invoke(null));
    for (E val : clazz.getEnumConstants()) {
      ignoreThrows(() -> clazz.getDeclaredMethod("valueOf", String.class).invoke(null, val.name()));
    }
    ignoreThrows(() -> clazz.getDeclaredMethod("valueOf", String.class).invoke(null, ""));
  }

  //-------------------------------------------------------------------------
  /**
   * Test a mutable bean for the primary purpose of increasing test coverage.
   * 
   * @param bean  the bean to test, not null
   */
  public static void coverMutableBean(Bean bean) {
    assertNotNull(bean, "coverImmutableBean() called with null bean");
    assertFalse(bean instanceof ImmutableBean);
    assertNotSame(JodaBeanUtils.clone(bean), bean);
    coverBean(bean);
  }

  /**
   * Test an immutable bean for the primary purpose of increasing test coverage.
   * 
   * @param bean  the bean to test, not null
   */
  public static void coverImmutableBean(ImmutableBean bean) {
    assertNotNull(bean, "coverImmutableBean() called with null bean");
    assertTrue(bean instanceof ImmutableBean);
    assertSame(JodaBeanUtils.clone(bean), bean);
    coverBean(bean);
  }

  // provide test coverage to all beans
  private static void coverBean(Bean bean) {
    coverProperties(bean);
    coverNonProperties(bean);
    coverEquals(bean);
  }

  // cover parts of a bean that are property-based
  private static void coverProperties(Bean bean) {
    MetaBean metaBean = bean.metaBean();
    Map<String, MetaProperty<?>> metaPropMap = metaBean.metaPropertyMap();
    assertNotNull(metaPropMap);
    assertEquals(metaBean.metaPropertyCount(), metaPropMap.size());
    for (MetaProperty<?> mp : metaBean.metaPropertyIterable()) {
      assertTrue(metaBean.metaPropertyExists(mp.name()));
      assertEquals(metaBean.metaProperty(mp.name()), mp);
      assertEquals(metaBean.metaProperty(new String(mp.name())), mp);
      assertEquals(metaPropMap.values().contains(mp), true);
      assertEquals(metaPropMap.keySet().contains(mp.name()), true);
      if (mp.style().isReadable()) {
        ignoreThrows(() -> mp.get(bean));
      } else {
        assertThrows(() -> mp.get(bean), UnsupportedOperationException.class);
      }
      if (mp.style().isWritable()) {
        ignoreThrows(() -> mp.set(bean, ""));
      } else {
        assertThrows(() -> mp.set(bean, ""), UnsupportedOperationException.class);
      }
      if (mp.style().isBuildable()) {
        metaBean.builder().get(mp);
        metaBean.builder().get(mp.name());
        for (Object setValue : sampleValues(mp)) {
          ignoreThrows(() -> metaBean.builder().set(mp, setValue));
        }
        for (Object setValue : sampleValues(mp)) {
          ignoreThrows(() -> metaBean.builder().set(mp.name(), setValue));
        }
        ignoreThrows(() -> metaBean.builder().setString(mp, ""));
        ignoreThrows(() -> metaBean.builder().setString(mp.name(), ""));
      }
      ignoreThrows(() -> {
        Method m = metaBean.getClass().getDeclaredMethod(mp.name());
        m.setAccessible(true);
        m.invoke(metaBean);
      });
      ignoreThrows(() -> {
        Method m = metaBean.getClass().getDeclaredMethod(
            "propertySet", Bean.class, String.class, Object.class, Boolean.TYPE);
        m.setAccessible(true);
        m.invoke(metaBean, bean, mp.name(), "", true);
      });
    }
  }

  // cover parts of a bean that are not property-based
  private static void coverNonProperties(Bean bean) {
    MetaBean metaBean = bean.metaBean();
    assertFalse(metaBean.metaPropertyExists(""));
    metaBean.builder().setAll(ImmutableMap.of());
    assertThrows(() -> metaBean.builder().get("foo_bar"), NoSuchElementException.class);
    assertThrows(() -> metaBean.builder().set("foo_bar", ""), NoSuchElementException.class);
    assertThrows(() -> metaBean.builder().setString("foo_bar", ""), NoSuchElementException.class);
    assertThrows(() -> metaBean.metaProperty("foo_bar"), NoSuchElementException.class);

    DirectMetaProperty<String> dummy = DirectMetaProperty.ofReadWrite(metaBean, "foo_bar", metaBean.beanType(), String.class);
    assertThrows(() -> dummy.get(bean), NoSuchElementException.class);
    assertThrows(() -> dummy.set(bean, ""), NoSuchElementException.class);
    assertThrows(() -> metaBean.builder().get(dummy), NoSuchElementException.class);
    assertThrows(() -> metaBean.builder().set(dummy, ""), NoSuchElementException.class);

    Set<String> propertyNameSet = bean.propertyNames();
    assertNotNull(propertyNameSet);
    for (String propertyName : propertyNameSet) {
      assertNotNull(bean.property(propertyName));
    }
    assertThrows(() -> bean.property(""), NoSuchElementException.class);

    ignoreThrows(() -> {
      Method m = bean.getClass().getDeclaredMethod("meta" + bean.getClass().getSimpleName(), Class.class);
      m.setAccessible(true);
      m.invoke(null, String.class);
    });
    
    assertNotNull(bean.toString());
    assertNotNull(metaBean.toString());
    assertNotNull(metaBean.builder().toString());
  }

  // different combinations of values to cover equals()
  private static void coverEquals(Bean bean) {
    // create beans with different data and compare each to the input bean
    // this will normally trigger each of the possible branches in equals
    List<MetaProperty<?>> buildableProps = bean.metaBean().metaPropertyMap().values().stream()
        .filter(mp -> mp.style().isBuildable())
        .collect(Collectors.toList());
    for (int i = 0; i < buildableProps.size(); i++) {
      try {
        BeanBuilder<? extends Bean> bld = bean.metaBean().builder();
        for (int j = 0; j < buildableProps.size(); j++) {
          MetaProperty<?> mp = buildableProps.get(j);
          if (j < i) {
            bld.set(mp, mp.get(bean));
          } else {
            List<?> samples = sampleValues(mp);
            bld.set(mp, samples.get(0));
          }
        }
        Bean built = bld.build();
        assertEquals(built, built);
        assertNotEquals(bean, built);
        assertEquals(built.hashCode(), built.hashCode());
      } catch (AssertionError ex) {
        throw ex;
      } catch (RuntimeException ex) {
        // ignore
      }
    }
    // cover the remaining equals edge cases
    assertFalse(bean.equals(null));
    assertFalse(bean.equals("NonBean"));
    assertTrue(bean.equals(bean));
    assertEquals(bean, JodaBeanUtils.cloneAlways(bean));
    assertTrue(bean.hashCode() == bean.hashCode());
  }

  // sample values for setters
  private static List<?> sampleValues(MetaProperty<?> mp) {
    Class<?> type = mp.propertyType();
    if (Enum.class.isAssignableFrom(type)) {
      return Arrays.asList(type.getEnumConstants());
    }
    return SAMPLES.getOrDefault(type, ImmutableList.of());
  }

  private static final Map<Class<?>, List<?>> SAMPLES =
      ImmutableMap.<Class<?>, List<?>>builder()
        .put(String.class, Arrays.asList("Hello", "Goodbye", " ", ""))
        .put(Byte.class, Arrays.asList((byte) 0, (byte) 1))
        .put(Byte.TYPE, Arrays.asList((byte) 0, (byte) 1))
        .put(Short.class, Arrays.asList((short) 0, (short) 1))
        .put(Short.TYPE, Arrays.asList((short) 0, (short) 1))
        .put(Integer.class, Arrays.asList((int) 0, (int) 1))
        .put(Integer.TYPE, Arrays.asList((int) 0, (int) 1))
        .put(Long.class, Arrays.asList((long) 0, (long) 1))
        .put(Long.TYPE, Arrays.asList((long) 0, (long) 1))
        .put(Float.class, Arrays.asList((float) 0, (float) 1))
        .put(Float.TYPE, Arrays.asList((float) 0, (float) 1))
        .put(Double.class, Arrays.asList((double) 0, (double) 1))
        .put(Double.TYPE, Arrays.asList((double) 0, (double) 1))
        .put(Character.class, Arrays.asList(' ', 'A', 'z'))
        .put(Character.TYPE, Arrays.asList(' ', 'A', 'z'))
        .put(Boolean.class, Arrays.asList(Boolean.TRUE, Boolean.FALSE))
        .put(Boolean.TYPE, Arrays.asList(Boolean.TRUE, Boolean.FALSE))
        .put(LocalDate.class, Arrays.asList(LocalDate.now(), LocalDate.of(2012, 6, 30)))
        .put(LocalTime.class, Arrays.asList(LocalTime.now(), LocalTime.of(11, 30)))
        .put(LocalDateTime.class, Arrays.asList(LocalDateTime.now(), LocalDateTime.of(2012, 6, 30, 11, 30)))
        .put(OffsetTime.class, Arrays.asList(OffsetTime.now(), OffsetTime.of(11, 30, 0, 0, ZoneOffset.ofHours(1))))
        .put(OffsetDateTime.class, Arrays.asList(
            OffsetDateTime.now(), OffsetDateTime.of(2012, 6, 30, 11, 30, 0, 0, ZoneOffset.ofHours(1))))
        .put(ZonedDateTime.class, Arrays.asList(
            ZonedDateTime.now(), ZonedDateTime.of(2012, 6, 30, 11, 30, 0, 0, ZoneId.systemDefault())))
        .put(Instant.class, Arrays.asList(Instant.now(), Instant.EPOCH))
        .put(Year.class, Arrays.asList(Year.now(), Year.of(2012)))
        .put(YearMonth.class, Arrays.asList(YearMonth.now(), YearMonth.of(2012, 6)))
        .put(MonthDay.class, Arrays.asList(MonthDay.now(), MonthDay.of(12, 25)))
        .put(Month.class, Arrays.asList(Month.JULY, Month.DECEMBER))
        .put(DayOfWeek.class, Arrays.asList(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
        .put(URI.class, Arrays.asList(URI.create("http://www.opengamma.com"), URI.create("http://www.joda.org")))
        .put(Class.class, Arrays.asList(Throwable.class, RuntimeException.class, String.class))
        .put(Object.class, Arrays.asList("", 6))
        .put(Collection.class, Arrays.asList(new ArrayList<String>()))
        .put(List.class, Arrays.asList(new ArrayList<String>()))
        .put(Set.class, Arrays.asList(new HashSet<String>()))
        .put(SortedSet.class, Arrays.asList(new TreeSet<String>()))
        .put(ImmutableList.class, Arrays.asList(ImmutableList.<String>of()))
        .put(ImmutableSet.class, Arrays.asList(ImmutableSet.<String>of()))
        .put(ImmutableSortedSet.class, Arrays.asList(ImmutableSortedSet.<String>naturalOrder()))
        .build();

}
