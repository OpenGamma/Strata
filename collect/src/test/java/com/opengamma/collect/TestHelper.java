/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectMetaProperty;

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
    coverBean(bean);
  }

  private static void coverBean(Bean bean) {
    MetaBean metaBean = bean.metaBean();
    assertNotNull(metaBean);

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
        for (Object setValue : SETTABLES) {
          ignoreThrows(() -> metaBean.builder().set(mp, setValue));
        }
        for (Object setValue : SETTABLES) {
          ignoreThrows(() -> metaBean.builder().set(mp.name(), setValue));
        }
        ignoreThrows(() -> metaBean.builder().setString(mp, ""));
        ignoreThrows(() -> metaBean.builder().setString(mp.name(), ""));
      }
      ignoreThrows(() -> metaBean.getClass().getDeclaredMethod(mp.name()).invoke(metaBean));
      ignoreThrows(() -> {
        Method m = metaBean.getClass().getDeclaredMethod(
            "propertySet", Bean.class, String.class, Object.class, Boolean.TYPE);
        m.setAccessible(true);
        m.invoke(metaBean, bean, mp.name(), "", true);
      });
    }
    assertFalse(metaBean.metaPropertyExists(""));
    assertThrows(() -> metaBean.builder().set("foo_bar", ""), NoSuchElementException.class);
    assertThrows(() -> metaBean.builder().setString("foo_bar", ""), NoSuchElementException.class);
    assertThrows(() -> metaBean.metaProperty("foo_bar"), NoSuchElementException.class);

    DirectMetaProperty<String> dummy = DirectMetaProperty.ofReadWrite(metaBean, "foo_bar", metaBean.beanType(), String.class);
    assertThrows(() -> dummy.get(bean), NoSuchElementException.class);
    assertThrows(() -> dummy.set(bean, ""), NoSuchElementException.class);

    Set<String> propertyNameSet = bean.propertyNames();
    assertNotNull(propertyNameSet);
    for (String propertyName : propertyNameSet) {
      assertNotNull(bean.property(propertyName));
    }
    assertThrows(() -> bean.property(""), NoSuchElementException.class);

    ignoreThrows(() -> metaBean.builder().build());
  }

  private static final Object[] SETTABLES = new Object[] {
    "", Byte.valueOf((byte) 0), Short.valueOf((short) 0), Integer.valueOf((int) 0), Long.valueOf((long) 0),
    Float.valueOf((float) 0), Double.valueOf((double) 0), Character.valueOf(' '), Boolean.TRUE,
    LocalDate.now(), LocalTime.now(), LocalDateTime.now(), OffsetTime.now(), OffsetDateTime.now(), ZonedDateTime.now(),
    Instant.now(), Year.now(), YearMonth.now(), MonthDay.now(), Month.JULY, DayOfWeek.MONDAY,
  };

}
