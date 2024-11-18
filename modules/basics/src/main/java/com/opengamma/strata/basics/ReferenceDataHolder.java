/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Associates {@link ReferenceData} with the current execution thread.
 * <p>
 * This class provides a series of static methods that delegate to {@link InheritableThreadLocalReferenceDataHolderStrategy}.
 * This strategy uses an {@link InheritableThreadLocal} to store {@code ReferenceData} against a thread.
 *
 * </p>
 */
public class ReferenceDataHolder {
  private ReferenceDataHolder() {
    // prevent instantiation
  }

  private static final InheritableThreadLocalReferenceDataHolderStrategy STRATEGY = new InheritableThreadLocalReferenceDataHolderStrategy();

  /**
   * Sets the current reference data for this thread.
   *
   * @param refData the new value to hold (should never be null).
   * @throws IllegalArgumentException if {@code refData} is null.
   */
  public static void setReferenceData(ReferenceData refData) {
    ArgChecker.notNull(refData, "refData");
    STRATEGY.setReferenceData(refData);
  }

  /**
   * Gets the current reference data for this thread.
   * If it has not previously been set for this thread then it will be null.
   *
   * @return current reference data
   */
  public static ReferenceData getReferenceData() {
    return STRATEGY.getReferenceData();
  }

  /**
   * Gets the current reference data for this thread and, if it is not set, return a fallback value instead.
   *
   * @param other the fallback value to return if the reference data has not been set for this thread (should never be null).
   * @return the reference data for this thread, or the provided fallback value
   * @throws IllegalArgumentException if {@code other} is null.
   */
  public static ReferenceData getReferenceDataWithFallback(ReferenceData other) {
    ArgChecker.notNull(other, "other");
    return STRATEGY.getReferenceDataWithFallback(other);
  }

  /**
   * Executes a function to return a value with reference data set for the duration of the function and then
   * cleared afterwards.
   *
   * @param referenceData the new value to hold (should never be null).
   * @param fn the function to call to return a value
   * @return the value returned from the function
   * @param <T> type of value
   */
  public static <T> T withReferenceData(ReferenceData referenceData, Supplier<T> fn) {
    setReferenceData(referenceData);
    try {
      T result = fn.get();
      return result;
    } finally {
      clearReferenceData();
    }
  }

  /**
   * Executes a function that returns no value with reference data set for the duration of the function and then
   * cleared afterwards.
   *
   * @param referenceData the new value to hold (should never be null).
   * @param fn the function to call
   */
  public static void withReferenceData(ReferenceData referenceData, Runnable fn) {
    setReferenceData(referenceData);
    try {
      fn.run();
    } finally {
      clearReferenceData();
    }
  }

  /**
   * Clears the current reference data for this thread.
   */
  public static void clearReferenceData() {
    STRATEGY.clearReferenceData();
  }

  private static class InheritableThreadLocalReferenceDataHolderStrategy {
    private Logger log = LoggerFactory.getLogger(ReferenceDataHolder.class);
    private static final ThreadLocal<ReferenceData> HOLDER = new InheritableThreadLocal<>();

    public void clearReferenceData() {
      HOLDER.remove();
    }

    public ReferenceData getReferenceData() {
      return HOLDER.get();
    }

    public void setReferenceData(ReferenceData refData) {
      HOLDER.set(refData);
    }

    public ReferenceData getReferenceDataWithFallback(ReferenceData other) {
      final ReferenceData result = getReferenceData();
      if (result == null) {
        log.warn("No reference data saved in thread, falling back to default.");
        return other;
      }
      return result;
    }
  }
}
