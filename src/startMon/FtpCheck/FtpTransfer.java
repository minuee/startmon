/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package startMon.FtpCheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

import startMon.StartMonitor;

/**
 * @author kgrid-dev-pinoto
 */
@SuppressWarnings("unused")
public class FtpTransfer {
	private final String ftpAddress;
	private final int ftpPort;
	private final String ftpUser;
	private final String ftpPass;
	private final SocketAddress address;

	private Socket ftpSocket = null;
	private OutputStreamWriter outputStreamWriter = null;
	private InputStreamReader inputStreamReader = null;
	private boolean isConnected = false;
	private Socket dataSocket = null;
	private Thread tRecv = null;

	private static int SOCKET_READ_BUFFER_SIZE = 128;
	private static int DATA_READ_BUFFER_SIZE = 16;
	private static int CONNECT_TIMEOUT = 10000;
	private static int SO_TIMEOUT = 10000;
	
	public FtpTransfer(String ftpAddress, int ftpPort, String ftpUser, String ftpPass) {
		this.ftpAddress = ftpAddress;
		this.ftpPort = ftpPort;
		this.ftpUser = ftpUser;
		this.ftpPass = ftpPass;
		this.address = new InetSocketAddress(ftpAddress, ftpPort);
	}

	public void setConnectTimeOut(int msec) {
		CONNECT_TIMEOUT = msec;
	}
	
	public void setSoTimeOut(int msec) {
		SO_TIMEOUT = msec;
	}

	public void connect() throws IOException {
		if (ftpSocket == null) {
			ftpSocket = new Socket();
		}

		if (ftpSocket.isConnected()) {
			StartMonitor.logger.info("connect - already connected");
			return;
		}

		if (isConnected) {
			StartMonitor.logger.info("connect - already logined");
			return;
		}
		
		ftpSocket.connect(address, CONNECT_TIMEOUT);
		ftpSocket.setSoTimeout(SO_TIMEOUT);
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - connect to " + ftpAddress);
		}

		// Login
		outputStreamWriter = new OutputStreamWriter(ftpSocket.getOutputStream());
		inputStreamReader = new InputStreamReader(ftpSocket.getInputStream());

		StringBuilder sb = new StringBuilder();
		char[] recv = new char[SOCKET_READ_BUFFER_SIZE];
		
		Arrays.fill(recv, ' ');
		inputStreamReader.read(recv);
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - recv: " + sb.toString().trim());
		}

		String sCmd = "USER " + ftpUser + "\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();

		Arrays.fill(recv, ' ');
		inputStreamReader.read(recv);
		sb.setLength(0);
		sb.append(recv);
		// ex) sb = "331 User name okay, need password for testuser.";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - recv: " + sb.toString().trim());
		}
		if (!sb.toString().startsWith("331")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}
		
		sCmd = "PASS " + ftpPass + "\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();

		Arrays.fill(recv, ' ');
		inputStreamReader.read(recv);
		sb.setLength(0);
		// ex) sb = "230 User logged in, proceed.";
		sb.append(recv);
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("connect - recv: " + sb.toString().trim());
		}
		
		if (!sb.toString().startsWith("230")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}
		// Login

		isConnected = true;
	}

	public void close() throws IOException {
		if (null == ftpSocket) {
			return;
		}

		if (null != dataSocket && dataSocket.isConnected()) {
			dataSocket.close();
		}

		if (isConnected) {
			outputStreamWriter.close();
			inputStreamReader.close();
			isConnected = false;
		}

		if (ftpSocket.isConnected()) {
			ftpSocket.close();
		}

		ftpSocket = null;
	}

	public long downloadFile(String fileName, long pos, long size, OutputStream outFile) throws IOException {
		if (!ftpSocket.isConnected()) {
			throw new IOException("downloadFile - Not connected");
		}

		if (! isConnected) {
			throw new IOException("downloadFile - Not Logined");
		}

		while (tRecv != null && tRecv.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
		}

		StringBuilder sb = new StringBuilder();
		char[] recv = new char[SOCKET_READ_BUFFER_SIZE];

		String sCmd = "TYPE I\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();
		
		Arrays.fill(recv, ' ');
		inputStreamReader.read(recv);
		// ex) sb = "200 Command TYPE okay.";
		sb.append(recv);
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - recv: " + sb.toString().trim());
		}
		
		if (!sb.toString().startsWith("200")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}
		
		sCmd = "PASV\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();

		Arrays.fill(recv, ' ');
		sb.setLength(0);
		inputStreamReader.read(recv);
		// ex) sb = "227 Entering Passive Mode (121,78,239,242,24,148)";
		sb.append(recv);
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - recv: " + sb.toString().trim());
		}
		
		if (!sb.toString().startsWith("227")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}
		
		int s = sb.toString().lastIndexOf("(");
		int e = sb.toString().lastIndexOf(")");
		
		// ex) pasv = "121,78,239,242,24,148";
		String pasv = sb.toString().substring(s + 1, e);
		String[] aryPasv = pasv.split(",");
		String pasvIp = "";
		for (int i = 0 ; i < 4 ; ++i) {
			if (!pasvIp.isEmpty()) {
				pasvIp += ".";
			}

			pasvIp += aryPasv[i].trim();
		}
		
		// ex) pasvIp = "121.78.239.242";
		// ex) pasvPort = 6292;
		int pasvPort = Integer.parseInt(aryPasv[4].trim()) * 256 + Integer.parseInt(aryPasv[5].trim());
		SocketAddress pasvAddress = new InetSocketAddress(pasvIp, pasvPort);

		sCmd = "REST " + Long.toString(pos) + "\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();

		Arrays.fill(recv, ' ');
		sb.setLength(0);
		inputStreamReader.read(recv);
		// ex) sb = "350 Restarting at 0. Send STORE or RETRIEVE to initiate transfer.";
		sb.append(recv);
		
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - recv: " + sb.toString().trim());
		}
		if (! sb.toString().startsWith("350")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}

		sCmd = "RETR " + fileName + "\r\n";
		if (StartMonitor.FTP_SOCKET_LOG) {
			StartMonitor.logger.info("downloadFile - send: " + sCmd.trim());
		}
		outputStreamWriter.write(sCmd);
		outputStreamWriter.flush();

		/* ??????? Unknown code...
		Arrays.fill(recv, ' ');
		sb.setLength(0);
		isr.read(recv);
		sb.append(recv);
		if (! sb.toString().startsWith("150")) {
			throw new IOException("Receive error signal : " + sb.toString());
		}
		
//		LOG.info("recv from [" + sb.toString().trim() + "]");
//		System.out.println(sb.toString().trim());
//		recvReady(inputStreamReader);
		*/

		recvReady(inputStreamReader);
		
		// File Download from Data Socket
		dataSocket = new Socket();
		dataSocket.connect(pasvAddress, CONNECT_TIMEOUT);
		dataSocket.setSoTimeout(SO_TIMEOUT);
		InputStream inputStream = dataSocket.getInputStream();

		byte[] buff = new byte[DATA_READ_BUFFER_SIZE];
		long upSize = 0;
		
		while (true) {
			int readBytes = inputStream.read(buff);
			if (0 >= readBytes) {
				break;
			}
			
			outFile.write(buff, 0, readBytes);
			upSize += readBytes;
		}
		
		dataSocket.close();
		return upSize;
	}

	public void recvReady(final InputStreamReader isr) {
		while (null != tRecv && tRecv.isAlive()) {
			try {
				StartMonitor.logger.info("recvReady - tRecv is alive... waiting");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Do Nothing...
			}
		}

		tRecv = new Thread() {

			@Override
			public void run() {
				char[] recv = new char[SOCKET_READ_BUFFER_SIZE];
				StringBuilder sb = new StringBuilder();
				
				try {
					while (true) {
						Arrays.fill(recv, ' ');
						isr.read(recv);
						sb.setLength(0);
						sb.append(recv);

						if (StartMonitor.FTP_SOCKET_LOG) {
							StartMonitor.logger.info("recvReady - recv: " + sb.toString().trim());
						}
						if (sb.toString().trim().equals("")) {
							break;
						}

						if (sb.toString().startsWith("226")) {
							break;
						}
					}
				} catch (IOException e) {
					/*
					StringWriter errors = new StringWriter();
		            e.printStackTrace(new PrintWriter(errors));
					StartMonitor.logger.info("tRecv - exception: " + errors.toString());
					*/
				}
			}
		};

		tRecv.start();
	}
}
