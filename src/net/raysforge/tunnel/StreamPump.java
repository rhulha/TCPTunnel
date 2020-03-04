package net.raysforge.tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;

import lombok.SneakyThrows;

public class StreamPump extends Thread {

	Byte[] PROPFIND = BoundedFifoBuffer.convertByteArray("\nPROPFIND ".getBytes());
	Byte[] REPORT = BoundedFifoBuffer.convertByteArray("\nREPORT ".getBytes());

	private InputStream is;
	private OutputStream os;
	private OutputStream os2; // optional second os
	private boolean log_stdout=false;
	private boolean log_stderr=false;
	private boolean checkForPropfindError=false;
	BoundedFifoBuffer<Byte> p_bfb = new BoundedFifoBuffer<>(PROPFIND.length, true);
	BoundedFifoBuffer<Byte> r_bfb = new BoundedFifoBuffer<>(REPORT.length, true);

	public StreamPump(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}

	public StreamPump(InputStream is, OutputStream os, OutputStream os2) {
		this.is = is;
		this.os = os;
		this.os2 = os2;
	}

	private void write(int b) throws IOException {
		os.write(b);
		if(os2!=null)
		{
			os2.write(b);
		}
		if (log_stdout)
			//System.out.write( (Integer.toHexString(b)+"\n").getBytes());
			System.out.write( b);
		if (log_stderr)
			//System.out.write( (Integer.toHexString(b)+"\n").getBytes());
			System.err.write( b);
	}

	@Override
	@SneakyThrows
	public void run() {
		int read;
		try {
			while ((read = is.read()) != -1) {
				if (checkForPropfindError) {
					if ((p_bfb.startsWithArray(PROPFIND) || r_bfb.startsWithArray(REPORT)) && read != '/') {
						write('/');
					}
				}
				write(read);

				if (checkForPropfindError) {
					// add to the buffer later so we can
					// peak at the next read and still
					// compare the string
					p_bfb.add((byte) read);
					r_bfb.add((byte) read);
				}

			}
			is.close();
			os.close();
		} catch (IOException e) {
			try {
				is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				os.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
}
