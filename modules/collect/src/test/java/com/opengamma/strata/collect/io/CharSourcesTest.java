/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.TestHelper;

/**
 * Tests {@link CharSources}
 */
public class CharSourcesTest {

  private final String fileName = "src/test/resources/com/opengamma/strata/collect/io/utf16le.txt";

  @Test
  public void testPrivateConstructor() throws Exception {
    TestHelper.coverPrivateConstructor(CharSources.class);
  }

  @Test
  public void testOfFileName() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName);
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.length()).isEqualTo(10);
  }

  @Test
  public void testOfFileNameWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName, Charsets.UTF_16LE);
    assertThat(charSource.readFirstLine()).isEqualTo("Hello");
    assertThat(charSource.length()).isEqualTo(5);
  }

  @Test
  public void testOfFile() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName));
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.length()).isEqualTo(10);
  }

  @Test
  public void testOfFileWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName), Charsets.UTF_16LE);
    assertThat(charSource.readFirstLine()).isEqualTo("Hello");
    assertThat(charSource.length()).isEqualTo(5);
  }

  @Test
  public void testOfPath() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName));
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.length()).isEqualTo(10);
  }

  @Test
  public void testOfPathWithCharset() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName), Charsets.UTF_16LE);
    assertThat(charSource.readFirstLine()).isEqualTo("Hello");
    assertThat(charSource.length()).isEqualTo(5);
  }

  @Test
  public void testOfUrl() throws Exception {
    String fullPathToFile = "file:///" + System.getProperty("user.dir") + "/" + fileName;
    URL url = new URL(fullPathToFile);
    CharSource charSource = CharSources.ofUrl(url);
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
  }

  @Test
  public void testOfUrlWithCharset() throws Exception {
    String fullPathToFile = "file:///" + System.getProperty("user.dir") + "/" + fileName;
    URL url = new URL(fullPathToFile);
    CharSource charSource = CharSources.ofUrl(url, Charsets.UTF_16LE);
    assertThat(charSource.readFirstLine()).isEqualTo("Hello");
  }

  @Test
  public void testOfContentString() throws Exception {
    CharSource charSource = CharSources.ofContent("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.length()).isEqualTo(10);
  }

  @Test
  public void testOfContentByteArray() throws Exception {
    byte[] inputText = "H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes(Charsets.UTF_8);
    CharSource charSource = CharSources.ofContent(inputText);
    assertThat(charSource.readFirstLine()).isEqualTo("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertThat(charSource.length()).isEqualTo(10);
  }

  @Test
  public void testOfContentByteArrayWithCharset() throws Exception {
    byte[] inputText = "H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes(Charsets.UTF_8);
    CharSource charSource = CharSources.ofContent(inputText, Charsets.UTF_16LE);
    assertThat(charSource.readFirstLine()).isEqualTo("Hello");
    assertThat(charSource.length()).isEqualTo(5);
  }
}
