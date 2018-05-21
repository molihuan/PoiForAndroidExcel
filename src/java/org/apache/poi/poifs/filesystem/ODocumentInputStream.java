/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.poifs.filesystem;

import java.io.IOException;
import java.util.function.Function;

import org.apache.poi.poifs.storage.DataInputBlock;
import org.apache.poi.util.RecordFormatException;

/**
 * This class provides methods to read a DocumentEntry managed by a
 * {@link OPOIFSFileSystem} instance.
 */
public final class ODocumentInputStream extends DocumentInputStream {
	/** current offset into the Document */
	private int _current_offset;

	/** current marked offset into the Document (used by mark and reset) */
	private int _marked_offset;

	/** the Document's size */
	private final int _document_size;

	/** have we been closed? */
	private boolean _closed;

	/** the actual Document */
	private final OPOIFSDocument _document;

	/** the data block containing the current stream pointer */
	private DataInputBlock _currentBlock;

	/**
	 * Create an InputStream from the specified DocumentEntry
	 * 
	 * @param document the DocumentEntry to be read
	 * 
	 * @exception IOException if the DocumentEntry cannot be opened (like, maybe it has
	 *                been deleted?)
	 */
	public ODocumentInputStream(DocumentEntry document) throws IOException {
		if (!(document instanceof DocumentNode)) {
			throw new IOException("Cannot open internal document storage");
		}
		DocumentNode documentNode = (DocumentNode)document;
		if (documentNode.getDocument() == null) {
         throw new IOException("Cannot open internal document storage");
		}
		      
		_current_offset = 0;
		_marked_offset = 0;
		_document_size = document.getSize();
		if (_document_size < 0) {
			throw new RecordFormatException("document_size cannot be < 0");
		}
		_closed = false;
		_document = documentNode.getDocument();
		_currentBlock = getDataInputBlock(0);
	}

	/**
	 * Create an InputStream from the specified Document
	 * 
	 * @param document the Document to be read
	 */
	public ODocumentInputStream(OPOIFSDocument document) {
		_current_offset = 0;
		_marked_offset = 0;
		_document_size = document.getSize();
		_closed = false;
		_document = document;
		_currentBlock = getDataInputBlock(0);
	}

	@Override
	public int available() {
		if (_closed) {
			throw new IllegalStateException("cannot perform requested operation on a closed stream");
		}
		return _document_size - _current_offset;
	}

   @Override
	public void close() {
		_closed = true;
	}

   @Override
	public void mark(int ignoredReadlimit) {
		_marked_offset = _current_offset;
	}

	private DataInputBlock getDataInputBlock(int offset) {
		return _document.getDataInputBlock(offset);
	}

   @Override
	public int read() throws IOException {
		dieIfClosed();
		if (atEOD()) {
			return EOF;
		}
		int result = _currentBlock.readUByte();
		_current_offset++;
		if (_currentBlock.available() < 1) {
			_currentBlock = getDataInputBlock(_current_offset);
		}
		return result;
	}

   @Override
	public int read(byte[] b, int off, int len) throws IOException {
		dieIfClosed();
		if (b == null) {
			throw new IllegalArgumentException("buffer must not be null");
		}
		if (off < 0 || len < 0 || b.length < off + len) {
			throw new IndexOutOfBoundsException("can't read past buffer boundaries");
		}
		if (len == 0) {
			return 0;
		}
		if (atEOD()) {
			return EOF;
		}
		int limit = Math.min(_document_size - _current_offset, len);
		readFully(b, off, limit);
		return limit;
	}

	/**
	 * Repositions this stream to the position at the time the mark() method was
	 * last called on this input stream. If mark() has not been called this
	 * method repositions the stream to its beginning.
	 */
   @Override
	public void reset() {
		_current_offset = _marked_offset;
		_currentBlock = getDataInputBlock(_current_offset);
	}

   @Override
	public long skip(long n) throws IOException {
		dieIfClosed();
		if (n < 0) {
			return 0;
		}
		int new_offset = _current_offset + (int) n;

		if (new_offset < _current_offset) {

			// wrap around in converting a VERY large long to an int
			new_offset = _document_size;
		} else if (new_offset > _document_size) {
			new_offset = _document_size;
		}
		long rval = new_offset - _current_offset;

		_current_offset = new_offset;
		_currentBlock = getDataInputBlock(_current_offset);
		return rval;
	}

	private void dieIfClosed() throws IOException {
		if (_closed) {
			throw new IOException("cannot perform requested operation on a closed stream");
		}
	}

	private boolean atEOD() {
		return _current_offset == _document_size;
	}

	private void checkAvaliable(int requestedSize) {
		if (_closed) {
			throw new IllegalStateException("cannot perform requested operation on a closed stream");
		}
		if (requestedSize > _document_size - _current_offset) {
			throw new RuntimeException("Buffer underrun - requested " + requestedSize
					+ " bytes but " + (_document_size - _current_offset) + " was available");
		}
	}

   @Override
	public byte readByte() {
		return (byte) readUByte();
	}

   @Override
	public double readDouble() {
		return Double.longBitsToDouble(readLong());
	}

   @Override
	public short readShort() {
		return (short) readUShort();
	}

   @Override
	public void readFully(byte[] buf, int off, int len) {
		checkAvaliable(len);

		Function<Integer,DataInputBlock> nextDataInputBlock = (offset) -> {
			if (offset >= _document_size) {
				_currentBlock = null;
			} else if (offset != _current_offset) {
				_currentBlock = getDataInputBlock(offset);
			}
			return _currentBlock;
		};

		_current_offset = readFullyInternal(buf, off, len, _current_offset, _document_size, nextDataInputBlock);
	}

	/* package */ static int readFullyInternal(byte[] buf, int off, int len, int currentOffset, int maxSize, Function<Integer,DataInputBlock> nextDataInputBlock) {
		DataInputBlock currentBlock = nextDataInputBlock.apply(currentOffset);
		if (currentBlock == null) {
			throw new IllegalStateException("reached end of document stream unexpectedly");
		}
		int blockAvailable = currentBlock.available();
		if (blockAvailable > len) {
			currentBlock.readFully(buf, off, len);
			return currentOffset + len;
		}
		// else read big amount in chunks
		int remaining = len;
		int writePos = off;
		int offset = currentOffset;
		while (remaining > 0) {
			final boolean blockIsExpiring = remaining >= blockAvailable;
			final int reqSize = (blockIsExpiring) ? blockAvailable : remaining;
			currentBlock.readFully(buf, writePos, reqSize);
			remaining -= reqSize;
			writePos += reqSize;
			offset += reqSize;
			if (blockIsExpiring) {
				if (offset >= maxSize) {
					if (remaining > 0) {
						throw new IllegalStateException(
								"reached end of document stream unexpectedly");
					}
					break;
				}
				currentBlock = nextDataInputBlock.apply(offset);
				if (currentBlock == null) {
					throw new IllegalStateException(
							"reached end of document stream unexpectedly");
				}
				blockAvailable = currentBlock.available();
			}
		}
		return offset;
	}

   @Override
	public long readLong() {
		checkAvaliable(SIZE_LONG);
		int blockAvailable = _currentBlock.available();
		long result;
		if (blockAvailable > SIZE_LONG) {
			result = _currentBlock.readLongLE();
		} else {
			DataInputBlock nextBlock = getDataInputBlock(_current_offset + blockAvailable);
			if (blockAvailable == SIZE_LONG) {
				result = _currentBlock.readLongLE();
			} else {
				result = nextBlock.readLongLE(_currentBlock, blockAvailable);
			}
			_currentBlock = nextBlock;
		}
		_current_offset += SIZE_LONG;
		return result;
	}

   @Override
	public int readInt() {
		checkAvaliable(SIZE_INT);
		int blockAvailable = _currentBlock.available();
		int result;
		if (blockAvailable > SIZE_INT) {
			result = _currentBlock.readIntLE();
		} else {
			DataInputBlock nextBlock = getDataInputBlock(_current_offset + blockAvailable);
			if (blockAvailable == SIZE_INT) {
				result = _currentBlock.readIntLE();
			} else {
				result = nextBlock.readIntLE(_currentBlock, blockAvailable);
			}
			_currentBlock = nextBlock;
		}
		_current_offset += SIZE_INT;
		return result;
	}

   @Override
	public int readUShort() {
		checkAvaliable(SIZE_SHORT);
		int blockAvailable = _currentBlock.available();
		int result;
		if (blockAvailable > SIZE_SHORT) {
			result = _currentBlock.readUShortLE();
		} else {
			DataInputBlock nextBlock = getDataInputBlock(_current_offset + blockAvailable);
			if (blockAvailable == SIZE_SHORT) {
				result = _currentBlock.readUShortLE();
			} else {
				result = nextBlock.readUShortLE(_currentBlock);
			}
			_currentBlock = nextBlock;
		}
		_current_offset += SIZE_SHORT;
		return result;
	}

   @Override
	public int readUByte() {
		checkAvaliable(1);
		int result = _currentBlock.readUByte();
		_current_offset++;
		if (_currentBlock.available() < 1) {
			_currentBlock = getDataInputBlock(_current_offset);
		}
		return result;
	}
}
