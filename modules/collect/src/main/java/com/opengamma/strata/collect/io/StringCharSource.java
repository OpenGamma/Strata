/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * A char source implementation that explicitly wraps a {@code String}.
 * <p>
 * This implementation allows {@link IOException} to be avoided in many cases,
 * and to be able to create and retrieve the internal array unsafely.
 */
public final class StringCharSource extends BeanCharSource implements ImmutableBean, Serializable {

  /**
   * An empty source.
   */
  public static final StringCharSource EMPTY = new StringCharSource("");

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    MetaBean.register(Meta.META);
  }

  /**
   * The string.
   */
  private final String value;
  /**
   * The file name, null if not known.
   */
  private final String fileName;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param str  the string
   * @return the char source
   */
  public static StringCharSource of(String str) {
    return new StringCharSource(str);
  }

  /**
   * Obtains an instance from a UTF-8 byte array.
   * 
   * @param bytes  the UTF-8 bytes
   * @return the char source
   */
  public static StringCharSource fromBytesUtf8(byte[] bytes) {
    return fromBytes(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Obtains an instance from a byte array.
   * 
   * @param bytes  the bytes
   * @param charset  the encoding
   * @return the char source
   */
  public static StringCharSource fromBytes(byte[] bytes, Charset charset) {
    return new StringCharSource(new String(bytes, charset));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from another char source.
   * 
   * @param other  the other char source
   * @return the char source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static StringCharSource from(CharSource other) {
    if (other instanceof StringCharSource) {
      return (StringCharSource) other;
    }
    String fileName = null;
    if (other instanceof BeanCharSource) {
      fileName = ((BeanCharSource) other).getFileName().orElse(null);
    } else {
      // handle all other char sources
      String str = other.toString();
      if (str.equals("CharSource.empty()")) {
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
    return new StringCharSource(Unchecked.wrap(() -> other.read()), fileName);
  }

  /**
   * Obtains an instance from a {@code Reader}.
   * <p>
   * This method use the supplier to open the reader, extract the chars and close the reader.
   * It is intended that invoking the supplier opens the reader.
   * It is not intended that an already open reader is supplied.
   * 
   * @param readerSupplier  the supplier of the reader
   * @return the char source
   * @throws UncheckedIOException if an IO error occurs
   */
  public static StringCharSource from(CheckedSupplier<? extends Reader> readerSupplier) {
    return Unchecked.wrap(() -> {
      try (Reader in = readerSupplier.get()) {
        return from(in);
      }
    });
  }

  /**
   * Obtains an instance from a {@code Readable}.
   * <p>
   * This method uses an already open reader, extracting the chars.
   * The stream is not closed - that is the responsibility of the caller.
   * 
   * @param reader  the open reader, which will not be closed
   * @return the char source
   * @throws IOException if an IO error occurs
   */
  public static StringCharSource from(Readable reader) throws IOException {
    String chars = CharStreams.toString(reader);
    return new StringCharSource(chars);
  }

  //-------------------------------------------------------------------------
  // creates an instance
  private StringCharSource(String value) {
    this.value = value;
    this.fileName = null;
  }

  // creates an instance
  StringCharSource(String value, String fileName) {
    this.value = value;
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
  public StringCharSource withFileName(String fileName) {
    ArgChecker.notNull(fileName, "fileName");
    int lastSlash = fileName.lastIndexOf('/');
    return new StringCharSource(value, fileName.substring(lastSlash + 1));
  }

  //-------------------------------------------------------------------------
  @Override
  public Reader openStream() {
    return new StringReader(value);
  }

  @Override
  public boolean isEmpty() {
    return value.length() == 0;
  }

  /**
   * Gets the length, which is always known.
   * 
   * @return the length, which is always known
   */
  @Override
  public com.google.common.base.Optional<Long> lengthIfKnown() {
    return com.google.common.base.Optional.of(length());
  }

  @Override
  public long length() {
    return value.length();
  }

  @Override
  public long copyTo(Appendable appendable) throws IOException {
    appendable.append(value);
    return value.length();
  }

  @Override
  public long copyTo(CharSink sink) throws IOException {
    sink.write(value);
    return value.length();
  }

  @Override
  public String read() {
    return value;
  }

  @Override
  public StringCharSource load() {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public ArrayByteSource asByteSourceUtf8() {
    return asByteSource(StandardCharsets.UTF_8);
  }

  @Override
  public ArrayByteSource asByteSource(Charset charset) {
    return new ArrayByteSource(value.getBytes(charset), fileName);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      StringCharSource other = ((StringCharSource) obj);
      return JodaBeanUtils.equal(fileName, other.fileName) &&
          JodaBeanUtils.equal(value, other.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(fileName);
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    return hash;
  }

  @Override
  public String toString() {
    return "StringCharSource[" + length() + " chars" + (fileName != null ? ", " + fileName : "") + "]";
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<String> VALUE = new BasicMetaProperty<String>("value") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return StringCharSource.class;
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
        return ((StringCharSource) bean).value;
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
        return StringCharSource.class;
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
        return ((StringCharSource) bean).fileName;
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP =
        ImmutableMap.of("value", VALUE, "fileName", FILE_NAME);

    private Meta() {
    }

    @Override
    public boolean isBuildable() {
      return true;
    }

    @Override
    public BeanBuilder<StringCharSource> builder() {
      return new BasicImmutableBeanBuilder<StringCharSource>(this) {
        private String value;
        private String fileName;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(VALUE.name())) {
            return value;
          } else if (propertyName.equals(FILE_NAME.name())) {
            return fileName;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<StringCharSource> set(String propertyName, Object value) {
          if (propertyName.equals(VALUE.name())) {
            this.value = (String) ArgChecker.notNull(value, "value");
          } else if (propertyName.equals(FILE_NAME.name())) {
            this.fileName = (String) value;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public StringCharSource build() {
          ArgChecker.notNull(value, "value");
          return new StringCharSource(value, fileName);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return StringCharSource.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
