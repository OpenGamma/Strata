/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.concurrent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CloseableExecutor}.
 */
public class CloseableExecutorTest {

  @Test
  public void testAutoClose() {
    ExecutorService mockExecutorService = mock(ExecutorService.class);
    try (CloseableExecutor ignored = CloseableExecutor.of(mockExecutorService)) {
      // no-op
    }
    verify(mockExecutorService).shutdown();
  }
}
