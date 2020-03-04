/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

// http://svn.apache.org/repos/asf/commons/proper/collections/branches/collections_jdk5_branch/src/java/org/apache/commons/collections/buffer/CircularFifoBuffer.java

public class BoundedFifoBuffer<E> {

	private transient E[] elements;

	private transient int start = 0;

	/**
	 * Index mod maxElements of the array position following the last buffer
	 * element. Buffer elements start at elements[start] and "wrap around"
	 * elements[maxElements-1], ending at elements[decrement(end)]. For example,
	 * elements = {c,a,b}, start=1, end=1 corresponds to the buffer [a,b,c].
	 */
	private transient int end = 0;

	private transient boolean full = false;

	private final int maxElements;

	private boolean circular;

	@SuppressWarnings("unchecked")
	public BoundedFifoBuffer(int size, boolean circular) {
		this.circular = circular;
		if (size <= 0) {
			throw new IllegalArgumentException("The size must be greater than 0");
		}
		elements = (E[]) new Object[size];
		maxElements = elements.length;
	}

	public int size() {
		int size = 0;
		if (end < start) {
			size = maxElements - start + end;
		} else if (end == start) {
			size = (full ? maxElements : 0);
		} else {
			size = end - start;
		}
		return size;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean isFull() {
		return size() == maxElements;
	}

	public int maxSize() {
		return maxElements;
	}

	public void clear() {
		full = false;
		start = 0;
		end = 0;
		Arrays.fill(elements, null);
	}

	/**
	 * Adds the given element to this buffer.
	 *
	 * @param element the element to add
	 * @param circular remove an element if the buffer is full
	 * @return true, always
	 * @throws NullPointerException if the given element is null
	 * @throws BufferOverflowException if this buffer is full
	 */
	public boolean add(E element) {
		if (null == element) {
			throw new NullPointerException("Attempted to add null object to buffer");
		}

		if (circular && isFull()) {
            remove();
        }
		
		if (full) {
			throw new RuntimeException("The buffer cannot hold more than " + maxElements + " objects.");
		}

		elements[end++] = element;

		if (end >= maxElements) {
			end = 0;
		}

		if (end == start) {
			full = true;
		}

		return true;
	}

	/**
	 * @return the least recently inserted element
	 * @throws BufferUnderflowException
	 *             if the buffer is empty
	 */
	public E get() {
		if (isEmpty()) {
			throw new RuntimeException("The buffer is already empty");
		}
		return elements[start];
	}

	/**
	 * Removes the least recently inserted element from this buffer.
	 *
	 * @return the least recently inserted element
	 * @throws BufferUnderflowException
	 *             if the buffer is empty
	 */
	public E remove() {
		if (isEmpty()) {
			throw new RuntimeException("The buffer is already empty");
		}

		E element = elements[start];

		if (null != element) {
			elements[start++] = null;

			if (start >= maxElements) {
				start = 0;
			}
			full = false;
		}
		return element;
	}

	/**
	 * Increments the internal index.
	 * @param index the index to increment
	 * @return the updated index
	 */
	private int increment(int index) {
		if (++index >= maxElements) {
			index = 0;
		}
		return index;
	}

	/**
	 * Decrements the internal index.
	 * @param index the index to decrement
	 * @return the updated index
	 */
	private int decrement(int index) {
		if (--index < 0) {
			index = maxElements - 1;
		}
		return index;
	}

	/**
	 * @return an iterator over this buffer's elements
	 */
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			private int index = start;
			private int lastReturnedIndex = -1;
			private boolean isFirst = full;

			public boolean hasNext() {
				return isFirst || (index != end);
			}

			public E next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				isFirst = false;
				lastReturnedIndex = index;
				index = increment(index);
				return elements[lastReturnedIndex];
			}

			public void remove() {
				if (lastReturnedIndex == -1) {
					throw new IllegalStateException();
				}

				// First element can be removed quickly
				if (lastReturnedIndex == start) {
					BoundedFifoBuffer.this.remove();
					lastReturnedIndex = -1;
					return;
				}

				int pos = lastReturnedIndex + 1;
				if (start < lastReturnedIndex && pos < end) {
					// shift in one part
					System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos);
				} else {
					// Other elements require us to shift the subsequent
					// elements
					while (pos != end) {
						if (pos >= maxElements) {
							elements[pos - 1] = elements[0];
							pos = 0;
						} else {
							elements[decrement(pos)] = elements[pos];
							pos = increment(pos);
						}
					}
				}

				lastReturnedIndex = -1;
				end = decrement(end);
				elements[end] = null;
				full = false;
				index = decrement(index);
			}

		};
	}
	
	public boolean equalsArray( E[] buffer) {
		if( buffer.length!=size())
			return false;
        Iterator<E> it = iterator();
        for (int i = 0; i < size(); i++) {
            if ( !it.hasNext()) // fewer elements than expected
                return false;
            if( it.next() != buffer[i])
            	return false;
        }
		return true;
	}

	public boolean startsWithArray( E[] buffer) {
        Iterator<E> it = iterator();
        for (int i = 0; i < buffer.length; i++) {
            if ( !it.hasNext()) // fewer elements than expected
                return false;
            if( it.next() != buffer[i])
            	return false;
        }
		return true;
	}
	
	public static Byte[] convertByteArray(byte[] b) {
		Byte[] B = new Byte[b.length];
		for (int i = 0; i < b.length; i++) {
			B[i]=b[i];
		}
		return B;
	}
	
	public static void main(String[] args) {
		Byte[] findme = convertByteArray("\nPROPFIND ".getBytes());
		String testStr = "asdasd 324234324 sefsdf sdf PROPFIND 0\n\nPROPFIND repos/JPAGen/!svn/vcc/default HTTP/1.1\nHost: localhost:8081\n";
		byte[] testBytes = testStr.getBytes();
		BoundedFifoBuffer<Byte> bfb = new BoundedFifoBuffer<>(findme.length, true);
		
		for (int i = 0; i < testBytes.length; i++) {
			byte b = testBytes[i];
			bfb.add(b);
			if( bfb.equalsArray(findme)) {
				System.out.println(testStr.substring(i));
			}
		}
		
	}

}
