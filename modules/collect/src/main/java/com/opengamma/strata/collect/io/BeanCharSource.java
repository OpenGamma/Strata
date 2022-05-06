/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.LineProcessor;

/**
 * A char source implementation that is also a Joda-Bean.
 * <p>
 * See {@link StringCharSource}.
 * <p>
 * Various methods override {@code CharSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
 */
public abstract class BeanCharSource extends CharSource implements ImmutableBean {

  /**
   * Creates an instance.
   */
  protected BeanCharSource() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the file name of the source.
   * <p>
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   * 
   * @return the file name, empty if not known
   */
  public Optional<String> getFileName() {
    return Optional.empty();
  }

  /**
   * Gets the file name of the source.
   * <p>
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   *
   * @return the file name
   * @throws IllegalArgumentException if the file name is not known
   */
  public String getFileNameOrThrow() {
    return getFileName().orElseThrow(() -> new IllegalArgumentException("No file name present on char source"));
  }

  //-------------------------------------------------------------------------
  @Override
  public Stream<String> lines() {
    try {
      return super.lines();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public boolean isEmpty() {
    try {
      return super.isEmpty();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public long length() {
    try {
      return super.length();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String read() {
    try {
      return super.read();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String readFirstLine() {
    try {
      return super.readFirstLine();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public ImmutableList<String> readLines() {
    try {
      return super.readLines();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public <T> T readLines(LineProcessor<T> processor) {
    try {
      return super.readLines(processor);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void forEachLine(Consumer<? super String> action) {
    try {
      super.forEachLine(action);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the content of the source into memory.
   * 
   * @return the string-based source
   * @throws UncheckedIOException if an IO error occurs
   */
  public StringCharSource load() {
    return StringCharSource.from(this);
  }

  /**
   * Converts this char source to a byte source in UTF-8.
   * 
   * @return the equivalent byte source
   */
  public BeanByteSource asByteSourceUtf8() {
    return asByteSource(StandardCharsets.UTF_8);
  }

  @Override
  public BeanByteSource asByteSource(Charset charset) {
    return ArrayByteSource.from(super.asByteSource(charset));
  }

}
