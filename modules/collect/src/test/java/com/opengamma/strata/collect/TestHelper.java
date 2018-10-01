/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.test.BeanAssert;
import org.joda.beans.test.JodaBeanTests;
import org.joda.convert.StringConvert;

import com.google.common.collect.ImmutableList;

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
   * Creates an empty {@code ImmutableList}, intended for static import.
   * 
   * @return the list
   */
  public static <T> ImmutableList<T> list() {
    return ImmutableList.of();
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param item0  the item
   * @return the list
   */
  public static <T> ImmutableList<T> list(T item0) {
    return ImmutableList.of(item0);
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param item0  the item
   * @param item1  the item
   * @return the list
   */
  public static <T> ImmutableList<T> list(T item0, T item1) {
    return ImmutableList.of(item0, item1);
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param item0  the item
   * @param item1  the item
   * @param item2  the item
   * @return the list
   */
  public static <T> ImmutableList<T> list(T item0, T item1, T item2) {
    return ImmutableList.of(item0, item1, item2);
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param item0  the item
   * @param item1  the item
   * @param item2  the item
   * @param item3  the item
   * @return the list
   */
  public static <T> ImmutableList<T> list(T item0, T item1, T item2, T item3) {
    return ImmutableList.of(item0, item1, item2, item3);
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param item0  the item
   * @param item1  the item
   * @param item2  the item
   * @param item3  the item
   * @param item4  the item
   * @return the list
   */
  public static <T> ImmutableList<T> list(T item0, T item1, T item2, T item3, T item4) {
    return ImmutableList.of(item0, item1, item2, item3, item4);
  }

  /**
   * Creates an {@code ImmutableList}, intended for static import.
   * 
   * @param list  the list
   * @return the list
   */
  @SafeVarargs
  public static <T> ImmutableList<T> list(T... list) {
    return ImmutableList.copyOf(list);
  }

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
    JodaBeanTests.coverMutableBean(bean);
  }

  /**
   * Test an immutable bean for the primary purpose of increasing test coverage.
   * 
   * @param bean  the bean to test
   */
  public static void coverImmutableBean(ImmutableBean bean) {
    JodaBeanTests.coverImmutableBean(bean);
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
    JodaBeanTests.coverBeanEquals(bean1, bean2);
  }

}
