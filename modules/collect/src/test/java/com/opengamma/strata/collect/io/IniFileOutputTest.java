/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.CharSource;

/**
 * Test {@link IniFileOutput}.
 */
public class IniFileOutputTest {

  private static final String TWO_SECTIONS_NEWLINE_PADDED = "" +
      "[Section A]\n" +
      "a = b\n" +
      "\n" +
      "[Section B]\n" +
      "c = d\n" +
      "\n";

  private static final String SECTION_A = "Section A";

  private static final PropertySet SECTION_A_VALUES = PropertySet.of(ImmutableMultimap.of(
      "a", "b",
      "a", "c"));

  private static final String SINGLE_SECTION_NEWLINE_PADDED = "" +
      "[Section A]\n" +
      "a = b\n" +
      "a = c\n";

  //-------------------------------------------------------------------------
  @Test
  public void test_standard_writeFile_systemNewLine_padSeparator() {
    String fileContents = TWO_SECTIONS_NEWLINE_PADDED.replaceAll("\n", System.lineSeparator());
    IniFile file = IniFile.of(CharSource.wrap(fileContents));
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf).writeIniFile(file);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeFile_customNewLine_padSeparator() {
    String fileContents = TWO_SECTIONS_NEWLINE_PADDED;
    IniFile file = IniFile.of(CharSource.wrap(fileContents));
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, "\n").writeIniFile(file);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeFile_customNewLine_noPadding() {
    String fileContents = TWO_SECTIONS_NEWLINE_PADDED.replaceAll(" = ", "=");
    IniFile file = IniFile.of(CharSource.wrap(fileContents));
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, false, "\n").writeIniFile(file);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeFile_systemNewLine_noPadding() {
    String fileContents = TWO_SECTIONS_NEWLINE_PADDED
        .replaceAll("\n", System.lineSeparator())
        .replaceAll(" = ", "=");
    IniFile file = IniFile.of(CharSource.wrap(fileContents));
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, false).writeIniFile(file);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_standard_writeSection_systemNewLine_padSeparator() {
    String fileContents = SINGLE_SECTION_NEWLINE_PADDED.replaceAll("\n", System.lineSeparator());
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf).writeSection(SECTION_A, SECTION_A_VALUES);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeSection_customNewLine_padSeparator() {
    String fileContents = SINGLE_SECTION_NEWLINE_PADDED;
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, "\n").writeSection(SECTION_A, SECTION_A_VALUES);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeSection_customNewLine_noPadding() {
    String fileContents = SINGLE_SECTION_NEWLINE_PADDED.replaceAll(" = ", "=");
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, false, "\n").writeSection(SECTION_A, SECTION_A_VALUES);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  @Test
  public void test_standard_writeSection_systemNewLine_noPadding() {
    String fileContents = SINGLE_SECTION_NEWLINE_PADDED
        .replaceAll("\n", System.lineSeparator())
        .replaceAll(" = ", "=");
    StringBuilder buf = new StringBuilder();
    IniFileOutput.standard(buf, false).writeSection(SECTION_A, SECTION_A_VALUES);
    assertThat(buf.toString()).isEqualTo(fileContents);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_exception() {
    Appendable throwingAppendable = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        throw new IOException();
      }

      @Override
      public Appendable append(char c) throws IOException {
        throw new IOException();
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        throw new IOException();
      }
    };

    IniFile file = IniFile.of(CharSource.wrap(TWO_SECTIONS_NEWLINE_PADDED));

    IniFileOutput output = IniFileOutput.standard(throwingAppendable);
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> output.writeIniFile(file));
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> output.writeSection("Section A", file.section("Section A")));
  }
}
