/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.common.base.Optional;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * A byte source implementation that explicitly wraps a byte array.
 * <p>
 * This implementation allows {@link IOException} to be avoided in many cases,
 * and to be able to create and retrieve the internal array unsafely.
 */
public final class ArrayByteSource extends ByteSource {

  /**
   * An empty source.
   */
  public static final ArrayByteSource EMPTY = new ArrayByteSource(new byte[0]);

  /**
   * The byte array.
   */
  private final byte[] array;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, copying the array.
   * 
   * @param array  the array, copied
   * @return the byte source
   */
  public static ArrayByteSource copyOf(byte[] array) {
    return new ArrayByteSource(array.clone());
  }

  /**
   * Obtains an instance by copying part of an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy
   * @param fromIndex  the offset from the start of the array
   * @return an array containing the specified values
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public static ArrayByteSource copyOf(byte[] array, int fromIndex) {
    return copyOf(array, fromIndex, array.length);
  }

  /**
   * Obtains an instance by copying part of an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy
   * @param fromIndexInclusive  the start index of the input array to copy from
   * @param toIndexExclusive  the end index of the input array to copy to
   * @return an array containing the specified values
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public static ArrayByteSource copyOf(byte[] array, int fromIndexInclusive, int toIndexExclusive) {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length);
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length);
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return EMPTY;
    }
    return new ArrayByteSource(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive));
  }

  /**
   * Creates an instance, not copying the array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   * 
   * @param array  the array, not copied
   * @return the byte source
   */
  public static ArrayByteSource ofUnsafe(byte[] array) {
    return new ArrayByteSource(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from another byte source.
   * 
   * @param other  the other byte source
   * @return the byte source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource from(ByteSource other) {
    if (other instanceof ArrayByteSource) {
      return (ArrayByteSource) other;
    }
    return new ArrayByteSource(Unchecked.wrap(() -> other.read()));
  }

  /**
   * Creates an instance from an input stream.
   * <p>
   * This method use the supplier to open the input stream, extract the bytes and close the stream.
   * It is intended that invoking the supplier opens the stream.
   * It is not intended that an already open stream is supplied.
   * 
   * @param inputStreamSupplier  the supplier of the input stream
   * @return the byte source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource from(CheckedSupplier<InputStream> inputStreamSupplier) {
    return Unchecked.wrap(() -> {
      try (InputStream in = inputStreamSupplier.get()) {
        byte[] bytes = Unchecked.wrap(() -> ByteStreams.toByteArray(in));
        return new ArrayByteSource(bytes);
      }
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, without copying the array.
   * 
   * @param array  the array, not copied
   */
  private ArrayByteSource(byte[] array) {
    this.array = array;
  }

  /**
   * Returns the underlying array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the array returned by this method.
   * Doing so would violate the immutability of this class.
   * 
   * @return the raw array
   */
  public byte[] readUnsafe() {
    return array;
  }

  /**
   * Reads the source, converting to UTF-8.
   * 
   * @return the UTF-8 string
   */
  public String readUtf8() {
    return new String(array, StandardCharsets.UTF_8);
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the UTF-8 string
   */
  public String readUtf8UsingBom() {
    return UnicodeBom.toString(array);
  }

  /**
   * Returns a {@code CharSource} for the same bytes, converted to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the equivalent {@code CharSource}
   */
  public CharSource asCharSourceUtf8UsingBom() {
    return CharSource.wrap(readUtf8UsingBom());
  }

  //-------------------------------------------------------------------------
  @Override
  public ByteArrayInputStream openStream() {
    return new ByteArrayInputStream(array);
  }

  @Override
  public ByteArrayInputStream openBufferedStream() {
    return openStream();
  }

  @Override
  public boolean isEmpty() {
    return array.length == 0;
  }

  /**
   * Gets the size, which is always known.
   * 
   * @return the size, which is always known
   */
  @Override
  public Optional<Long> sizeIfKnown() {
    return Optional.of(size());
  }

  @Override
  public long size() {
    return array.length;
  }

  @Override
  public long copyTo(OutputStream output) throws IOException {
    output.write(array);
    return array.length;
  }

  @Override
  public byte[] read() {
    return array.clone();
  }

  @Override
  public <T> T read(ByteProcessor<T> processor) throws IOException {
    processor.processBytes(array, 0, array.length);
    return processor.getResult();
  }

  @Override
  public HashCode hash(HashFunction hashFunction) {
    return hashFunction.hashBytes(array);
  }

  @Override
  public String toString() {
    return "ArrayByteSource[" + size() + " bytes]";
  }

}
