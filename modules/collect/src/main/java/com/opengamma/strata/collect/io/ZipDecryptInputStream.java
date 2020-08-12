/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `com.alutam.ziputils` package.
 * https://bitbucket.org/matulic/ziputils/src
 * Changes:
 * - package name
 * - reformat
 * - error messages
 */
/*
 *  Copyright 2011, 2012 Martin Matula (martin@alutam.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Input stream converting a password-protected zip to an unprotected zip.
 */
class ZipDecryptInputStream extends InputStream {

  private final InputStream delegate;
  private final int[] keys = new int[3];
  private final int[] pwdKeys = new int[3];

  private ZipState state = ZipState.SIGNATURE;
  private boolean isEncrypted;
  private ZipSection section;
  private int skipBytes;
  private int compressedSize;
  private int crc;

  // creates an instance
  ZipDecryptInputStream(InputStream stream, char[] password) {
    this.delegate = stream;
    pwdKeys[0] = 305419896;
    pwdKeys[1] = 591751049;
    pwdKeys[2] = 878082192;
    for (int i = 0; i < password.length; i++) {
      updateKeyArray((byte) (password[i] & 0xff), pwdKeys);
    }
  }

  @Override
  public int read() throws IOException {
    // errors throw UncheckedIOException, because IOException is caught somewhere causing the message to be lost
    int result = delegateRead();
    if (skipBytes == 0) {
      switch (state) {
        case SIGNATURE:
          if (!peekAheadEquals(LFH_SIGNATURE)) {
            state = ZipState.TAIL;
          } else {
            section = ZipSection.FILE_HEADER;
            skipBytes = 5;
            state = ZipState.FLAGS;
          }
          break;
        case FLAGS:
          isEncrypted = (result & 1) != 0;
          if ((result & 64) == 64) {
            throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, ZIP has strong encryption"));
          }
          if ((result & 8) == 8) {
            compressedSize = -1;
            state = ZipState.FN_LENGTH;
            skipBytes = 19;
          } else {
            state = ZipState.CRC;
            skipBytes = 10;
          }
          if (isEncrypted) {
            result -= 1;
          }
          break;
        case CRC:
          crc = result;
          state = ZipState.COMPRESSED_SIZE;
          break;
        case COMPRESSED_SIZE:
          int[] values = new int[4];
          peekAhead(values);
          compressedSize = 0;
          int valueInc = isEncrypted ? DECRYPT_HEADER_SIZE : 0;
          for (int i = 0; i < 4; i++) {
            compressedSize += values[i] << (8 * i);
            values[i] -= valueInc;
            if (values[i] < 0) {
              valueInc = 1;
              values[i] += 256;
            } else {
              valueInc = 0;
            }
          }
          overrideBuffer(values);
          result = values[0];
          if (section == ZipSection.DATA_DESCRIPTOR) {
            state = ZipState.SIGNATURE;
          } else {
            state = ZipState.FN_LENGTH;
          }
          skipBytes = 7;
          break;
        case FN_LENGTH:
          values = new int[4];
          peekAhead(values);
          skipBytes = 3 + values[0] + values[2] + (values[1] + values[3]) * 256;
          if (!isEncrypted) {
            if (compressedSize > 0) {
              throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, ZIP is not password protected"));
            }
            state = ZipState.SIGNATURE;
          } else {
            state = ZipState.HEADER;
          }
          break;
        case HEADER:
          section = ZipSection.FILE_DATA;
          initKeys();
          byte lastValue = 0;
          for (int i = 0; i < DECRYPT_HEADER_SIZE; i++) {
            lastValue = (byte) (result ^ decryptByte());
            updateKeys(lastValue);
            result = delegateRead();
          }
          if ((lastValue & 0xff) != crc) {
            throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, wrong password"));
          }
          compressedSize -= DECRYPT_HEADER_SIZE;
          state = ZipState.DATA;
          // fall through
        case DATA:
          if (compressedSize == -1 && peekAheadEquals(DD_SIGNATURE)) {
            section = ZipSection.DATA_DESCRIPTOR;
            skipBytes = 5;
            state = ZipState.CRC;
          } else {
            result = (result ^ decryptByte()) & 0xff;
            updateKeys((byte) result);
            compressedSize--;
            if (compressedSize == 0) {
              state = ZipState.SIGNATURE;
            }
          }
          break;
        case TAIL:
        default:
          // do nothing
      }
    } else {
      skipBytes--;
    }
    return result;
  }

  private static final int BUF_SIZE = 8;
  private int bufOffset = BUF_SIZE;
  private final int[] buf = new int[BUF_SIZE];

  private int delegateRead() throws IOException {
    bufOffset++;
    if (bufOffset >= BUF_SIZE) {
      fetchData(0);
      bufOffset = 0;
    }
    return buf[bufOffset];
  }

  private boolean peekAheadEquals(int[] values) throws IOException {
    prepareBuffer(values);
    for (int i = 0; i < values.length; i++) {
      if (buf[bufOffset + i] != values[i]) {
        return false;
      }
    }
    return true;
  }

  private void prepareBuffer(int[] values) throws IOException {
    if (values.length > (BUF_SIZE - bufOffset)) {
      for (int i = bufOffset; i < BUF_SIZE; i++) {
        buf[i - bufOffset] = buf[i];
      }
      fetchData(BUF_SIZE - bufOffset);
      bufOffset = 0;
    }
  }

  private void peekAhead(int[] values) throws IOException {
    prepareBuffer(values);
    System.arraycopy(buf, bufOffset, values, 0, values.length);
  }

  private void overrideBuffer(int[] values) throws IOException {
    prepareBuffer(values);
    System.arraycopy(values, 0, buf, bufOffset, values.length);
  }

  private void fetchData(int offset) throws IOException {
    for (int i = offset; i < BUF_SIZE; i++) {
      buf[i] = delegate.read();
      if (buf[i] == -1) {
        break;
      }
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
    super.close();
  }

  private void initKeys() {
    System.arraycopy(pwdKeys, 0, keys, 0, keys.length);
  }

  private void updateKeys(byte charAt) {
    updateKeyArray(charAt, keys);
  }

  private byte decryptByte() {
    int temp = keys[2] | 2;
    return (byte) ((temp * (temp ^ 1)) >>> 8);
  }

  private static final int[] CRC_TABLE = new int[256];
  // compute the table
  // (could also have it pre-computed - see http://snippets.dzone.com/tag/crc32)
  static {
    for (int i = 0; i < 256; i++) {
      int r = i;
      for (int j = 0; j < 8; j++) {
        if ((r & 1) == 1) {
          r = (r >>> 1) ^ 0xedb88320;
        } else {
          r >>>= 1;
        }
      }
      CRC_TABLE[i] = r;
    }
  }

  //-------------------------------------------------------------------------
  // from com.alutam.ziputils.ZipUtil
  private static final int DECRYPT_HEADER_SIZE = 12;
  private static final int[] LFH_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
  private static final int[] DD_SIGNATURE = {0x50, 0x4b, 0x07, 0x08};

  private static void updateKeyArray(byte charAt, int[] keys) {
    keys[0] = crc32(keys[0], charAt);
    keys[1] += keys[0] & 0xff;
    keys[1] = keys[1] * 134775813 + 1;
    keys[2] = crc32(keys[2], (byte) (keys[1] >> 24));
  }

  private static int crc32(int oldCrc, byte charAt) {
    return ((oldCrc >>> 8) ^ CRC_TABLE[(oldCrc ^ charAt) & 0xff]);
  }

  private static enum ZipState {
    SIGNATURE, FLAGS, COMPRESSED_SIZE, FN_LENGTH, EF_LENGTH, HEADER, DATA, TAIL, CRC
  }

  private static enum ZipSection {
    FILE_HEADER, FILE_DATA, DATA_DESCRIPTOR
  }
}
