/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * A byte source implementation that explicitly wraps a byte array.
 * <p>
 * This implementation allows {@link IOException} to be avoided in many cases,
 * and to be able to create and retrieve the internal array unsafely.
 */
public final class ArrayByteSource extends BeanByteSource implements ImmutableBean, Serializable {

  /**
   * An empty source.
   */
  public static final ArrayByteSource EMPTY = new ArrayByteSource(new byte[0]);

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    MetaBean.register(Meta.META);
  }

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

  /**
   * Creates an instance from a string using UTF-8.
   * 
   * @param str  the string to store using UTF-8
   * @return the byte source
   */
  public static ArrayByteSource ofUtf8(String str) {
    return new ArrayByteSource(str.getBytes(StandardCharsets.UTF_8));
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
   * Creates an instance from a base-64 encoded string.
   * 
   * @param base64  the base64 string to convert
   * @return the decoded byte source
   * @throws IllegalArgumentException if the input is not Base64 encoded
   */
  public static ArrayByteSource fromBase64(String base64) {
    return new ArrayByteSource(Base64.getDecoder().decode(base64));
  }

  /**
   * Creates an instance from a hex encoded string, sometimes referred to as base-16.
   * 
   * @param hex  the hex string to convert
   * @return the decoded byte source
   * @throws IllegalArgumentException if the input is not hex encoded
   */
  public static ArrayByteSource fromHex(String hex) {
    return new ArrayByteSource(BaseEncoding.base16().decode(hex));
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

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  //-------------------------------------------------------------------------
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
  @Override
  public String readUtf8() {
    return new String(array, StandardCharsets.UTF_8);
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the UTF-8 string
   */
  @Override
  public String readUtf8UsingBom() {
    return UnicodeBom.toString(array);
  }

  /**
   * Returns a {@code CharSource} for the same bytes, converted to UTF-8 using a Byte-Order Mark if available.
   * 
   * @return the equivalent {@code CharSource}
   */
  @Override
  public CharSource asCharSourceUtf8UsingBom() {
    return CharSource.wrap(readUtf8UsingBom());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the MD5 hash of the bytes.
   * 
   * @return the MD5 hash
   */
  public ArrayByteSource toMd5() {
    try {
      // MessageDigest instances are not thread safe so must be created each time
      MessageDigest md = MessageDigest.getInstance("MD5");
      return ArrayByteSource.ofUnsafe(md.digest(array));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Returns the SHA-512 hash of the bytes.
   * 
   * @return the SHA-512 hash
   */
  public ArrayByteSource toSha512() {
    try {
      // MessageDigest instances are not thread safe so must be created each time
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      return ArrayByteSource.ofUnsafe(md.digest(array));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Encodes the byte source using base-64.
   * 
   * @return the base-64 encoded form
   */
  public ArrayByteSource toBase64() {
    return ArrayByteSource.ofUnsafe(Base64.getEncoder().encode(array));
  }

  /**
   * Encodes the byte source using base-64, returning a string.
   * <p>
   * Equivalent to {@code toBase64().readUtf8()}.
   * 
   * @return the base-64 encoded string
   */
  public String toBase64String() {
    return Base64.getEncoder().encodeToString(array);
  }

  /**
   * Encodes the byte source using hex, sometimes referred to as base-16, returning a string.
   * 
   * @return the hex encoded string
   */
  public String toHexString() {
    return BaseEncoding.base16().encode(array);
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
  public ArrayByteSource slice(long offset, long length) {
    checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
    checkArgument(length >= 0, "length (%s) may not be negative", length);
    if (offset > array.length) {
      return EMPTY;
    }
    int minPos = (int) offset;
    long len = Math.min(Math.min(length, Integer.MAX_VALUE), array.length);
    int maxPos = (int) Math.min(minPos + len, array.length);
    return new ArrayByteSource(Arrays.copyOfRange(array, minPos, maxPos));
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
  public boolean contentEquals(ByteSource other) throws IOException {
    if (other instanceof ArrayByteSource) {
      return equals(other);
    }
    return super.contentEquals(other);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ArrayByteSource) {
      return Arrays.equals(array, ((ArrayByteSource) obj).array);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array);
  }

  @Override
  public String toString() {
    return "ArrayByteSource[" + size() + " bytes]";
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<byte[]> ARRAY = new BasicMetaProperty<byte[]>("array") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return ArrayByteSource.class;
      }

      @Override
      public Class<byte[]> propertyType() {
        return byte[].class;
      }

      @Override
      public Type propertyGenericType() {
        return byte[].class;
      }

      @Override
      public PropertyStyle style() {
        return PropertyStyle.IMMUTABLE;
      }

      @Override
      public List<Annotation> annotations() {
        return ImmutableList.of();
      }

      @Override
      public byte[] get(Bean bean) {
        return ((ArrayByteSource) bean).read();
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP = ImmutableMap.of("array", ARRAY);

    private Meta() {
    }

    @Override
    public boolean isBuildable() {
      return true;
    }

    @Override
    public BeanBuilder<ArrayByteSource> builder() {
      return new BasicImmutableBeanBuilder<ArrayByteSource>(this) {
        private byte[] array = new byte[0];

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(ARRAY.name())) {
            return array;  // not cloned for performance
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<ArrayByteSource> set(String propertyName, Object value) {
          if (propertyName.equals(ARRAY.name())) {
            this.array = ((byte[]) ArgChecker.notNull(value, "value"));  // not cloned for performance
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public ArrayByteSource build() {
          return ArrayByteSource.ofUnsafe(array);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return ArrayByteSource.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
