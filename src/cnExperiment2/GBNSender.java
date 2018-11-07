package cnExperiment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;

public class GBNSender {
	private int SendPort = 10241;
	private int ReceiverPort = 10240;
	private DatagramSocket SenderSocket;
	private DatagramPacket packet;
	private int send_base = 0;
	private int nextseqnum = 0;
	private static int N = 8;
	private boolean flag = false;
	private int timeout = 3000;
	
	private static byte[] B;
	private static String filestring=new String();
	
	/**
	 * 以行为单位读取文件，常用于读面向行的格式化文件
	 * 
	 * @param fileName
	 *            文件名
	 */
	public static void readFileByLines(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			//System.out.println("以行为单位读取文件内容，一次读一整行：");
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			//int line = 1;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				filestring=filestring+tempString+"\r\n";
			}
			reader.close();
		B=filestring.getBytes();
		
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
	
	private byte[] getByteArray(int nextseq) {
		byte[] temp=new byte[1000];
		for (int i=0;i<1000;i++) {
			if (nextseq*1000+i>=B.length) {
				break;
			}
			temp[i]=B[nextseq*1000+i];
		}
		return temp;
	}

	private Timer timer;

	private void timerBegin() {
		if (!flag) {
			flag = true;
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						for (int i = send_base; i < nextseqnum; i++) {
							byte[] tempb=getByteArray(i);
							String temp=new String(tempb);
							String s = new String((i % N) + ":"+temp);
							byte[] data = s.getBytes();
							DatagramPacket SenderPacket = new DatagramPacket(data, data.length,
									InetAddress.getLocalHost(), SendPort);
							SenderSocket.send(SenderPacket);
							System.out.println("重发分组"+i%N);
							//System.out.println(s);
							timerBegin();
						}
					} catch (Exception e) {
					}
				}
			}, timeout);
		} else {
			timer.cancel();
			timer.purge();
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						for (int i = send_base; i < nextseqnum; i++) {
							byte[] tempb=getByteArray(i);
							String temp=new String(tempb);
							String s = new String((i % N) + ":"+temp);
							byte[] data = s.getBytes();
							DatagramPacket SenderPacket = new DatagramPacket(data, data.length,
									InetAddress.getLocalHost(), SendPort);
							SenderSocket.send(SenderPacket);
							System.out.println("重发分组"+i%N);
							timerBegin();
						}
					} catch (Exception e) {
					}
				}
			}, timeout);
		}
	}

	private void timerEnd() {
		if (flag) {
			timer.purge();
			timer.cancel();
			flag = false;
		}
	}

	public GBNSender() {
		readFileByLines("test.txt");
		//System.out.println(B.length);
		try {
			SenderSocket = new DatagramSocket(ReceiverPort);
			while (true) {
				SendToReciver();
				//ReceiveACK();
				System.out.println("aaaaaaa"+nextseqnum);
				TimerReset();
				System.out.println("aaaaaaa"+nextseqnum);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void SendToReciver() {
		try {
			while (nextseqnum < send_base + N) {				
				byte[] tempb=getByteArray(nextseqnum);
				String temp=new String(tempb);
				String s = new String((nextseqnum % N) + ":"+temp);
				byte[] data = s.getBytes();
				packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SendPort);
				//模拟数据包丢失
				if (nextseqnum%5!=0) {
					SenderSocket.send(packet);
					System.out.println("第一次发送分组:"+nextseqnum%N);
				}
				if (send_base == nextseqnum) {
					timerBegin();
				}
				nextseqnum++;
				System.out.println(nextseqnum);
				ReceiveACK();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void ReceiveACK() {
		try {
			byte[] bytes = new byte[100];
			packet = new DatagramPacket(bytes, bytes.length);
			SenderSocket.receive(packet);
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
			if (ack%7==0) {
			System.out.println("ACK" + ack);
			send_base = Math.max((send_base / N) * N + ack + 1,send_base);
			}
			//System.out.println("base="+send_base);
			if (send_base>B.length/1000) {
				System.exit(0);;
			}
		} catch (Exception e) {
		}
	}

	private void TimerReset() {
		if (send_base == nextseqnum) {
			timerEnd();
		} else {
			timerBegin();
		}
		/*
		int x = send_base / N, y = nextseqnum / N;
		int min = Math.min(x, y);
		send_base = send_base - min * N;
		// System.out.println(send_base);
		nextseqnum = nextseqnum - min * N;
		System.out.println(nextseqnum);
		*/
	}

	public static void main(String[] args) {
		new GBNSender();

	}
}
