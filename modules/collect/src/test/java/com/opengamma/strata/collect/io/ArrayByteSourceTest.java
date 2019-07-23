/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
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
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, -1, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, 0, 4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, 4, 5));
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

  public void test_ofUtf8() {
    ArrayByteSource test = ArrayByteSource.ofUtf8("ABC");
    assertEquals(test.size(), 3);
    assertEquals(test.read()[0], 'A');
    assertEquals(test.read()[1], 'B');
    assertEquals(test.read()[2], 'C');
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
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> ArrayByteSource.from(supplier));
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
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> ArrayByteSource.from(supplier));
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

  public void test_slice() throws IOException {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {65, 66, 67, 68, 69});
    assertEquals(test.size(), 5);
    assertEquals(test.slice(0, 3).readUtf8(), "ABC");
    assertEquals(test.slice(0, 5).readUtf8(), "ABCDE");
    assertEquals(test.slice(0, Long.MAX_VALUE).readUtf8(), "ABCDE");
    assertEquals(test.slice(1, 1).readUtf8(), "B");
    assertEquals(test.slice(1, 2).readUtf8(), "BC");
    assertEquals(test.slice(1, 3).readUtf8(), "BCD");
    assertEquals(test.slice(1, 4).readUtf8(), "BCDE");
    assertEquals(test.slice(2, 1).readUtf8(), "C");
    assertEquals(test.slice(2, 2).readUtf8(), "CD");
    assertEquals(test.slice(2, 3).readUtf8(), "CDE");
    assertEquals(test.slice(2, Long.MAX_VALUE).readUtf8(), "CDE");
    assertEquals(test.slice(5, 6).readUtf8(), "");
    assertEquals(test.slice(5, Long.MAX_VALUE).readUtf8(), "");
    assertEquals(test.slice(Long.MAX_VALUE - 10, Long.MAX_VALUE).readUtf8(), "");
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
    assertTrue(test.contentEquals(test));
    assertEquals(test.toString(), "ArrayByteSource[3 bytes]");
  }

  //-------------------------------------------------------------------------
  public void test_md5() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    byte[] hash = Hashing.md5().hashBytes(bytes).asBytes();
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.toMd5(), ArrayByteSource.ofUnsafe(hash));
  }

  public void test_sha512() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    byte[] hash = Hashing.sha512().hashBytes(bytes).asBytes();
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.toSha512(), ArrayByteSource.ofUnsafe(hash));
  }

  public void test_base64() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    byte[] base64 = BaseEncoding.base64().encode(bytes).getBytes(StandardCharsets.UTF_8);
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.toBase64(), ArrayByteSource.ofUnsafe(base64));
  }

  public void test_base64String() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    String base64 = BaseEncoding.base64().encode(bytes);
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.toBase64String(), base64);
    ArrayByteSource roundtrip = ArrayByteSource.fromBase64(base64);
    assertEquals(roundtrip, test);
    assertEquals(test.toBase64String(), test.toBase64().readUtf8());
  }

  public void test_hexString() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    String hex = "41424363";
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertEquals(test.toHexString(), hex);
    ArrayByteSource roundtrip = ArrayByteSource.fromHex(hex);
    assertEquals(roundtrip, test);
  }

}
