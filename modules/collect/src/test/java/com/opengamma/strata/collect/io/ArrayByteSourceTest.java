/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.testng.annotations.Test;

import com.google.common.io.ByteSource;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * Test {@link ArrayByteSource}.
 */
@Test
public class ArrayByteSourceTest {

  public void test_EMPTY() {
    ArrayByteSource test = ArrayByteSource.EMPTY;
    assertEquals(test.isEmpty(), true);
    assertEquals(test.size(), 0);
  }

  public void test_copyOf() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.size(), 3);
    assertEquals(test.read()[0], 1);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
    bytes[0] = 4;
    assertEquals(test.read()[0], 1);
  }

  public void test_copyOf_from() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1);
    assertEquals(test.size(), 2);
    assertEquals(test.read()[0], 2);
    assertEquals(test.read()[1], 3);
  }

  public void test_copyOf_fromTo() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1, 2);
    assertEquals(test.size(), 1);
    assertEquals(test.read()[0], 2);
  }

  public void test_copyOf_fromTo_empty() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1, 1);
    assertEquals(test.size(), 0);
  }

  public void test_copyOf_fromTo_bad() {
    byte[] bytes = {1, 2, 3};
    assertThrows(IndexOutOfBoundsException.class, () -> ArrayByteSource.copyOf(bytes, -1, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> ArrayByteSource.copyOf(bytes, 0, 4));
    assertThrows(IndexOutOfBoundsException.class, () -> ArrayByteSource.copyOf(bytes, 4, 5));
  }

  public void test_ofUnsafe() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.ofUnsafe(bytes);
    assertEquals(test.size(), 3);
    assertEquals(test.read()[0], 1);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
    bytes[0] = 4;  // abusing the unsafe factory
    assertEquals(test.read()[0], 4);
  }

  public void test_from_ByteSource() {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(source);
    assertEquals(test.size(), 3);
    assertEquals(test.read()[0], 1);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
  }

  public void test_from_ByteSource_alreadyArrayByteSource() {
    ArrayByteSource base = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(base);
    assertSame(test, base);
  }

  public void test_from_Supplier() {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(() -> source.openStream());
    assertEquals(test.size(), 3);
    assertEquals(test.read()[0], 1);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
  }

  public void test_from_SupplierExceptionOnCreate() {
    CheckedSupplier<InputStream> supplier = () -> {
      throw new IOException();
    };
    assertThrows(UncheckedIOException.class, () -> ArrayByteSource.from(supplier));
  }

  public void test_from_SupplierExceptionOnRead() {
    CheckedSupplier<InputStream> supplier = () -> {
      return new InputStream() {
        @Override
        public int read() throws IOException {
          throw new IOException();
        }
      };
    };
    assertThrows(UncheckedIOException.class, () -> ArrayByteSource.from(supplier));
  }

  //-------------------------------------------------------------------------
  public void test_read() {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    assertEquals(test.size(), 3);
    byte[] safeArray = test.read();
    safeArray[0] = 4;
    assertEquals(test.read()[0], 1);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
  }

  public void test_readUnsafe() {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    assertEquals(test.size(), 3);
    byte[] unsafeArray = test.readUnsafe();
    unsafeArray[0] = 4;  // abusing the unsafe array
    assertEquals(test.read()[0], 4);
    assertEquals(test.read()[1], 2);
    assertEquals(test.read()[2], 3);
  }

  public void test_methods() throws IOException {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {65, 66, 67});
    assertEquals(test.size(), 3);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.sizeIfKnown().isPresent(), true);
    assertEquals(test.sizeIfKnown().get(), (Long) 3L);
    assertEquals(test.readUtf8(), "ABC");
    assertEquals(test.readUtf8UsingBom(), "ABC");
    assertEquals(test.asCharSourceUtf8UsingBom().read(), "ABC");
    assertEquals(test.toString(), "ArrayByteSource[3 bytes]");
  }

}
