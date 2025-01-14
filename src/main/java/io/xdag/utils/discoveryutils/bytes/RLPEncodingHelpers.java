/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.utils.discoveryutils.bytes;

class RLPEncodingHelpers {
    private RLPEncodingHelpers() {}

    static boolean isSingleRLPByte(final BytesValue value) {
        return value.size() == 1 && value.get(0) >= 0;
    }

    static boolean isShortElement(final BytesValue value) {
        return value.size() <= 55;
    }

    static boolean isShortList(final int payloadSize) {
        return payloadSize <= 55;
    }

    /** The encoded size of the provided value. */
    static int elementSize(final BytesValue value) {
        if (isSingleRLPByte(value)) return 1;

        if (isShortElement(value)) return 1 + value.size();

        return 1 + sizeLength(value.size()) + value.size();
    }

    /** The encoded size of a list given the encoded size of its payload. */
    static int listSize(final int payloadSize) {
        int size = 1 + payloadSize;
        if (!isShortList(payloadSize)) size += sizeLength(payloadSize);
        return size;
    }

    /**
     * Writes the result of encoding the provided value to the provided destination (which must be big
     * enough).
     */
    static int writeElement(
            final BytesValue value, final MutableBytesValue dest, final int destOffset) {
        final int size = value.size();
        if (isSingleRLPByte(value)) {
            dest.set(destOffset, value.get(0));
            return destOffset + 1;
        }

        if (isShortElement(value)) {
            dest.set(destOffset, (byte) (0x80 + size));
            value.copyTo(dest, destOffset + 1);
            return destOffset + 1 + size;
        }

        final int offset = writeLongMetadata(0xb7, size, dest, destOffset);
        value.copyTo(dest, offset);
        return offset + size;
    }

    /**
     * Writes the encoded header of a list provided its encoded payload size to the provided
     * destination (which must be big enough).
     */
    static int writeListHeader(
            final int payloadSize, final MutableBytesValue dest, final int destOffset) {
        if (isShortList(payloadSize)) {
            dest.set(destOffset, (byte) (0xc0 + payloadSize));
            return destOffset + 1;
        }

        return writeLongMetadata(0xf7, payloadSize, dest, destOffset);
    }

    private static int writeLongMetadata(
            final int baseCode, final int size, final MutableBytesValue dest, final int destOffset) {
        final int sizeLength = sizeLength(size);
        dest.set(destOffset, (byte) (baseCode + sizeLength));
        int shift = 0;
        for (int i = 0; i < sizeLength; i++) {
            dest.set(destOffset + sizeLength - i, (byte) (size >> shift));
            shift += 8;
        }
        return destOffset + 1 + sizeLength;
    }

    private static int sizeLength(final int size) {
        final int zeros = Integer.numberOfLeadingZeros(size);
        return 4 - (zeros / 8);
    }
}
