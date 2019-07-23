/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;

/**
 * Test {@link UnicodeBom}.
 */
public class UnicodeBomTest {

  private static final byte X_00 = '\u0000';
  private static final byte X_FE = (byte) 0xFE;
  private static final byte X_FF = (byte) 0xFF;

  //-------------------------------------------------------------------------
  @Test
  public void test_toString_noBomUtf8() throws IOException {
    byte[] bytes = {'H', 'e', 'l', 'l', 'o'};
    String str = UnicodeBom.toString(bytes);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toString_bomUtf8() throws IOException {
    byte[] bytes = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'H', 'e', 'l', 'l', 'o'};
    String str = UnicodeBom.toString(bytes);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toString_bomUtf16BE() throws IOException {
    byte[] bytes = {X_FE, X_FF, X_00, 'H', X_00, 'e', X_00, 'l', X_00, 'l', X_00, 'o'};
    String str = UnicodeBom.toString(bytes);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toString_bomUtf16LE() throws IOException {
    byte[] bytes = {X_FF, X_FE, 'H', X_00, 'e', X_00, 'l', X_00, 'l', X_00, 'o', X_00};
    String str = UnicodeBom.toString(bytes);
    assertThat(str).isEqualTo("Hello");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toCharSource_noBomUtf8() throws IOException {
    byte[] bytes = {'H', 'e', 'l', 'l', 'o'};
    ByteSource byteSource = ByteSource.wrap(bytes);
    CharSource charSource = UnicodeBom.toCharSource(byteSource);
    String str = charSource.read();
    assertThat(str).isEqualTo("Hello");
    assertThat(charSource.asByteSource(StandardCharsets.UTF_8).contentEquals(byteSource)).isTrue();
    assertThat(charSource.toString().startsWith("UnicodeBom")).isEqualTo(true);
  }

  @Test
  public void test_toReader_noBomUtf8() throws IOException {
    byte[] bytes = {'H', 'e', 'l', 'l', 'o'};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("Hello");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toReader_bomUtf8() throws IOException {
    byte[] bytes = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'H', 'e', 'l', 'l', 'o'};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toReader_bomUtf16BE() throws IOException {
    byte[] bytes = {X_FE, X_FF, X_00, 'H', X_00, 'e', X_00, 'l', X_00, 'l', X_00, 'o'};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toReader_bomUtf16BE_short() throws IOException {
    byte[] bytes = {X_FE, X_FF};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("");
  }

  @Test
  public void test_toReader_almostBomUtf16BE() throws IOException {
    byte[] bytes = {X_FE, X_00};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo(new String(bytes, StandardCharsets.UTF_8));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toReader_bomUtf16LE() throws IOException {
    byte[] bytes = {X_FF, X_FE, 'H', X_00, 'e', X_00, 'l', X_00, 'l', X_00, 'o', X_00};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("Hello");
  }

  @Test
  public void test_toReader_bomUtf16LE_short() throws IOException {
    byte[] bytes = {X_FF, X_FE};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo("");
  }

  @Test
  public void test_toReader_almostBomUtf16LE() throws IOException {
    byte[] bytes = {X_FF, X_00};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo(new String(bytes, StandardCharsets.UTF_8));
  }

  @Test
  public void test_toReader_notBomUtf16LE() throws IOException {
    byte[] bytes = {X_00, X_FE, 'M', 'P'};
    Reader reader = UnicodeBom.toReader(new ByteArrayInputStream(bytes));
    String str = CharStreams.toString(reader);
    assertThat(str).isEqualTo(new String(bytes, StandardCharsets.UTF_8));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_validUtilityClass() {
    assertUtilityClass(UnicodeBom.class);
  }

}
