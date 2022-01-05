/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.function.CheckedFunction;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Encodes and decodes common data formats.
 */
public enum ByteSourceCodec implements NamedEnum {

  /**
   * Encode base-64.
   */
  BASE64(base -> Base64.getEncoder().wrap(base), base -> Base64.getDecoder().wrap(base), ".base64"),
  /**
   * Encode using gz.
   */
  GZ(base -> new GZIPOutputStream(base), base -> new GZIPInputStream(base), ".gz"),
  /**
   * Encode using gz then base-64.
   */
  GZ_BASE64(
      base -> new GZIPOutputStream(Base64.getEncoder().wrap(base)),  // CSIGNORE
      base -> new GZIPInputStream(Base64.getDecoder().wrap(base)),  // CSIGNORE
      ".gz.base64");

  // helper for name conversions
  private static final EnumNames<ByteSourceCodec> NAMES = EnumNames.of(ByteSourceCodec.class);

  private final CheckedFunction<OutputStream, OutputStream> encoder;
  private final CheckedFunction<InputStream, InputStream> decoder;
  private final String suffix;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static ByteSourceCodec of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  // constructor
  private ByteSourceCodec(
      CheckedFunction<OutputStream, OutputStream> encoder, 
      CheckedFunction<InputStream, InputStream> decoder,
      String suffix) {
    
    this.encoder = encoder;
    this.decoder = decoder;
    this.suffix = suffix;
  }

  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

  //-------------------------------------------------------------------------
  // encodes the bytes to a source
  ArrayByteSource encode(byte[] bytes, String fileName) {
    ByteArrayOutputStream baos = encode(bytes);
    return new ArrayByteSource(baos.toByteArray(), fileName == null ? null : fileName + suffix);
  }

  // encodes the bytes
  private ByteArrayOutputStream encode(byte[] bytes) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    try (OutputStream out = encoder.apply(baos)) {
      out.write(bytes);
    } catch (Throwable ex) {
      throw Unchecked.propagate(ex);
    }
    return baos;
  }

  // decodes the input stream
  ArrayByteSource decode(byte[] bytes, String fileName) {
    try {
      ArrayByteSource decoded = ArrayByteSource.from(decode(bytes));
      return fileName == null ?
          decoded :
          decoded.withFileName(fileName.endsWith(suffix) ? fileName.substring(0, fileName.length() - suffix.length()) : fileName);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // decodes the input stream
  private InputStream decode(byte[] bytes) throws IOException {
    try {
      return decoder.apply(new ByteArrayInputStream(bytes));
    } catch (Throwable ex) {
      throw Unchecked.propagate(ex);
    }
  }

}
