/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.google.common.primitives.Bytes;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.function.CheckedConsumer;
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
  /**
   * The file name, null if not known.
   */
  private final String fileName;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance, copying the array.
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
   * Obtains an instance, not copying the array.
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
   * Obtains an instance from a string using UTF-8.
   * 
   * @param str  the string to store using UTF-8
   * @return the byte source
   */
  public static ArrayByteSource ofUtf8(String str) {
    return new ArrayByteSource(str.getBytes(StandardCharsets.UTF_8));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from another byte source.
   * 
   * @param other  the other byte source
   * @return the byte source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource from(ByteSource other) {
    if (other instanceof ArrayByteSource) {
      return (ArrayByteSource) other;
    }
    String fileName = null;
    if (other instanceof BeanByteSource) {
      fileName = ((BeanByteSource) other).getFileName().orElse(null);

    } else if (other.getClass().getName().equals("com.google.common.io.ByteSource$ByteArrayByteSource")) {
      // extract the byte[] without using reflection
      // if the Guava implementation changes this could break, but that seems unlikely
      ByteProcessor<byte[]> processor = new ByteProcessor<byte[]>() {
        private byte[] captured;

        @Override
        public boolean processBytes(byte[] buf, int off, int len) throws IOException {
          if (captured != null) {
            // this defends against the Guava implementation being changed
            captured = Bytes.concat(captured, Arrays.copyOfRange(buf, off, off + len));
          } else if (off == 0 && len == buf.length) {
            // this is the normal case where we can just assign the source
            captured = buf;
          } else {
            // this happens if the source has been sliced
            captured = Arrays.copyOfRange(buf, off, off + len);
          }
          return true;
        }

        @Override
        public byte[] getResult() {
          return captured;
        }
      };
      return Unchecked.wrap(() -> new ArrayByteSource(other.read(processor)));

    } else {
      // handle all other byte sources
      String str = other.toString();
      if (str.equals("ByteSource.empty()")) {
        return EMPTY;
      } else if (str.startsWith("Files.asByteSource(")) {
        // extract the file name from toString()
        int pos = str.indexOf(')', 19);
        fileName = Paths.get(str.substring(19, pos)).getFileName().toString();
      } else if (str.startsWith("MoreFiles.asByteSource(")) {
        // extract the path name from toString()
        int pos = str.indexOf(',', 23);
        fileName = Paths.get(str.substring(23, pos)).getFileName().toString();
      } else if (str.startsWith("Resources.asByteSource(")) {
        // extract the URI from toString()
        int pos = str.indexOf(')', 23);
        String path = str.substring(23, pos);
        int lastSlash = path.lastIndexOf('/');
        fileName = path.substring(lastSlash + 1);
      }
    }
    return new ArrayByteSource(Unchecked.wrap(() -> other.read()), fileName);
  }

  /**
   * Obtains an instance from an input stream.
   * <p>
   * This method use the supplier to open the input stream, extract the bytes and close the stream.
   * It is intended that invoking the supplier opens the stream.
   * It is not intended that an already open stream is supplied.
   * 
   * @param inputStreamSupplier  the supplier of the input stream
   * @return the byte source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource from(CheckedSupplier<? extends InputStream> inputStreamSupplier) {
    return Unchecked.wrap(() -> {
      try (InputStream in = inputStreamSupplier.get()) {
        return from(in);
      }
    });
  }

  /**
   * Obtains an instance from an input stream.
   * <p>
   * This method uses an already open input stream, extracting the bytes.
   * The stream is not closed - that is the responsibility of the caller.
   * 
   * @param inputStream  the open input stream, which will not be closed
   * @return the byte source
   * @throws IOException if an IO error occurs
   */
  public static ArrayByteSource from(InputStream inputStream) throws IOException {
    byte[] bytes = ByteStreams.toByteArray(inputStream);
    return new ArrayByteSource(bytes);
  }

  /**
   * Obtains an instance from an input stream, specifying the expected size.
   * <p>
   * This method uses an already open input stream, extracting the bytes.
   * The stream is not closed - that is the responsibility of the caller.
   * 
   * @param inputStream  the open input stream, which will not be closed
   * @param expectedSize  the expected size of the input, not negative
   * @return the byte source
   * @throws IOException if an IO error occurs
   */
  public static ArrayByteSource from(InputStream inputStream, int expectedSize) throws IOException {
    ArgChecker.notNegative(expectedSize, "expectedSize");
    byte[] main = new byte[expectedSize];
    int remaining = expectedSize;
    while (remaining > 0) {
      int offset = expectedSize - remaining;
      int read = inputStream.read(main, offset, remaining);
      if (read == -1) {
        // actual stream size < expected size
        return new ArrayByteSource(Arrays.copyOf(main, offset));
      }
      remaining -= read;
    }
    // bytes is now full
    int firstExcess = inputStream.read();
    if (firstExcess == -1) {
      // actual stream size == expected size
      return new ArrayByteSource(main);
    }
    // actual stream size > expected size
    byte[] excess = ByteStreams.toByteArray(inputStream);
    byte[] result = Arrays.copyOf(main, main.length + 1 + excess.length);
    result[main.length] = (byte) firstExcess;
    System.arraycopy(excess, 0, result, main.length + 1, excess.length);
    return new ArrayByteSource(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that captures the contents of an output stream.
   * <p>
   * This method provides an output stream.
   * When bytes are written to the output stream, they are captured in the resulting byte source.
   * The caller is not responsible for closing the stream.
   * 
   * @param handler  the handler that writes to the output stream
   * @return the byte source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static ArrayByteSource fromOutput(CheckedConsumer<OutputStream> handler) {
    return Unchecked.wrap(() -> {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
        handler.accept(baos);
        return new ArrayByteSource(baos.toByteArray());
      }
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a base-64 encoded string.
   * 
   * @param base64  the base64 string to convert
   * @return the decoded byte source
   * @throws IllegalArgumentException if the input is not Base64 encoded
   */
  public static ArrayByteSource fromBase64(String base64) {
    return new ArrayByteSource(Base64.getDecoder().decode(base64));
  }

  /**
   * Obtains an instance from a hex encoded string, sometimes referred to as base-16.
   * 
   * @param hex  the hex string to convert
   * @return the decoded byte source
   * @throws IllegalArgumentException if the input is not hex encoded
   */
  public static ArrayByteSource fromHex(String hex) {
    return new ArrayByteSource(BaseEncoding.base16().decode(hex));
  }

  //-------------------------------------------------------------------------
  // creates an instance, without copying the array
  private ArrayByteSource(byte[] array) {
    this.array = array;
    this.fileName = null;
  }

  // creates an instance, without copying the array
  ArrayByteSource(byte[] array, String fileName) {
    this.array = array;
    this.fileName = Strings.emptyToNull(fileName);
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  @Override
  public Optional<String> getFileName() {
    return Optional.ofNullable(fileName);
  }

  /**
   * Returns an instance with the file name updated.
   * <p>
   * If a path is passed in, only the file name is retained.
   * 
   * @param fileName the file name, an empty string can be used to remove the file name
   * @return a source with the specified file name
   */
  public ArrayByteSource withFileName(String fileName) {
    ArgChecker.notNull(fileName, "fileName");
    int lastSlash = fileName.lastIndexOf('/');
    return new ArrayByteSource(array, fileName.substring(lastSlash + 1));
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

  @Override
  public StringCharSource asCharSourceUtf8() {
    // no need to bridge, as javac is already doing that
    return new StringCharSource(readUtf8(), fileName);
  }

  /**
   * @hidden
   * @return the source
   */
  @Override
  public CharSource asCharSourceUtf8$$bridge() { // CSIGNORE
    // bridged below for backwards compatibility
    return asCharSourceUtf8();
  }

  @Override
  public StringCharSource asCharSource(Charset charset) {
    return new StringCharSource(new String(array, charset), fileName);
  }

  @Override
  public StringCharSource asCharSourceUtf8UsingBom() {
    // bridged below for backwards compatibility
    return new StringCharSource(UnicodeBom.toString(array), fileName);
  }

  /**
   * @hidden
   * @return the source
   */
  @Override
  public CharSource asCharSourceUtf8UsingBom$$bridge() { // CSIGNORE
    return UnicodeBom.toCharSource((ByteSource) this);
  }

  //-------------------------------------------------------------------------
  @Override
  public HashCode hash(HashFunction hashFunction) {
    // overridden to use array directly for performance
    return hashFunction.hashBytes(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the MD5 hash of the bytes.
   * <p>
   * The returned hash is in byte form.
   * 
   * @return the MD5 hash
   * @deprecated Use {@link #toHash(HashFunction)}
   */
  @Deprecated
  public ArrayByteSource toMd5() {
    return toHash(Hashing.md5());
  }

  /**
   * Returns the SHA-512 hash of the bytes.
   * 
   * @return the SHA-512 hash
   * @deprecated Use {@link #toHash(HashFunction)}
   */
  @Deprecated
  public ArrayByteSource toSha512() {
    return ArrayByteSource.ofUnsafe(hash(Hashing.sha512()).asBytes());
  }

  /**
   * Encodes the byte source using base-64.
   * 
   * @return the base-64 encoded form
   */
  @Override
  public ArrayByteSource toBase64() {
    // overridden to use array directly for performance
    return ArrayByteSource.ofUnsafe(Base64.getEncoder().encode(array));
  }

  /**
   * Encodes the byte source using base-64, returning a string.
   * <p>
   * Equivalent to {@code toBase64().readUtf8()}.
   * 
   * @return the base-64 encoded string
   */
  @Override
  public String toBase64String() {
    // overridden to use array directly for performance
    return Base64.getEncoder().encodeToString(array);
  }

  /**
   * Encodes the byte source.
   * 
   * @param codec  the codec to use
   * @return the encoded form
   */
  public ArrayByteSource encode(ByteSourceCodec codec) {
    return codec.encode(array, fileName);
  }

  /**
   * Decodes the byte source.
   * 
   * @param codec  the codec to use
   * @return the decoded form
   */
  public ArrayByteSource decode(ByteSourceCodec codec) {
    return codec.decode(array, fileName);
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
  public com.google.common.base.Optional<Long> sizeIfKnown() {
    return com.google.common.base.Optional.of(size());
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
    return new ArrayByteSource(Arrays.copyOfRange(array, minPos, maxPos), fileName);
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
  public boolean contentEquals(ByteSource other) throws IOException {
    if (other instanceof ArrayByteSource) {
      return JodaBeanUtils.equal(array, ((ArrayByteSource) other).array);
    }
    return super.contentEquals(other);
  }

  @Override
  public ArrayByteSource load() {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ArrayByteSource other = ((ArrayByteSource) obj);
      return JodaBeanUtils.equal(fileName, other.fileName) &&
          JodaBeanUtils.equal(array, other.array);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(fileName);
    hash = hash * 31 + JodaBeanUtils.hashCode(array);
    return hash;
  }

  @Override
  public String toString() {
    return "ArrayByteSource[" + size() + " bytes" + (fileName != null ? ", " + fileName : "") + "]";
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
    private static final MetaProperty<String> FILE_NAME = new BasicMetaProperty<String>("fileName") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return ArrayByteSource.class;
      }

      @Override
      public Class<String> propertyType() {
        return String.class;
      }

      @Override
      public Type propertyGenericType() {
        return String.class;
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
      public String get(Bean bean) {
        return ((ArrayByteSource) bean).fileName;
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP =
        ImmutableMap.of("array", ARRAY, "fileName", FILE_NAME);

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
        private String fileName;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(ARRAY.name())) {
            return array;  // not cloned for performance
          } else if (propertyName.equals(FILE_NAME.name())) {
            return fileName;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<ArrayByteSource> set(String propertyName, Object value) {
          if (propertyName.equals(ARRAY.name())) {
            this.array = ((byte[]) ArgChecker.notNull(value, "value"));  // not cloned for performance
          } else if (propertyName.equals(FILE_NAME.name())) {
            this.fileName = (String) value;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public ArrayByteSource build() {
          ArrayByteSource byteSource = ArrayByteSource.ofUnsafe(array);
          return fileName != null ? byteSource.withFileName(fileName) : byteSource;
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
