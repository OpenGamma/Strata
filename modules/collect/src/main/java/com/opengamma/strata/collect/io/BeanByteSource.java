/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.joda.beans.ImmutableBean;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

/**
 * A byte source implementation that is also a Joda-Bean.
 * <p>
 * See {@link ArrayByteSource}, {@link UriByteSource} and {@link FileByteSource}.
 */
public abstract class BeanByteSource extends ByteSource implements ImmutableBean {

  /**
   * Creates an instance.
   */
  protected BeanByteSource() {
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the byte source is empty, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public boolean isEmpty() {
    try {
      return super.isEmpty();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Gets the size of the byte source, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public long size() {
    try {
      return super.size();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Reads the source as a byte array, throwing an unchecked exception.
   * <p>
   * This overrides {@code ByteSource} to throw {@link UncheckedIOException} instead of {@link IOException}.
   * 
   * @return the byte array
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  public byte[] read() {
    try {
      return super.read();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Reads the source, converting to UTF-8.
   * 
   * @return the UTF-8 string
   * @throws UncheckedIOException if an IO error occurs
   */
  public String readUtf8() {
    return new String(read(), StandardCharsets.UTF_8);
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the UTF-8 string
   * @throws UncheckedIOException if an IO error occurs
   */
  public String readUtf8UsingBom() {
    return UnicodeBom.toString(read());
  }

  /**
   * Returns a {@code CharSource} for the same bytes, converted to UTF-8.
   * <p>
   * This does not read the underlying source.
   * 
   * @return the equivalent {@code CharSource}
   */
  public CharSource asCharSourceUtf8() {
    return asCharSource(StandardCharsets.UTF_8);
  }

  /**
   * Returns a {@code CharSource} for the File, converted to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the equivalent {@code CharSource}
   */
  public CharSource asCharSourceUtf8UsingBom() {
    return UnicodeBom.toCharSource(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the content of the byte source into memory.
   * 
   * @return the byte array
   * @throws UncheckedIOException if an IO error occurs
   */
  public ArrayByteSource load() {
    return ArrayByteSource.from(this);
  }

}
