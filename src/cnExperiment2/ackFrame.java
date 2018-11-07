package cnExperiment2;

public class ackFrame {
	protected int ACK;
	protected String ack;
	protected byte[] ackByte;
	
	public ackFrame(int ACK) {
		this.ACK=ACK;
		ack=String.valueOf(ACK);
		ackByte=ack.getBytes();
	}
	
	
}
