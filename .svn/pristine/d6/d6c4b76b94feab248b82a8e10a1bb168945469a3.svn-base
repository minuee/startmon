package startMon.Detail;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class GetLoad {
	
	@SuppressWarnings("rawtypes")
	String getSnmpLoad(String ip) throws IOException {
		PDU pdu = new PDU();

		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2021.10.1.3.1")));
		pdu.setType(PDU.GETNEXT);

		CommunityTarget target = new CommunityTarget();
		UdpAddress targetAddress = new UdpAddress();
		targetAddress.setInetAddress(InetAddress.getByName(ip));
		targetAddress.setPort(161);
		target.setAddress(targetAddress);
		target.setCommunity(new OctetString("kgdevicecomm"));
		target.setVersion(SnmpConstants.version2c);

		Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
		snmp.listen();
		ResponseEvent response = snmp.send(pdu, target);

		if (response.getResponse() == null) {
			return "ERROR";
		} else {
			Vector variableBindings = response.getResponse().getVariableBindings();
			for (int i = 0; i < variableBindings.size();) {
				return variableBindings.get(i).toString();
			}
		}

		snmp.close();
		return "ERROR";
	}
}
