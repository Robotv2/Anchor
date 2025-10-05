package fr.robotv2.anchor.api.util;

import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;

@ApiStatus.Internal
public class DummyBlob implements Blob {

    private byte[] data;

    public DummyBlob(byte[] data) {
        this.data = data != null ? data.clone() : new byte[0];
    }

    @Override
    public long length() {
        return data.length;
    }

    @Override
    public byte[] getBytes(long pos, int length) throws SQLException {
        if (pos < 1 || pos > data.length) {
            throw new SQLException("Invalid position: " + pos);
        }

        int startIndex = (int) (pos - 1); // JDBC positions are 1-based
        int bytesToRead = Math.min(length, data.length - startIndex);

        byte[] result = new byte[bytesToRead];
        System.arraycopy(data, startIndex, result, 0, bytesToRead);
        return result;
    }

    @Override
    public InputStream getBinaryStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public long position(byte[] pattern, long start) throws SQLException {
        if (pattern == null) {
            throw new SQLException("Pattern cannot be null");
        }
        if (start < 1 || start > data.length) {
            return -1;
        }

        int startIndex = (int) (start - 1);

        for (int i = startIndex; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i + 1; // Return 1-based position
            }
        }
        return -1;
    }

    @Override
    public long position(Blob pattern, long start) throws SQLException {
        if (pattern == null) {
            throw new SQLException("Pattern cannot be null");
        }
        byte[] patternBytes = pattern.getBytes(1, (int) pattern.length());
        return position(patternBytes, start);
    }

    @Override
    public int setBytes(long pos, byte[] bytes) throws SQLException {
        if (bytes == null) {
            throw new SQLException("Bytes cannot be null");
        }
        return setBytes(pos, bytes, 0, bytes.length);
    }

    @Override
    public int setBytes(long pos, byte[] bytes, int offset, int len)
            throws SQLException {
        if (pos < 1) {
            throw new SQLException("Invalid position: " + pos);
        }
        if (bytes == null) {
            throw new SQLException("Bytes cannot be null");
        }
        if (offset < 0 || offset > bytes.length || len < 0
                || offset + len > bytes.length) {
            throw new SQLException("Invalid offset or length");
        }

        int startIndex = (int) (pos - 1);
        int endIndex = startIndex + len;

        // Expand array if necessary
        if (endIndex > data.length) {
            byte[] newData = new byte[endIndex];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }

        System.arraycopy(bytes, offset, data, startIndex, len);
        return len;
    }

    @Override
    public OutputStream setBinaryStream(long pos) throws SQLException {
        if (pos < 1) {
            throw new SQLException("Invalid position: " + pos);
        }

        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    byte[] bytes = toByteArray();
                    setBytes(pos, bytes);
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    @Override
    public void truncate(long len) throws SQLException {
        if (len < 0 || len > data.length) {
            throw new SQLException("Invalid length: " + len);
        }

        byte[] newData = new byte[(int) len];
        System.arraycopy(data, 0, newData, 0, (int) len);
        data = newData;
    }

    @Override
    public void free() {
        data = new byte[0];
    }

    @Override
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        if (pos < 1 || pos > data.length) {
            throw new SQLException("Invalid position: " + pos);
        }

        int startIndex = (int) (pos - 1);
        int bytesToRead = (int) Math.min(length, data.length - startIndex);

        return new ByteArrayInputStream(data, startIndex, bytesToRead);
    }
}
