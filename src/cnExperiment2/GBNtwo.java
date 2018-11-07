package cnExperiment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GBNtwo implements Runnable{
	private static int N = 8;
	private int choice;

	public GBNtwo(int choice) {
		super();
		this.choice = choice;
	}

	// �������ݲ���
	private static int SendAckPort = 10240;
	private static int ReceiveDataPort = 10241;
	private static DatagramSocket ReciverSocket;
	private static DatagramPacket SendAckPacket;
	private static int expectedSeqNum = 0;
	
	private static int last=-1;
	/**
	 * �������ݲ�����ACK
	 */
	public void receive() {
		try {
			ReciverSocket = new DatagramSocket(ReceiveDataPort);
			while (true) {
				byte[] data = new byte[1472];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				ReciverSocket.receive(packet);
				byte[] d = packet.getData();
				String message = new String(d);
				String num = new String();
				for (int i = 0; i < message.length(); i++) {
					if (message.charAt(i) <= '9' && message.charAt(i) >= '0') {
						num = num + message.charAt(i);
					} else {
						break;
					}
				}
				// �ж��Ƿ���˳�򵽴��
				if (expectedSeqNum == Integer.valueOf(num)) {
					int ack = expectedSeqNum;
					sendACKback(ack);
					expectedSeqNum = (expectedSeqNum + 1)%seqnum;
					last=ack;
				}
				else {
					if (last>=0) {
						sendACKback(last);
					}
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * ����ACK
	 * 
	 * @param ack
	 *            ���ص�ACK��ţ�Ϊ0��N-1
	 */
	private void sendACKback(int ack) {
		try {
			ackFrame ACK = new ackFrame(ack);
			SendAckPacket = new DatagramPacket(ACK.ackByte, ACK.ackByte.length, InetAddress.getLocalHost(),
					SendAckPort);
			ReciverSocket.send(SendAckPacket);
			System.out.println("Send ACK" + ack + " Back");
		} catch (Exception e) {
		}
	}

	// �������ݲ���
	private static int seqnum=15;
	private static int SendDataPort = 10242;
	private static int ReceiveAckPort = 10243;
	private static DatagramSocket SenderSocket;
	private static DatagramPacket SendDataPacket;
	private static DatagramSocket ReceiveAckSocket;
	private static DatagramPacket ReceiverAckPacket;
	private static int send_base = 0;
	private static int nextseqnum = 0;
	private static boolean flag = false;
	private static int timeout = 2;
	private static String filestring = new String();
	private static byte[] B;
	private static int team;
	
	private ScheduledExecutorService executor;
	
	/**
	 * ��ʼ��ʱ�������¼�ʱ����ʱʱ��Ϊ2s
	 */
	private void timerBegin() {
		TimerTask task=new TimerTask() {
			@Override
			public void run() {
				if (send_base>= Math.ceil(B.length / 1478)-1) {
					return;
				}
				try {
					for (int i = send_base; i < nextseqnum; i++) {
						byte[] tempb = getByteArray(i);
						String temp = new String(tempb);
						String s = new String(i%seqnum + ":" + temp);
						byte[] data = s.getBytes();
						DatagramPacket SenderPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(),
								SendDataPort);
						SenderSocket.send(SenderPacket);
						System.out.println("�ط�����:" + i%seqnum+" �ط����ǵ�"+i+"����");
						timerBegin();
					}
				} catch (Exception e) {
				}
			}
		};
		
		if (!flag) {
			flag = true;
		} else {
			executor.shutdown();
			//timer.cancel();
			//timer.purge();
		}
		executor=Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(task, timeout, timeout, TimeUnit.SECONDS);
		
	}

	/**
	 * ������ʱ
	 */
	private void timerEnd() {
		if (flag) {
			//timer.purge();
			//timer.cancel();
			executor.shutdown();
			flag = false;
		}
	}

	/**
	 * ��ȡ�ļ�������filestring���ֽ�����B��
	 * 
	 * @param fileName
	 */
	public static void readFileByLines(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				filestring = filestring + tempString + "\r\n";
			}
			reader.close();
			B = filestring.getBytes();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	/**
	 * ����nextseqnum���Ҫ������ֽ�
	 * 
	 * @param nextseqnum
	 *            Ҫ����ķ������
	 * @return �ֽ����飬Ҫ������ֽ�
	 */
	private byte[] getByteArray(int nextseqnum) {
		byte[] temp = new byte[1478];
		for (int i = 0; i < 1478; i++) {
			if (nextseqnum * 1478 + i >= B.length) {
				break;
			}
			temp[i] = B[nextseqnum * 1478 + i];
		}
		return temp;
	}

	/**
	 * ��������
	 */
	public void send() {
		readFileByLines("test.txt");
		team=(int) Math.ceil(B.length/1478);
		try {
			SenderSocket = new DatagramSocket();
			ReceiveAckSocket = new DatagramSocket(ReceiveAckPort);
			while (true) {
				SendToReciver();
				ReceiveACK();
				if (send_base >= team) {
					break;
				}
			}
			System.out.println("Send Over");
		} catch (Exception e) {
		}
	}

	/**
	 * �������ݸ����շ�
	 */
	private void SendToReciver() {
		try {
			while (nextseqnum < send_base + N) {
				if (send_base>= team||nextseqnum>=team) {
					break;
				}
				byte[] tempb = getByteArray(nextseqnum);
				String temp = new String(tempb);
				String s = new String(nextseqnum%seqnum + ":" + temp);
				byte[] data = s.getBytes();
				SendDataPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SendDataPort);
				// ģ�����ݰ���ʧ
				if (nextseqnum % 5 != 0) {
					SenderSocket.send(SendDataPacket);
					System.out.println("���ͷ���:" + nextseqnum%seqnum+" ���͵��ǵ�"+nextseqnum+"����");
				} else {
					System.out.println("ģ�����" + nextseqnum%seqnum + "��ʧ"+" ��ʧ���ǵ�"+nextseqnum+"����");
				}
				if (send_base == nextseqnum) {
					timerBegin();
				}
				nextseqnum++;
				
				// System.out.println(nextseqnum);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ����ACK
	 * @throws InterruptedException 
	 */
	private void ReceiveACK() throws InterruptedException {
		try {
			if (send_base>=team) {
				return;
			}
			byte[] bytes = new byte[10];
			ReceiverAckPacket = new DatagramPacket(bytes, bytes.length);
			ReceiveAckSocket.receive(ReceiverAckPacket);
			String ackString = new String(bytes, 0, bytes.length);
			String acknum = new String();
			for (int i = 0; i < ackString.length(); i++) {
				if (ackString.charAt(i) >= '0' && ackString.charAt(i) <= '9') {
					acknum += ackString.charAt(i);
				} else {
					break;
				}
			}
			int ack = Integer.parseInt(acknum);
			// ģ��ACK����
			if (ack % 6 != 0) {
				System.out.println("ACK" + ack);
				//send_base = Math.max(ack + 1, send_base);
				int m;
				//System.out.println(send_base);
				if (send_base%seqnum>ack&&nextseqnum/seqnum>send_base/seqnum&&ack<=(send_base+N)%N) {
					m=send_base/seqnum*seqnum+ack+seqnum+1;
				}
				else {
					m=send_base/seqnum*seqnum+ack+1;
				}
				send_base = Math.max(send_base, m);
			} else {
				System.out.println("ģ��ACK" + ack + "��ʧ");
			}
			TimerReset();
			//System.out.println("base="+send_base);
			//System.out.println("next_seq="+nextseqnum);
		} catch (IOException e) {
		}
	}

	/**
	 * ���ü�ʱ��
	 */
	private void TimerReset() {
		if (send_base == nextseqnum) {
			timerEnd();
		} else {
			timerBegin();
		}
	}
	
	@Override
	public void run() {
		if (choice==0) {
			receive();
		}
		else if (choice==1){
			send();
		}
	}

	public static void main(String[] args) {
		new Thread(new GBNtwo(0)).start();
		new Thread(new GBNtwo(1)).start();
	}


}
