package startMon.Runnable;

import java.sql.SQLException;

import startMon.Detail.InDetailServer;
import startMon.Status.IpStatusConfig;

public class SearchServer implements Runnable {
	// ����͸� ����� ���� ��ü
	private IpStatusConfig ipInfo;

	public SearchServer(IpStatusConfig ipInfo) {
		this.ipInfo = ipInfo;
	}

	@Override
	public void run() {
		long threadId = Thread.currentThread().getId();
		try {
			nextCheck(threadId);
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
		}

	}

	synchronized void nextCheck(long threadId) throws InterruptedException, SQLException {
		InDetailServer inDetails = new InDetailServer(threadId, ipInfo);
		inDetails.detailCheck();
	}
}