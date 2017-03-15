/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.naming.NamingException;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.function.CheckedRunnable;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * Test Unchecked.
 */
@Test
public class UncheckedTest {

  //-------------------------------------------------------------------------
  public void test_wrap_runnable1() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedRunnable) () -> {
        throw new IOException();
      });
      fail();
    } catch (UncheckedIOException ex) {
      // success
    }
  }

  public void test_wrap_runnable2() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedRunnable) () -> {
        throw new Exception();
      });
      fail();
    } catch (RuntimeException ex) {
      // success
    }
  }

  //-------------------------------------------------------------------------
  public void test_wrap_supplier() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedSupplier<String>) () -> {
        throw new IOException();
      });
      fail();
    } catch (UncheckedIOException ex) {
      // success
    }
  }

  public void test_wrap_supplier2() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedSupplier<String>) () -> {
        throw new Exception();
      });
      fail();
    } catch (RuntimeException ex) {
      // success
    }
  }

  //-------------------------------------------------------------------------
  public void test_runnable_fail1() {
    Runnable a = Unchecked.runnable(() -> {
      throw new IOException();
    });
    assertThrows(() -> a.run(), UncheckedIOException.class);
  }

  public void test_runnable_fail2() {
    Runnable a = Unchecked.runnable(() -> {
      throw new Exception();
    });
    assertThrows(() -> a.run(), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_function_success() {
    Function<String, String> a = Unchecked.function((t) -> t);
    assertEquals(a.apply("A"), "A");
  }

  public void test_function_fail1() {
    Function<String, String> a = Unchecked.function((t) -> {
      throw new IOException();
    });
    assertThrows(() -> a.apply("A"), UncheckedIOException.class);
  }

  public void test_function_fail2() {
    Function<String, String> a = Unchecked.function((t) -> {
      throw new Exception();
    });
    assertThrows(() -> a.apply("A"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_biFunction_success() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> t + u);
    assertEquals(a.apply("A", "B"), "AB");
  }

  public void test_biFunction_fail1() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> {
      throw new IOException();
    });
    assertThrows(() -> a.apply("A", "B"), UncheckedIOException.class);
  }

  public void test_biFunction_fail2() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> {
      throw new Exception();
    });
    assertThrows(() -> a.apply("A", "B"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_unaryOperator_success() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> t);
    assertEquals(a.apply("A"), "A");
  }

  public void test_unaryOperator_fail1() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> {
      throw new IOException();
    });
    assertThrows(() -> a.apply("A"), UncheckedIOException.class);
  }

  public void test_unaryOperator_fail2() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> {
      throw new Exception();
    });
    assertThrows(() -> a.apply("A"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_binaryOperator_success() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> t + u);
    assertEquals(a.apply("A", "B"), "AB");
  }

  public void test_binaryOperator_fail1() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> {
      throw new IOException();
    });
    assertThrows(() -> a.apply("A", "B"), UncheckedIOException.class);
  }

  public void test_binaryOperator_fail2() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> {
      throw new Exception();
    });
    assertThrows(() -> a.apply("A", "B"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_predicate_success() {
    Predicate<String> a = Unchecked.predicate((t) -> true);
    assertEquals(a.test("A"), true);
  }

  public void test_predicate_fail1() {
    Predicate<String> a = Unchecked.predicate((t) -> {
      throw new IOException();
    });
    assertThrows(() -> a.test("A"), UncheckedIOException.class);
  }

  public void test_predicate_fail2() {
    Predicate<String> a = Unchecked.predicate((t) -> {
      throw new Exception();
    });
    assertThrows(() -> a.test("A"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_biPredicate_success() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> true);
    assertEquals(a.test("A", "B"), true);
  }

  public void test_biPredicate_fail1() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> {
      throw new IOException();
    });
    assertThrows(() -> a.test("A", "B"), UncheckedIOException.class);
  }

  public void test_biPredicate_fail2() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> {
      throw new Exception();
    });
    assertThrows(() -> a.test("A", "B"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_consumer_success() {
    Consumer<String> a = Unchecked.consumer((t) -> {});
    a.accept("A");
  }

  public void test_consumer_fail1() {
    Consumer<String> a = Unchecked.consumer((t) -> {
      throw new IOException();
    });
    assertThrows(() -> a.accept("A"), UncheckedIOException.class);
  }

  public void test_consumer_fail2() {
    Consumer<String> a = Unchecked.consumer((t) -> {
      throw new Exception();
    });
    assertThrows(() -> a.accept("A"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_biConsumer_success() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {});
    a.accept("A", "B");
  }

  public void test_biConsumer_fail1() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {
      throw new IOException();
    });
    assertThrows(() -> a.accept("A", "B"), UncheckedIOException.class);
  }

  public void test_biConsumer_fail2() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {
      throw new Exception();
    });
    assertThrows(() -> a.accept("A", "B"), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_supplier_success() {
    Supplier<String> a = Unchecked.supplier(() -> "A");
    assertEquals(a.get(), "A");
  }

  public void test_supplier_fail1() {
    Supplier<String> a = Unchecked.supplier(() -> {
      throw new IOException();
    });
    assertThrows(() -> a.get(), UncheckedIOException.class);
  }

  public void test_supplier_fail2() {
    Supplier<String> a = Unchecked.supplier(() -> {
      throw new Exception();
    });
    assertThrows(() -> a.get(), RuntimeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_validUtilityClass() {
    assertUtilityClass(Unchecked.class);
  }

  public void test_propagate() {
    Error error = new Error("a");
    IllegalArgumentException argEx = new IllegalArgumentException("b");
    IOException ioEx = new IOException("c");
    NamingException namingEx = new NamingException("d");

    // use old-style try-catch to ensure test really working
    try {
      Unchecked.propagate(error);
      fail();
    } catch (Error ex) {
      assertSame(ex, error);
    }
    try {
      Unchecked.propagate(argEx);
      fail();
    } catch (IllegalArgumentException ex) {
      assertSame(ex, argEx);
    }
    try {
      Unchecked.propagate(ioEx);
      fail();
    } catch (UncheckedIOException ex) {
      assertEquals(ex.getClass(), UncheckedIOException.class);
      assertSame(ex.getCause(), ioEx);
    }
    try {
      Unchecked.propagate(namingEx);
      fail();
    } catch (RuntimeException ex) {
      assertEquals(ex.getClass(), RuntimeException.class);
      assertSame(ex.getCause(), namingEx);
    }

    try {
      Unchecked.propagate(new InvocationTargetException(error));
      fail();
    } catch (Error ex) {
      assertSame(ex, error);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(argEx));
      fail();
    } catch (IllegalArgumentException ex) {
      assertSame(ex, argEx);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(ioEx));
      fail();
    } catch (UncheckedIOException ex) {
      assertEquals(ex.getClass(), UncheckedIOException.class);
      assertSame(ex.getCause(), ioEx);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(namingEx));
      fail();
    } catch (RuntimeException ex) {
      assertEquals(ex.getClass(), RuntimeException.class);
      assertSame(ex.getCause(), namingEx);
    }
  }

}
