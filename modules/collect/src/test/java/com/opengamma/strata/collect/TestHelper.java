/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.StandaloneMetaProperty;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.test.BeanAssert;
import org.joda.beans.test.JodaBeanTests;
import org.joda.convert.StringConvert;

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
   * UTF-8 encoding name.
   */
  private static final String UTF_8 = "UTF-8";

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code LocalDate}, intended for static import.
   * 
   * @param year  the year
   * @param month  the month
   * @param dayOfMonth  the day of month
   * @return the date
   */
  public static LocalDate date(int year, int month, int dayOfMonth) {
    return LocalDate.of(year, month, dayOfMonth);
  }

  /**
   * Creates a {@code LocalDate}, intended for static import.
   * 
   * @param year  the year
   * @param month  the month
   * @param dayOfMonth  the day of month
   * @return the date
   */
  public static LocalDate date(int year, Month month, int dayOfMonth) {
    return LocalDate.of(year, month, dayOfMonth);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code ZonedDateTime} from the date.
   * <p>
   * The time is start of day and the zone is UTC.
   * 
   * @param year  the year
   * @param month  the month
   * @param dayOfMonth  the day of month
   * @return the date-time, representing the date at midnight UTC
   */
  public static ZonedDateTime dateUtc(int year, int month, int dayOfMonth) {
    return LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneOffset.UTC);
  }

  //-------------------------------------------------------------------------
  /**
   * Asserts that two beans are equal.
   * Provides better error messages than a normal {@code assertEquals} comparison.
   * 
   * @param actual  the actual bean under test
   * @param expected  the expected bean
   */
  public static void assertEqualsBean(Bean actual, Bean expected) {
    BeanAssert.assertBeanEquals(expected, actual);
  }

  /**
   * Asserts that two beans are equal.
   * Provides better error messages than a normal {@code assertEquals} comparison.
   * <p>
   * This provides extra detail used when debugging an issue.
   * Normal use should be to call {@link #assertEqualsBean(Bean, Bean)}.
   * 
   * @param actual  the actual bean under test
   * @param expected  the expected bean
   */
  public static void assertEqualsBeanDetailed(Bean actual, Bean expected) {
    BeanAssert.assertBeanEqualsFullDetail(expected, actual);
  }

  //-------------------------------------------------------------------------
  /**
   * Asserts that the object can be serialized and deserialized to an equal form.
   * 
   * @param base  the object to be tested
   */
  public static void assertSerialization(Object base) {
    assertNotNull(base);
    try {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
          oos.writeObject(base);
          oos.close();
          try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              assertEquals(ois.readObject(), base);
            }
          }
        }
      }
    } catch (IOException | ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Asserts that the object can be serialized and deserialized via a string using Joda-Convert.
   * 
   * @param <T>  the type
   * @param cls  the effective type
   * @param base  the object to be tested
   */
  public static <T> void assertJodaConvert(Class<T> cls, Object base) {
    assertNotNull(base);
    StringConvert convert = StringConvert.create();
    String str = convert.convertToString(base);
    T result = convert.convertFromString(cls, str);
    assertEquals(result, base);
  }

  //-------------------------------------------------------------------------
  /**
   * Asserts that the lambda-based code throws the specified exception.
   * <p>
   * For example:
   * <pre>
   *  assertThrows(() -> bean.property(""), NoSuchElementException.class);
   * </pre>
   * 
   * @param runner  the lambda containing the code to test
   * @param expected  the expected exception
   */
  public static void assertThrows(AssertRunnable runner, Class<? extends Throwable> expected) {
    assertThrowsImpl(runner, expected, null);
  }

  /**
   * Asserts that the lambda-based code throws the specified exception
   * and that the exception message matches the supplied regular
   * expression.
   * <p>
   * For example:
   * <pre>
   *  assertThrows(() -> bean.property(""), NoSuchElementException.class, "Unknown property.*");
   * </pre>
   *
   * @param runner  the lambda containing the code to test
   * @param expected  the expected exception
   * @param regex  the regex that the exception message is expected to match
   */
  public static void assertThrows(AssertRunnable runner, Class<? extends Throwable> expected, String regex) {
    assertNotNull(regex, "assertThrows() called with null regex");
    assertThrowsImpl(runner, expected, regex);
  }

  private static void assertThrowsImpl(
      AssertRunnable runner,
      Class<? extends Throwable> expected,
      String regex) {

    assertNotNull(runner, "assertThrows() called with null AssertRunnable");
    assertNotNull(expected, "assertThrows() called with null expected Class");

    try {
      runner.run();
      fail("Expected " + expected.getSimpleName() + " but code succeeded normally");
    } catch (AssertionError ex) {
      throw ex;
    } catch (Throwable ex) {
      if (expected.isInstance(ex)) {
        String message = ex.getMessage();
        if (regex == null || message.matches(regex)) {
          return;
        } else {
          fail("Expected exception message to match: [" + regex + "] but received: " + message);
        }
      }
      fail("Expected " + expected.getSimpleName() + " but received " + ex.getClass().getSimpleName(), ex);
    }
  }

  /**
   * Asserts that the lambda-based code throws an {@code RuntimeException}.
   * <p>
   * For example:
   * <pre>
   *  assertThrowsRuntime(() -> new Foo(null));
   * </pre>
   * 
   * @param runner  the lambda containing the code to test
   */
  public static void assertThrowsRuntime(AssertRunnable runner) {
    assertThrows(runner, RuntimeException.class);
  }

  /**
   * Asserts that the lambda-based code throws an {@code IllegalArgumentException}.
   * <p>
   * For example:
   * <pre>
   *  assertThrowsIllegalArg(() -> new Foo(null));
   * </pre>
   * 
   * @param runner  the lambda containing the code to test
   */
  public static void assertThrowsIllegalArg(AssertRunnable runner) {
    assertThrows(runner, IllegalArgumentException.class);
  }

  /**
   * Asserts that the lambda-based code throws an {@code IllegalArgumentException} and checks the message
   * matches an regex.
   * <p>
   * For example:
   * <pre>
   *  assertThrowsIllegalArg(() -> new Foo(null), "Foo constructor argument must not be null");
   * </pre>
   *
   * @param runner  the lambda containing the code to test
   * @param regex  regular expression that must match the exception message
   */
  public static void assertThrowsIllegalArg(AssertRunnable runner, String regex) {
    assertThrows(runner, IllegalArgumentException.class, regex);
  }

  /**
   * Asserts that the lambda-based code throws an exception
   * and that the cause of the exception is the supplied cause.
   * <p>
   * For example:
   * <pre>
   *  assertThrowsWithCause(() ->
   *    executeSql("INSERT DATA THAT ALREADY EXISTS"), SQLIntegrityConstraintViolationException.class);
   * </pre>
   *
   * @param runner  the lambda containing the code to test
   * @param cause  the expected cause of the exception thrown
   */
  public static void assertThrowsWithCause(
      AssertRunnable runner, Class<? extends Throwable> cause) {
    assertNotNull(runner, "assertThrowsWithCause() called with null AssertRunnable");
    assertNotNull(cause, "assertThrowsWithCause() called with null expected cause");

    try {
      runner.run();
      fail("Expected " + cause.getSimpleName() + " but code succeeded normally");
    } catch (AssertionError ex) {
      throw ex;
    } catch (Throwable ex) {
      Throwable ex2 = ex;
      while (ex2 != null && !cause.isInstance(ex2)) {
        ex2 = ex2.getCause();
      }

      if (ex2 == null) {
        fail("Expected cause of exception to be: " + cause.getSimpleName() + " but got different exception", ex);
      }
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
   * @param runner  the lambda containing the code to test
   */
  public static void ignoreThrows(AssertRunnable runner) {
    assertNotNull(runner, "ignoreThrows() called with null AssertRunnable");
    try {
      runner.run();
    } catch (Throwable ex) {
      // ignore
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Capture system out for testing.
   * <p>
   * This returns the output from calls to {@code System.out}.
   * This is thread-safe, providing that no other utility alters system out.
   * <p>
   * For example:
   * <pre>
   *  String sysOut = captureSystemOut(() -> myCode);
   * </pre>
   * 
   * @param runner  the lambda containing the code to test
   * @return the captured output
   */
  public static synchronized String caputureSystemOut(Runnable runner) {
    // it would be possible to use some form of thread-local PrintStream to increase concurrency,
    // but that should be done only if synchronized is insufficient
    assertNotNull(runner, "caputureSystemOut() called with null Runnable");
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    PrintStream ps = Unchecked.wrap(() -> new PrintStream(baos, false, UTF_8));
    PrintStream old = System.out;
    try {
      System.setOut(ps);
      runner.run();
      System.out.flush();
    } finally {
      System.setOut(old);
    }
    return Unchecked.wrap(() -> baos.toString(UTF_8));
  }

  /**
   * Capture system err for testing.
   * <p>
   * This returns the output from calls to {@code System.err}.
   * This is thread-safe, providing that no other utility alters system out.
   * <p>
   * For example:
   * <pre>
   *  String sysErr = captureSystemErr(() -> myCode);
   * </pre>
   * 
   * @param runner  the lambda containing the code to test
   * @return the captured output
   */
  public static synchronized String caputureSystemErr(Runnable runner) {
    // it would be possible to use some form of thread-local PrintStream to increase concurrency,
    // but that should be done only if synchronized is insufficient
    assertNotNull(runner, "caputureSystemErr() called with null Runnable");
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    PrintStream ps = Unchecked.wrap(() -> new PrintStream(baos, false, UTF_8));
    PrintStream old = System.err;
    try {
      System.setErr(ps);
      runner.run();
      System.err.flush();
    } finally {
      System.setErr(old);
    }
    return Unchecked.wrap(() -> baos.toString(UTF_8));
  }

  /**
   * Capture log for testing.
   * <p>
   * This returns the output from calls to the java logger.
   * This is thread-safe, providing that no other utility alters the logger.
   * <p>
   * For example:
   * <pre>
   *  String log = captureLog(Foo.class, () -> myCode);
   * </pre>
   * 
   * @param loggerClass  the class defining the logger to trap
   * @param runner  the lambda containing the code to test
   * @return the captured output
   */
  public static synchronized List<LogRecord> caputureLog(Class<?> loggerClass, Runnable runner) {
    assertNotNull(loggerClass, "caputureLog() called with null Class");
    assertNotNull(runner, "caputureLog() called with null Runnable");

    Logger logger = Logger.getLogger(loggerClass.getName());
    LogHandler handler = new LogHandler();
    try {
      handler.setLevel(Level.ALL);
      logger.setUseParentHandlers(false);
      logger.addHandler(handler);
      runner.run();
      return handler.records;
    } finally {
      logger.removeHandler(handler);
    }
  }

  private static class LogHandler extends Handler {
    List<LogRecord> records = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Asserts that a class is a well-defined utility class.
   * <p>
   * Must be final and with one zero-arg private constructor.
   * All public methods must be static.
   * 
   * @param clazz  the class to test
   */
  public static void assertUtilityClass(Class<?> clazz) {
    assertNotNull(clazz, "assertUtilityClass() called with null class");
    assertTrue(Modifier.isFinal(clazz.getModifiers()), "Utility class must be final");
    assertEquals(clazz.getDeclaredConstructors().length, 1, "Utility class must have one constructor");
    Constructor<?> con = clazz.getDeclaredConstructors()[0];
    assertEquals(con.getParameterTypes().length, 0, "Utility class must have zero-arg constructor");
    assertTrue(Modifier.isPrivate(con.getModifiers()), "Utility class must have private constructor");
    for (Method method : clazz.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())) {
        assertTrue(Modifier.isStatic(method.getModifiers()), "Utility class public methods must be static");
      }
    }
    // coverage
    ignoreThrows(() -> {
      con.setAccessible(true);
      con.newInstance();
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Test a private no-arg constructor the primary purpose of increasing test coverage.
   * 
   * @param clazz  the class to test
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
   * @param <E>  the enum type
   * @param clazz  the class to test
   */
  public static <E extends Enum<E>> void coverEnum(Class<E> clazz) {
    assertNotNull(clazz, "coverEnum() called with null class");
    ignoreThrows(() -> {
      Method method = clazz.getDeclaredMethod("values");
      method.setAccessible(true);
      method.invoke(null);
    });
    for (E val : clazz.getEnumConstants()) {
      ignoreThrows(() -> {
        Method method = clazz.getDeclaredMethod("valueOf", String.class);
        method.setAccessible(true);
        method.invoke(null, val.name());
      });
    }
    ignoreThrows(() -> {
      Method method = clazz.getDeclaredMethod("valueOf", String.class);
      method.setAccessible(true);
      method.invoke(null, "");
    });
    ignoreThrows(() -> {
      Method method = clazz.getDeclaredMethod("valueOf", String.class);
      method.setAccessible(true);
      method.invoke(null, (Object) null);
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Test a mutable bean for the primary purpose of increasing test coverage.
   * 
   * @param bean  the bean to test
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
   * @param bean  the bean to test
   */
  public static void coverImmutableBean(ImmutableBean bean) {
    assertNotNull(bean, "coverImmutableBean() called with null bean");
    assertSame(JodaBeanUtils.clone(bean), bean);
    coverBean(bean);
  }

  /**
   * Test a bean equals method for the primary purpose of increasing test coverage.
   * <p>
   * The two beans passed in should contain a different value for each property.
   * The method creates a cross-product to ensure test coverage of equals.
   * 
   * @param bean1  the first bean to test
   * @param bean2  the second bean to test
   */
  public static void coverBeanEquals(Bean bean1, Bean bean2) {
    assertNotNull(bean1, "coverBeanEquals() called with null bean");
    assertNotNull(bean2, "coverBeanEquals() called with null bean");
    assertFalse(bean1.equals(null));
    assertFalse(bean1.equals("NonBean"));
    assertTrue(bean1.equals(bean1));
    assertTrue(bean2.equals(bean2));
    ignoreThrows(() -> assertEquals(bean1, JodaBeanUtils.cloneAlways(bean1)));
    ignoreThrows(() -> assertEquals(bean2, JodaBeanUtils.cloneAlways(bean2)));
    assertTrue(bean1.hashCode() == bean1.hashCode());
    assertTrue(bean2.hashCode() == bean2.hashCode());
    if (bean1.equals(bean2) || bean1.getClass() != bean2.getClass()) {
      return;
    }
    MetaBean metaBean = bean1.metaBean();
    List<MetaProperty<?>> buildableProps = metaBean.metaPropertyMap().values().stream()
        .filter(mp -> mp.style().isBuildable())
        .collect(Collectors.toList());
    Set<Bean> builtBeansSet = new HashSet<>();
    builtBeansSet.add(bean1);
    builtBeansSet.add(bean2);
    for (int i = 0; i < buildableProps.size(); i++) {
      for (int j = 0; j < 2; j++) {
        try {
          BeanBuilder<? extends Bean> bld = metaBean.builder();
          for (int k = 0; k < buildableProps.size(); k++) {
            MetaProperty<?> mp = buildableProps.get(k);
            if (j == 0) {
              bld.set(mp, mp.get(k < i ? bean1 : bean2));
            } else {
              bld.set(mp, mp.get(i <= k ? bean1 : bean2));
            }
          }
          builtBeansSet.add(bld.build());
        } catch (RuntimeException ex) {
          // ignore
        }
      }
    }
    List<Bean> builtBeansList = new ArrayList<>(builtBeansSet);
    for (int i = 0; i < builtBeansList.size() - 1; i++) {
      for (int j = i + 1; j < builtBeansList.size(); j++) {
        builtBeansList.get(i).equals(builtBeansList.get(j));
      }
    }
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
      // Ensure we don't use interned value
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
        ignoreThrows(() -> metaBean.builder().get(mp));
        ignoreThrows(() -> metaBean.builder().get(mp.name()));
        for (Object setValue : sampleValues(mp)) {
          ignoreThrows(() -> metaBean.builder().set(mp, setValue));
        }
        for (Object setValue : sampleValues(mp)) {
          ignoreThrows(() -> metaBean.builder().set(mp.name(), setValue));
        }
        for (String setStr : sampleStrings(mp)) {
          ignoreThrows(() -> metaBean.builder().setString(mp, setStr));
        }
        for (String setStr : sampleStrings(mp)) {
          ignoreThrows(() -> metaBean.builder().setString(mp.name(), setStr));
        }
        ignoreThrows(() -> metaBean.builder().setString(mp, JodaBeanTests.TEST_COVERAGE_STRING));
        ignoreThrows(() -> metaBean.builder().setString(mp.name(), JodaBeanTests.TEST_COVERAGE_STRING));
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
    ignoreThrows(() -> {
      Method m = metaBean.getClass().getDeclaredMethod(
          "propertyGet", Bean.class, String.class, Boolean.TYPE);
      m.setAccessible(true);
      m.invoke(metaBean, bean, "Not a real property name", true);
    });
    MetaProperty<String> fakeMetaProp = StandaloneMetaProperty.of("fake", metaBean, String.class);
    ignoreThrows(() -> metaBean.builder().set(fakeMetaProp, JodaBeanTests.TEST_COVERAGE_STRING));
    ignoreThrows(() -> metaBean.builder().setString(fakeMetaProp, JodaBeanTests.TEST_COVERAGE_STRING));
    ignoreThrows(() -> metaBean.builder().set(JodaBeanTests.TEST_COVERAGE_PROPERTY, JodaBeanTests.TEST_COVERAGE_STRING));
    ignoreThrows(() -> metaBean.builder().setString(JodaBeanTests.TEST_COVERAGE_PROPERTY, JodaBeanTests.TEST_COVERAGE_STRING));
    ignoreThrows(() -> bean.property(JodaBeanTests.TEST_COVERAGE_PROPERTY));
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

    if (metaBean instanceof DirectMetaBean) {
      DirectMetaProperty<String> dummy = DirectMetaProperty.ofReadWrite(metaBean, "foo_bar", metaBean.beanType(), String.class);
      assertThrows(() -> dummy.get(bean), NoSuchElementException.class);
      assertThrows(() -> dummy.set(bean, ""), NoSuchElementException.class);
      assertThrows(() -> dummy.setString(bean, ""), NoSuchElementException.class);
      assertThrows(() -> metaBean.builder().get(dummy), NoSuchElementException.class);
      assertThrows(() -> metaBean.builder().set(dummy, ""), NoSuchElementException.class);
    }

    Set<String> propertyNameSet = bean.propertyNames();
    assertNotNull(propertyNameSet);
    for (String propertyName : propertyNameSet) {
      assertNotNull(bean.property(propertyName));
    }
    assertThrows(() -> bean.property(""), NoSuchElementException.class);

    Class<? extends Bean> beanClass = bean.getClass();
    ignoreThrows(() -> {
      Method m = beanClass.getDeclaredMethod("meta");
      m.setAccessible(true);
      m.invoke(null);
    });
    ignoreThrows(() -> {
      Method m = beanClass.getDeclaredMethod("meta" + beanClass.getSimpleName(), Class.class);
      m.setAccessible(true);
      m.invoke(null, String.class);
    });
    ignoreThrows(() -> {
      Method m = beanClass.getDeclaredMethod("meta" + beanClass.getSimpleName(), Class.class, Class.class);
      m.setAccessible(true);
      m.invoke(null, String.class, String.class);
    });
    ignoreThrows(() -> {
      Method m = beanClass.getDeclaredMethod("meta" + beanClass.getSimpleName(), Class.class, Class.class, Class.class);
      m.setAccessible(true);
      m.invoke(null, String.class, String.class, String.class);
    });

    ignoreThrows(() -> {
      Method m = bean.getClass().getDeclaredMethod("builder");
      m.setAccessible(true);
      m.invoke(null);
    });
    ignoreThrows(() -> {
      Method m = bean.getClass().getDeclaredMethod("toBuilder");
      m.setAccessible(true);
      m.invoke(bean);
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
        coverBeanEquals(bean, built);
        assertEquals(built, built);
        assertEquals(built.hashCode(), built.hashCode());
      } catch (RuntimeException ex) {
        // ignore
      }
    }
    // cover the remaining equals edge cases
    assertFalse(bean.equals(null));
    assertFalse(bean.equals("NonBean"));
    assertTrue(bean.equals(bean));
    ignoreThrows(() -> assertEquals(bean, JodaBeanUtils.cloneAlways(bean)));
    assertTrue(bean.hashCode() == bean.hashCode());
  }

  // sample values for setters
  private static List<?> sampleValues(MetaProperty<?> mp) {
    Class<?> type = mp.propertyType();
    // enum constants
    if (Enum.class.isAssignableFrom(type)) {
      return Arrays.asList(type.getEnumConstants());
    }
    // lookup pre-canned samples
    List<?> sample = SAMPLES.get(type);
    if (sample != null) {
      return sample;
    }
    // find any potential declared constants, using some plural rules
    String typeName = type.getName();
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    builder.addAll(buildSampleConstants(type, type));
    ignoreThrows(() -> {
      // cat -> cats
      builder.addAll(buildSampleConstants(Class.forName(typeName + "s"), type));
    });
    ignoreThrows(() -> {
      // dish -> dishes
      builder.addAll(buildSampleConstants(Class.forName(typeName + "es"), type));
    });
    ignoreThrows(() -> {
      // lady -> ladies
      builder.addAll(buildSampleConstants(Class.forName(typeName.substring(0, typeName.length() - 1) + "ies"), type));
    });
    ignoreThrows(() -> {
      // index -> indices
      builder.addAll(buildSampleConstants(Class.forName(typeName.substring(0, typeName.length() - 2) + "ices"), type));
    });
    // none
    return builder.build();
  }

  // adds sample constants to the 
  private static ImmutableList<Object> buildSampleConstants(Class<?> queryType, Class<?> targetType) {
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    for (Field field : queryType.getFields()) {
      if (field.getType() == targetType &&
          Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers()) &&
          field.isSynthetic() == false) {
        ignoreThrows(() -> builder.add(field.get(null)));
      }
    }
    return builder.build();
  }

  // sample strings for setters
  private static List<String> sampleStrings(MetaProperty<?> mp) {
    List<?> values = sampleValues(mp);
    List<String> strings = values.stream().map(Object::toString).collect(Collectors.toList());
    strings.add("");
    return strings;
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
          .put(LocalDate.class, Arrays.asList(LocalDate.now(ZoneOffset.UTC), LocalDate.of(2012, 6, 30)))
          .put(LocalTime.class, Arrays.asList(LocalTime.now(ZoneOffset.UTC), LocalTime.of(11, 30)))
          .put(LocalDateTime.class, Arrays.asList(LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.of(2012, 6, 30, 11, 30)))
          .put(OffsetTime.class, Arrays.asList(
              OffsetTime.now(ZoneOffset.UTC), OffsetTime.of(11, 30, 0, 0, ZoneOffset.ofHours(1))))
          .put(OffsetDateTime.class, Arrays.asList(
              OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.of(2012, 6, 30, 11, 30, 0, 0, ZoneOffset.ofHours(1))))
          .put(ZonedDateTime.class, Arrays.asList(
              ZonedDateTime.now(ZoneOffset.UTC), ZonedDateTime.of(2012, 6, 30, 11, 30, 0, 0, ZoneId.systemDefault())))
          .put(Instant.class, Arrays.asList(Instant.now(), Instant.EPOCH))
          .put(Year.class, Arrays.asList(Year.now(ZoneOffset.UTC), Year.of(2012)))
          .put(YearMonth.class, Arrays.asList(YearMonth.now(ZoneOffset.UTC), YearMonth.of(2012, 6)))
          .put(MonthDay.class, Arrays.asList(MonthDay.now(ZoneOffset.UTC), MonthDay.of(12, 25)))
          .put(Month.class, Arrays.asList(Month.JULY, Month.DECEMBER))
          .put(DayOfWeek.class, Arrays.asList(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
          .put(URI.class, Arrays.asList(URI.create("http://www.opengamma.com"), URI.create("http://www.joda.org")))
          .put(Class.class, Arrays.asList(Throwable.class, RuntimeException.class, String.class))
          .put(Object.class, Arrays.asList("", 6))
          .put(Collection.class, Arrays.asList(new ArrayList<>()))
          .put(List.class, Arrays.asList(new ArrayList<>()))
          .put(Set.class, Arrays.asList(new HashSet<>()))
          .put(SortedSet.class, Arrays.asList(new TreeSet<>()))
          .put(ImmutableList.class, Arrays.asList(ImmutableList.<String>of()))
          .put(ImmutableSet.class, Arrays.asList(ImmutableSet.<String>of()))
          .put(ImmutableSortedSet.class, Arrays.asList(ImmutableSortedSet.<String>naturalOrder()))
          .build();

}
