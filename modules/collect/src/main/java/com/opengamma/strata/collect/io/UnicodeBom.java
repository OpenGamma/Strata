/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;

/**
 * Utilities that allow code to use the Unicode Byte Order Mark.
 * <p>
 * A Unicode file may contain a Byte Order Mark (BOM) that specifies which
 * encoding is used. Sadly, neither the JDK nor Guava handle this properly.
 * <p>
 * This class supports the BOM for UTF-8, UTF-16LE and UTF-16BE.
 * The UTF-32 formats are rarely seen and cannot be easily determined as
 * the UTF-32 BOMs are similar to the UTF-16 BOMs.
 */
public final class UnicodeBom {

  private static final byte X_FE = (byte) 0xFE;
  private static final byte X_EF = (byte) 0xEF;
  private static final byte X_FF = (byte) 0xFF;
  private static final byte X_BF = (byte) 0xBF;
  private static final byte X_BB = (byte) 0xBB;

  /**
   * Restricted constructor.
   */
  private UnicodeBom() {
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a {@code byte[]} to a {@code String}.
   * <p>
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   * 
   * @param input  the input byte array
   * @return the equivalent string
   */
  public static String toString(byte[] input) {
    if (input.length >= 3 && input[0] == X_EF && input[1] == X_BB && input[2] == X_BF) {
      return new String(input, 3, input.length - 3, StandardCharsets.UTF_8);

    } else if (input.length >= 2 && input[0] == X_FE && input[1] == X_FF) {
      return new String(input, 2, input.length - 2, StandardCharsets.UTF_16BE);

    } else if (input.length >= 2 && input[0] == X_FF && input[1] == X_FE) {
      return new String(input, 2, input.length - 2, StandardCharsets.UTF_16LE);

    } else {
      return new String(input, StandardCharsets.UTF_8);
    }
  }

  /**
   * Converts a {@code ByteSource} to a {@code CharSource}.
   * <p>
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   * 
   * @param byteSource  the byte source
   * @return the char source, that uses the BOM to determine the encoding
   */
  public static CharSource toCharSource(ByteSource byteSource) {
    return new CharSource() {

      @Override
      public ByteSource asByteSource(Charset charset) {
        return byteSource;
      }

      @Override
      public Reader openStream() throws IOException {
        return toReader(byteSource.openStream());
      }

      @Override
      public String toString() {
        return "UnicodeBom.toCharSource(" + byteSource.toString() + ")";
      }
    };
  }

  /**
   * Converts an {@code InputStream} to a {@code Reader}.
   * <p>
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   * 
   * @param inputStream  the input stream to wrap
   * @return the reader, that uses the BOM to determine the encoding
   * @throws IOException if an IO error occurs
   */
  public static Reader toReader(InputStream inputStream) throws IOException {
    return new BomReader(inputStream);
  }

  //-------------------------------------------------------------------------
  /**
   * Reader that manages the BOM.
   */
  private static final class BomReader extends Reader {

    private static final int MAX_BOM_SIZE = 4;

    private final InputStreamReader underlying;

    BomReader(InputStream inputStream) throws IOException {
      super(inputStream);

      Charset encoding;
      byte[] bom = new byte[MAX_BOM_SIZE];

      // read first 3 bytes such that they can be pushed back later
      PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, MAX_BOM_SIZE);
      int bytesRead = ByteStreams.read(pushbackStream, bom, 0, 3);

      // look for BOM and adapt, defauling to UTF-8
      if (bytesRead >= 3 && bom[0] == X_EF && bom[1] == X_BB && bom[2] == X_BF) {
        encoding = StandardCharsets.UTF_8;
        pushbackStream.unread(bom, 3, (bytesRead - 3));

      } else if (bytesRead >= 2 && bom[0] == X_FE && bom[1] == X_FF) {
        encoding = StandardCharsets.UTF_16BE;
        pushbackStream.unread(bom, 2, (bytesRead - 2));

      } else if (bytesRead >= 2 && bom[0] == X_FF && bom[1] == X_FE) {
        encoding = StandardCharsets.UTF_16LE;
        pushbackStream.unread(bom, 2, (bytesRead - 2));

      } else {
        encoding = StandardCharsets.UTF_8;
        pushbackStream.unread(bom, 0, bytesRead);
      }

      // use Java standard code now we know the encoding
      this.underlying = new InputStreamReader(pushbackStream, encoding);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
      return underlying.read(target);
    }

    @Override
    public int read() throws IOException {
      return underlying.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
      return underlying.read(cbuf);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      return underlying.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return underlying.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
      return underlying.ready();
    }

    @Override
    public boolean markSupported() {
      return underlying.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
      underlying.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
      underlying.reset();
    }

    @Override
    public void close() throws IOException {
      underlying.close();
    }
  }

}
