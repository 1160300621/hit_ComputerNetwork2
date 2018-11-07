package cnExperiment2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class GBNReciver {
	private int SendPort = 10240;
	private int ReceiverPort=10241;
	private DatagramSocket ReciverSocket;
	private DatagramPacket packet;
	private int expectedSeqNum = 0;
	private int N=8;
	
	public GBNReciver() {
		try {
			ReciverSocket = new DatagramSocket(ReceiverPort);
			while (true) {
				byte[] data = new byte[1472];
				packet = new DatagramPacket(data, data.length);
				ReciverSocket.receive(packet);
				byte[] d = packet.getData();
				String message = new String(d);
				String num =new String();
				for (int i=0;i<message.length();i++) {
					if (message.charAt(i)<='9'&&message.charAt(i)>='0') {
						num=num+message.charAt(i);
					}
					else {
						break;
					}
				}
				//System.out.println(num);
				if (expectedSeqNum == Integer.valueOf(num)) {
					int ack = expectedSeqNum;
					sendACKback(ack);
					expectedSeqNum=(expectedSeqNum+1)%N;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendACKback(int ack) {
		try {
			ackFrame ACK = new ackFrame(ack);
			packet = new DatagramPacket(ACK.ackByte, ACK.ackByte.length, InetAddress.getLocalHost(), SendPort);
			ReciverSocket.send(packet);
			System.out.println("Send ACK"+ack+" Back");
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		new GBNReciver();
	}

}
