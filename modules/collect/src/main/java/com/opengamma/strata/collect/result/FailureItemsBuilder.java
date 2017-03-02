/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * A builder for a list of failure items.
 * <p>
 * This provides a builder to create {@link FailureItems}.
 */
public final class FailureItemsBuilder {

  /**
   * The mutable list of failures.
   */
  private final ImmutableList.Builder<FailureItem> listBuilder = ImmutableList.builder();

  /**
   * Creates an instance.
   */
  FailureItemsBuilder() {
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a failure to the list.
   * 
   * @param failure  the failure to add
   * @return this, for chaining
   */
  public FailureItemsBuilder addFailure(FailureItem failure) {
    listBuilder.add(failure);
    return this;
  }

  /**
   * Adds a list of failures to the list.
   * 
   * @param failures  the failures to add
   * @return this, for chaining
   */
  public FailureItemsBuilder addAllFailures(List<FailureItem> failures) {
    listBuilder.addAll(failures);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the resulting instance
   * 
   * @return the result
   */
  public FailureItems build() {
    return FailureItems.of(listBuilder.build());
  }

}
