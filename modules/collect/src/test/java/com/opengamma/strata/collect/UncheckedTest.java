/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.function.CheckedRunnable;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * Test Unchecked.
 */
public class UncheckedTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_wrap_runnable1() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedRunnable) () -> {
        throw new IOException();
      });
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      // success
    }
  }

  @Test
  public void test_wrap_runnable2() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedRunnable) () -> {
        throw new Exception();
      });
      fail("Expected RuntimeException");
    } catch (RuntimeException ex) {
      // success
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_wrap_supplier() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedSupplier<String>) () -> {
        throw new IOException();
      });
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      // success
    }
  }

  @Test
  public void test_wrap_supplier2() {
    // cannot use assertThrows() here
    try {
      Unchecked.wrap((CheckedSupplier<String>) () -> {
        throw new Exception();
      });
      fail("Expected RuntimeException");
    } catch (RuntimeException ex) {
      // success
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_runnable_fail1() {
    Runnable a = Unchecked.runnable(() -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.run());
  }

  @Test
  public void test_runnable_fail2() {
    Runnable a = Unchecked.runnable(() -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.run());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_function_success() {
    Function<String, String> a = Unchecked.function((t) -> t);
    assertThat(a.apply("A")).isEqualTo("A");
  }

  @Test
  public void test_function_fail1() {
    Function<String, String> a = Unchecked.function((t) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.apply("A"));
  }

  @Test
  public void test_function_fail2() {
    Function<String, String> a = Unchecked.function((t) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.apply("A"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_biFunction_success() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> t + u);
    assertThat(a.apply("A", "B")).isEqualTo("AB");
  }

  @Test
  public void test_biFunction_fail1() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.apply("A", "B"));
  }

  @Test
  public void test_biFunction_fail2() {
    BiFunction<String, String, String> a = Unchecked.biFunction((t, u) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.apply("A", "B"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unaryOperator_success() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> t);
    assertThat(a.apply("A")).isEqualTo("A");
  }

  @Test
  public void test_unaryOperator_fail1() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.apply("A"));
  }

  @Test
  public void test_unaryOperator_fail2() {
    UnaryOperator<String> a = Unchecked.unaryOperator((t) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.apply("A"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_binaryOperator_success() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> t + u);
    assertThat(a.apply("A", "B")).isEqualTo("AB");
  }

  @Test
  public void test_binaryOperator_fail1() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.apply("A", "B"));
  }

  @Test
  public void test_binaryOperator_fail2() {
    BinaryOperator<String> a = Unchecked.binaryOperator((t, u) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.apply("A", "B"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_predicate_success() {
    Predicate<String> a = Unchecked.predicate((t) -> true);
    assertThat(a.test("A")).isEqualTo(true);
  }

  @Test
  public void test_predicate_fail1() {
    Predicate<String> a = Unchecked.predicate((t) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.test("A"));
  }

  @Test
  public void test_predicate_fail2() {
    Predicate<String> a = Unchecked.predicate((t) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.test("A"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_biPredicate_success() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> true);
    assertThat(a.test("A", "B")).isTrue();
  }

  @Test
  public void test_biPredicate_fail1() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.test("A", "B"));
  }

  @Test
  public void test_biPredicate_fail2() {
    BiPredicate<String, String> a = Unchecked.biPredicate((t, u) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.test("A", "B"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_consumer_success() {
    Consumer<String> a = Unchecked.consumer((t) -> {});
    a.accept("A");
  }

  @Test
  public void test_consumer_fail1() {
    Consumer<String> a = Unchecked.consumer((t) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.accept("A"));
  }

  @Test
  public void test_consumer_fail2() {
    Consumer<String> a = Unchecked.consumer((t) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.accept("A"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_biConsumer_success() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {});
    a.accept("A", "B");
  }

  @Test
  public void test_biConsumer_fail1() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.accept("A", "B"));
  }

  @Test
  public void test_biConsumer_fail2() {
    BiConsumer<String, String> a = Unchecked.biConsumer((t, u) -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.accept("A", "B"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_supplier_success() {
    Supplier<String> a = Unchecked.supplier(() -> "A");
    assertThat(a.get()).isEqualTo("A");
  }

  @Test
  public void test_supplier_fail1() {
    Supplier<String> a = Unchecked.supplier(() -> {
      throw new IOException();
    });
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> a.get());
  }

  @Test
  public void test_supplier_fail2() {
    Supplier<String> a = Unchecked.supplier(() -> {
      throw new Exception();
    });
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> a.get());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_validUtilityClass() {
    assertUtilityClass(Unchecked.class);
  }

  @Test
  public void test_propagate() {
    Error error = new Error("a");
    IllegalArgumentException argEx = new IllegalArgumentException("b");
    IOException ioEx = new IOException("c");
    URISyntaxException namingEx = new URISyntaxException("d", "e");

    // use old-style try-catch to ensure test really working
    try {
      Unchecked.propagate(error);
      fail("Expected Error");
    } catch (Error ex) {
      assertThat(ex).isSameAs(error);
    }
    try {
      Unchecked.propagate(argEx);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertThat(ex).isSameAs(argEx);
    }
    try {
      Unchecked.propagate(ioEx);
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      assertThat(ex.getClass()).isEqualTo(UncheckedIOException.class);
      assertThat(ex.getCause()).isSameAs(ioEx);
    }
    try {
      Unchecked.propagate(namingEx);
      fail("Expected RuntimeException");
    } catch (RuntimeException ex) {
      assertThat(ex.getClass()).isEqualTo(RuntimeException.class);
      assertThat(ex.getCause()).isSameAs(namingEx);
    }

    try {
      Unchecked.propagate(new InvocationTargetException(error));
      fail("Expected Error");
    } catch (Error ex) {
      assertThat(ex).isSameAs(error);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(argEx));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      assertThat(ex).isSameAs(argEx);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(ioEx));
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      assertThat(ex.getClass()).isEqualTo(UncheckedIOException.class);
      assertThat(ex.getCause()).isSameAs(ioEx);
    }
    try {
      Unchecked.propagate(new InvocationTargetException(namingEx));
      fail("Expected RuntimeException");
    } catch (RuntimeException ex) {
      assertThat(ex.getClass()).isEqualTo(RuntimeException.class);
      assertThat(ex.getCause()).isSameAs(namingEx);
    }
    try {
      Unchecked.propagate(new CompletionException(ioEx));
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      assertThat(ex.getClass()).isEqualTo(UncheckedIOException.class);
      assertThat(ex.getCause()).isSameAs(ioEx);
    }
    try {
      Unchecked.propagate(new ExecutionException(ioEx));
      fail("Expected UncheckedIOException");
    } catch (UncheckedIOException ex) {
      assertThat(ex.getClass()).isEqualTo(UncheckedIOException.class);
      assertThat(ex.getCause()).isSameAs(ioEx);
    }
  }

}
