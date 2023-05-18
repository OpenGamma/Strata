/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
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

import com.google.common.base.Throwables;
import com.opengamma.strata.collect.function.CheckedBiConsumer;
import com.opengamma.strata.collect.function.CheckedBiFunction;
import com.opengamma.strata.collect.function.CheckedBiPredicate;
import com.opengamma.strata.collect.function.CheckedBinaryOperator;
import com.opengamma.strata.collect.function.CheckedConsumer;
import com.opengamma.strata.collect.function.CheckedFunction;
import com.opengamma.strata.collect.function.CheckedPredicate;
import com.opengamma.strata.collect.function.CheckedRunnable;
import com.opengamma.strata.collect.function.CheckedSupplier;
import com.opengamma.strata.collect.function.CheckedUnaryOperator;

/**
 * Static utility methods that convert checked exceptions to unchecked.
 * <p>
 * Two {@code wrap()} methods are provided that can wrap an arbitrary piece of logic
 * and convert checked exceptions to unchecked.
 * <p>
 * A number of other methods are provided that allow a lambda block to be decorated
 * to avoid handling checked exceptions.
 * For example, the method {@link File#getCanonicalFile()} throws an {@link IOException}
 * which can be handled as follows:
 * <pre>
 *  stream.map(Unchecked.function(file -&gt; file.getCanonicalFile())
 * </pre>
 * <p>
 * Each method accepts a functional interface that is defined to throw {@link Throwable}.
 * Catching {@code Throwable} means that any method can be wrapped.
 * Any {@code InvocationTargetException} is extracted and processed recursively.
 * Any {@link IOException} is converted to an {@link UncheckedIOException}.
 * Any {@link ReflectiveOperationException} is converted to an {@link UncheckedReflectiveOperationException}.
 * Any {@link Error} or {@link RuntimeException} is re-thrown without alteration.
 * Any other exception is wrapped in a {@link RuntimeException}.
 */
public final class Unchecked {

  /**
   * Restricted constructor.
   */
  private Unchecked() {
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a block of code, converting checked exceptions to unchecked.
   * <pre>
   *   Unchecked.wrap(() -&gt; {
   *     // any code that throws a checked exception
   *   }
   * </pre>
   * <p>
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   *
   * @param block  the code block to wrap
   * @throws UncheckedIOException if an IO exception occurs
   * @throws RuntimeException if an exception occurs
   */
  public static void wrap(CheckedRunnable block) {
    try {
      block.run();
    } catch (Throwable ex) {
      throw propagate(ex);
    }
  }

  /**
   * Wraps a block of code, converting checked exceptions to unchecked.
   * <pre>
   *   Unchecked.wrap(() -&gt; {
   *     // any code that throws a checked exception
   *   }
   * </pre>
   * <p>
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   *
   * @param <T> the type of the result
   * @param block  the code block to wrap
   * @return the result of invoking the block
   * @throws UncheckedIOException if an IO exception occurs
   * @throws RuntimeException if an exception occurs
   */
  public static <T> T wrap(CheckedSupplier<T> block) {
    try {
      return block.get();
    } catch (Throwable ex) {
      throw propagate(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code Runnable} interface.
   * <p>
   * This wraps the specified runnable returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param runnable  the runnable to be decorated
   * @return the runnable instance that handles checked exceptions
   */
  public static Runnable runnable(CheckedRunnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code Function} interface.
   * <p>
   * This wraps the specified function returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the input type of the function
   * @param <R>  the return type of the function
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  public static <T, R> Function<T, R> function(CheckedFunction<T, R> function) {
    return (t) -> {
      try {
        return function.apply(t);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  /**
   * Converts checked exceptions to unchecked based on the {@code BiFunction} interface.
   * <p>
   * This wraps the specified function returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the first input type of the function
   * @param <U>  the second input type of the function
   * @param <R>  the return type of the function
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  public static <T, U, R> BiFunction<T, U, R> biFunction(CheckedBiFunction<T, U, R> function) {
    return (t, u) -> {
      try {
        return function.apply(t, u);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code UnaryOperator} interface.
   * <p>
   * This wraps the specified operator returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the type of the operator
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  public static <T> UnaryOperator<T> unaryOperator(CheckedUnaryOperator<T> function) {
    return (t) -> {
      try {
        return function.apply(t);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  /**
   * Converts checked exceptions to unchecked based on the {@code BinaryOperator} interface.
   * <p>
   * This wraps the specified operator returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the type of the operator
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  public static <T> BinaryOperator<T> binaryOperator(CheckedBinaryOperator<T> function) {
    return (t, u) -> {
      try {
        return function.apply(t, u);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code Predicate} interface.
   * <p>
   * This wraps the specified predicate returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the type of the predicate
   * @param predicate  the predicate to be decorated
   * @return the predicate instance that handles checked exceptions
   */
  public static <T> Predicate<T> predicate(CheckedPredicate<T> predicate) {
    return (t) -> {
      try {
        return predicate.test(t);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  /**
   * Converts checked exceptions to unchecked based on the {@code BiPredicate} interface.
   * <p>
   * This wraps the specified predicate returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the first type of the predicate
   * @param <U>  the second type of the predicate
   * @param predicate  the predicate to be decorated
   * @return the predicate instance that handles checked exceptions
   */
  public static <T, U> BiPredicate<T, U> biPredicate(CheckedBiPredicate<T, U> predicate) {
    return (t, u) -> {
      try {
        return predicate.test(t, u);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code Consumer} interface.
   * <p>
   * This wraps the specified consumer returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the type of the consumer
   * @param consumer  the consumer to be decorated
   * @return the consumer instance that handles checked exceptions
   */
  public static <T> Consumer<T> consumer(CheckedConsumer<T> consumer) {
    return (t) -> {
      try {
        consumer.accept(t);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  /**
   * Converts checked exceptions to unchecked based on the {@code BiConsumer} interface.
   * <p>
   * This wraps the specified consumer returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <T>  the first type of the consumer
   * @param <U>  the second type of the consumer
   * @param consumer  the consumer to be decorated
   * @return the consumer instance that handles checked exceptions
   */
  public static <T, U> BiConsumer<T, U> biConsumer(CheckedBiConsumer<T, U> consumer) {
    return (t, u) -> {
      try {
        consumer.accept(t, u);
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the {@code Supplier} interface.
   * <p>
   * This wraps the specified supplier returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an {@link UncheckedIOException}
   * or {@link RuntimeException} as appropriate.
   * 
   * @param <R>  the result type of the supplier
   * @param supplier  the supplier to be decorated
   * @return the supplier instance that handles checked exceptions
   */
  public static <R> Supplier<R> supplier(CheckedSupplier<R> supplier) {
    return () -> {
      try {
        return supplier.get();
      } catch (Throwable ex) {
        throw propagate(ex);
      }
    };
  }

  /**
   * Propagates {@code throwable} as-is if possible, or by wrapping in a {@code RuntimeException} if not.
   * <ul>
   *   <li>If {@code throwable} is an {@code InvocationTargetException} the cause is extracted and processed recursively.</li>
   *   <li>If {@code throwable} is an {@code CompletionException} the cause is extracted and processed recursively.</li>
   *   <li>If {@code throwable} is an {@code ExecutionException} the cause is extracted and processed recursively.</li>
   *   <li>If {@code throwable} is an {@code Error} or {@code RuntimeException} it is propagated as-is.</li>
   *   <li>If {@code throwable} is an {@code IOException} it is wrapped in {@code UncheckedIOException} and thrown.</li>
   *   <li>If {@code throwable} is an {@code ReflectiveOperationException} it is wrapped in
   *     {@code UncheckedReflectiveOperationException} and thrown.</li>
   *   <li>Otherwise {@code throwable} is wrapped in a {@code RuntimeException} and thrown.</li>
   * </ul>
   * This method always throws an exception. The return type is a convenience to satisfy the type system
   * when the enclosing method returns a value. For example:
   * <pre>
   *   T foo() {
   *     try {
   *       return methodWithCheckedException();
   *     } catch (Exception e) {
   *       throw Unchecked.propagate(e);
   *     }
   *   }
   * </pre>
   *
   * @param throwable the {@code Throwable} to propagate
   * @return never returns as an exception is always thrown
   */
  public static RuntimeException propagate(Throwable throwable) {
    if (throwable instanceof InvocationTargetException) {
      throw propagate(((InvocationTargetException) throwable).getCause());
    } else if (throwable instanceof CompletionException) {
      throw propagate(((CompletionException) throwable).getCause());
    } else if (throwable instanceof ExecutionException) {
      throw propagate(((ExecutionException) throwable).getCause());
    } else if (throwable instanceof IOException) {
      throw new UncheckedIOException((IOException) throwable);
    } else if (throwable instanceof ReflectiveOperationException) {
      throw new UncheckedReflectiveOperationException((ReflectiveOperationException) throwable);
    } else {
      Throwables.throwIfUnchecked(throwable);
      throw new RuntimeException(throwable);
    }
  }

}
