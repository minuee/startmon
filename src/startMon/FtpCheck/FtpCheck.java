package startMon.FtpCheck;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;

import startMon.StartMonitor;

public class FtpCheck {
	private FTPClient client = null;
	private String host;

	public boolean init(String host, String userName, String password, int port) {
		this.host = host;
		client = new FTPClient();
		client.setControlEncoding("euc-kr");
		client.setRemoteVerificationEnabled(false);

		FTPClientConfig config = new FTPClientConfig();
		client.configure(config);

		try {
			client.connect(host, port);
			client.login(userName, password);
			return true;
		} catch (SocketException e) {
			StartMonitor.logger.debug(host + " - ftp: " + e.getMessage());
		} catch (IOException e) {
			StartMonitor.logger.debug(host + " - ftp: " + e.getMessage());
		}

		return false;
	}

	public boolean download(String dir, String downloadFileName, String path) {
		FileOutputStream out = null;
		InputStream in = null;
		dir += downloadFileName;
		
		try {
			in = client.retrieveFileStream(dir);
			out = new FileOutputStream(new File(path));
			int i;

			byte[] buff = new byte[4096];
			long upSize = 0;

			while ((i = in.read()) != -1) {
				int readBytes = in.read(buff);
				out.write(i);

				upSize += readBytes;
			}

			StartMonitor.logger.debug(upSize);

			return true;
		} catch (IOException e) {
			StartMonitor.logger.debug(host + " - ftp: " + e.getMessage());
			return false;
		} finally {
			try {
				if (in != null) {
					in.close();
					in = null;
				}
				
				if (out != null) {
					out.close();
					out = null;
				}
			} catch (IOException e) {
				StartMonitor.logger.debug(host + " - ftp: " + e.getMessage());
				return false;
			}
		}
	}

	public void disconnection() {
		try {
			client.logout();
			
			if (client.isConnected()) {
				client.disconnect();
				client = null;
			}
		} catch (IOException e) {
			StartMonitor.logger.debug(host + " - ftp: " + e.getMessage());
		}
	}
}
