package net2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GBNServer {
	// aim port = 80 inetAddress aim
	private final int port = 80;
	private DatagramSocket datagramSocket = new DatagramSocket();
	private DatagramPacket datagramPacket;
	private InetAddress inetAddress;
	private Time tm;
	private static GBNServer gbnServer;
	private Timer timer;
	private int nextSeq = 1;
	private int base = 1;
	private int N = 5;


	public GBNServer() throws Exception {
		tm = new Time();
		timer = new Timer(this, tm);
		tm.setTime(0);
		timer.start();
		while(true){
			//向服务器端发送数据
			sendData();
			//从服务器端接受ACK
			byte[] bytes = new byte[4096];
			datagramPacket = new DatagramPacket(bytes, bytes.length);
			datagramSocket.receive(datagramPacket);
			String fromServer = new String(bytes, 0, bytes.length);
			int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ack:")+4).trim());
			base = ack+1;
			if(base == nextSeq){ //窗口第一位和下一个要发送的帧重合 等待sendData中启动计时器
				//停止计时器
				tm.setTime(0);
			}else {
				//开始计时器 3秒后有无数据丢失
				tm.setTime(3);
			}
			System.out.println("从客户端获得的数据:" + fromServer);
			System.out.println("\n");
		}

	}

	public static void main(String[] args) throws Exception {
		gbnServer = new GBNServer();

	}

	/**
	 * 向服务器发送数据
	 *
	 * @throws Exception
	 */
	private void sendData() throws Exception {
		inetAddress = InetAddress.getLocalHost();
		while (nextSeq < base + N && nextSeq <= 10) {
			//不发编号为3的数据，模拟数据丢失
			if(nextSeq == 3) {
				nextSeq++;
				continue;
			}

			String clientData = "服务器发送的数据编号:" + nextSeq;
			System.out.println("向客户端发送的数据:"+nextSeq);

			byte[] data = clientData.getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
			datagramSocket.send(datagramPacket);

			if(nextSeq == base){
				//开始计时
				tm.setTime(3);
			}
			nextSeq++;
		}
	}

	/**
	 * 超时数据重传
	 */
	public void timeOut() throws Exception {
		for(int i = base;i < nextSeq;i++){
			String clientData = "服务器重新发送的数据编号:" + i;
			System.out.println("向客户端重新发送的数据:" + i);
			byte[] data = clientData.getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
			datagramSocket.send(datagramPacket);
		}
	}
}
