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

public class SRone implements Runnable{

	private static int seqnum = 16;
	private int choice;

	public SRone(int choice) {
		super();
		this.choice = choice;
	}

	// 发送数据部分
	private static int N = 8;
	private static int SendDataPort = 10241;
	private static int ReceiveAckPort = 10240;
	private static DatagramSocket SenderSocket;
	private static DatagramPacket SendDataPacket;
	private static DatagramSocket ReceiveAckSocket;
	private static DatagramPacket ReceiverAckPacket;
	private static int send_base = 0;
	private static int nextseqnum = 0;
	private static int timeout = 4;
	private static String filestring = new String();
	private static byte[] B;
	private static int team;
	private static boolean[] ackarray = new boolean[N];
	private static boolean[] flags = new boolean[N];
	private static ScheduledExecutorService[] executors = new ScheduledExecutorService[N];

	/**
	 * 开始计时或者重新计时，超时时间为2s
	 */
	private void timerBegin(int q, int x) {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (send_base >= Math.ceil(B.length / 1478) - 1) {
					return;
				}
				try {
					byte[] tempb = getByteArray(x);
					String temp = new String(tempb);
					String s = new String(x % seqnum + ":" + temp);
					byte[] data = s.getBytes();
					DatagramPacket SenderPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(),
							SendDataPort);
					SenderSocket.send(SenderPacket);
					System.out.println("重发分组:" + x % seqnum + " 重发的是第" + x + "个包");
				} catch (Exception e) {
				}
			}
		};
		if (!flags[q]) {
			flags[q] = true;
		} else {
			executors[q].shutdown();
		}
		executors[q] = Executors.newSingleThreadScheduledExecutor();
		executors[q].scheduleWithFixedDelay(task, timeout, timeout, TimeUnit.SECONDS);
	}

	/**
	 * 结束计时
	 */
	private void timerEnd(int q, int x) {
		/*
		 * if (flag) { //timer.purge(); //timer.cancel(); executors[q].shutdown(); flag
		 * = false; }
		 */
		if (flags[q]) {
			flags[q] = false;
			executors[q].shutdown();
		}
	}

	/**
	 * 读取文件，存入filestring和字节数组B中
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
	 * 根据nextseqnum获得要传输的字节
	 * 
	 * @param nextseqnum
	 *            要传输的分组序号
	 * @return 字节数组，要传输的字节
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
	 * 发送数据
	 */
	public void send() {
		readFileByLines("test.txt");
		team = (int) Math.ceil(B.length / 1478);
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
	 * 发送数据给接收方
	 */
	private void SendToReciver() {
		try {
			while (nextseqnum < send_base + N) {
				if (send_base >= team || nextseqnum >= team) {
					break;
				}
				byte[] tempb = getByteArray(nextseqnum);
				String temp = new String(tempb);
				String s = new String(nextseqnum % seqnum + ":" + temp);
				byte[] data = s.getBytes();
				SendDataPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SendDataPort);
				// 模拟数据包丢失
				if (nextseqnum % 5 != 0) {
					SenderSocket.send(SendDataPacket);
					System.out.println("发送分组:" + nextseqnum % seqnum + " 发送的是第" + nextseqnum + "个包");
				} else {
					System.out.println("模拟分组" + nextseqnum % seqnum + "丢失" + " 丢失的是第" + nextseqnum + "个包");
				}
				System.out.println("nextseqnum=" + nextseqnum);
				System.out.println("send_base=" + send_base);
				timerBegin(nextseqnum - send_base, nextseqnum);
				/*
				 * if (send_base == nextseqnum) { timerBegin(); }
				 */
				nextseqnum++;

				// System.out.println(nextseqnum);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 接收ACK
	 * 
	 * @throws InterruptedException
	 */
	private void ReceiveACK() throws InterruptedException {
		try {
			if (send_base >= team) {
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
			// 模拟ACK丢包
			// if (ack % 6 != 0) {
			System.out.println("ACK" + ack);
			int a = ack, b = ack + seqnum;
			while (!(send_base >= a && send_base <= b)) {
				a += seqnum;
				b += seqnum;
			}
			if (b - send_base > send_base - a) {
				ack = a;
			} else {
				ack = b;
			}
			System.out.println("ack=" + ack);
			if (ack >= send_base && ack < send_base + N) {
				// sendACKback(rev_num);
				ackarray[ack - send_base] = true;
				timerEnd(ack - send_base, ack);
				if (ack == send_base) {
					ackarray[0] = true;
					int cnt = 0;
					for (int i = 0; i < N; i++) {
						if (ackarray[i]) {
							cnt++;
						} else {
							break;
						}
					}
					System.out.println("cnt=" + cnt);
					for (int i = 0; i < N - cnt; i++) {
						ackarray[i] = ackarray[i + cnt];
						flags[i] = flags[i + cnt];
						executors[i] = executors[i + cnt];
					}
					for (int i = N - cnt; i < N; i++) {
						ackarray[i] = false;
						executors[i] = null;
						flags[i] = false;
						// timerEnd(i, ack);
						// executors[i];
						// flags[i]=false;
					}
					send_base = send_base + cnt;
				}
			}
			// } else {
			// System.out.println("模拟ACK" + ack + "丢失");
			// }
		} catch (IOException e) {
		}
		
		
		
	}

	// 接收数据部分
		private static int SendAckPort = 10243;
		private static int ReceiveDataPort = 10242;
		private static DatagramSocket ReciverSocket;
		private static DatagramPacket SendAckPacket;
		private static int RecWinSize=5;
		private static int rev_base=0;
		private static boolean ack[]=new boolean[RecWinSize];
		
		/**
		 * 接收数据并发回ACK
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
					int rev_num=Integer.valueOf(num);
					int a=rev_num,b=rev_num+seqnum;
					while (!(rev_base>=a&&rev_base<=b)) {
						a+=seqnum;
						b+=seqnum;
					}
					if (b-rev_base>rev_base-a) {
						rev_num=a;
					}
					else {
						rev_num=b;
					}
					if (rev_num>=rev_base&&rev_num<rev_base+RecWinSize) {
						sendACKback(rev_num);
						if (rev_num==rev_base) {
							ack[0]=true;
							int cnt=0;
							for (int i=0;i<RecWinSize;i++) {
								if (ack[i]) {
									cnt++;
								}
								else {
									break;
								}
							}
							for (int i=0;i<RecWinSize-cnt;i++) {
								ack[i]=ack[i+cnt];
							}
							for (int i=RecWinSize-cnt;i<RecWinSize;i++) {
								ack[i]=false;
							}
							rev_base=rev_base+cnt;
						}
						else {
							ack[rev_num-rev_base]=true;
						}
					}
					else if (rev_num<rev_base&&rev_num>=rev_base-RecWinSize) {
						sendACKback(rev_num);
					}
				}
			} catch (Exception e) {
			}
		}

		/**
		 * 发回ACK
		 * 
		 * @param ack
		 *            发回的ACK序号，为0到N-1
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
		
		@Override
		public void run() {
			if (choice==0) {
				send();
			}
			else if (choice==1){
				receive();
			}
		}
	
	public static void main(String[] args) {
		new Thread(new SRone(0)).start();
		new Thread(new SRone(1)).start();
	}


}
