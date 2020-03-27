package net2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class GBNClient {
	private final int port = 80;
	private DatagramSocket datagramSocket;
	private DatagramPacket datagramPacket;
	private int exceptedSeq = 1;

	public GBNClient() throws IOException {

		try {
			datagramSocket = new DatagramSocket(port);
			while (true) {
				byte[] receivedData = new byte[4096];
				datagramPacket = new DatagramPacket(receivedData, receivedData.length);
				datagramSocket.receive(datagramPacket);
				//收到的数据
				String received = new String(receivedData, 0, receivedData.length);//offset是初始偏移量
				System.out.println(received);
				//收到了预期的数据
				if (Integer.parseInt(received.substring(received.indexOf("编号:") + 3).trim()) == exceptedSeq) {
					//发送ack
					sendAck(exceptedSeq);
					System.out.println("客户端期待的数据编号:" + exceptedSeq);
					//期待值加1
					exceptedSeq++;
					System.out.println('\n');
				} else {
					System.out.println("客户端期待的数据编号:" + exceptedSeq);
					System.out.println("+++++++++++++++++++++未收到预期数据+++++++++++++++++++++");
					//仍发送之前的ack
					sendAck(exceptedSeq - 1);
					System.out.println('\n');
				}
			}
		}catch(SocketException e){
			e.printStackTrace();
		}
	}

	public static final void main(String[] args) throws IOException {
		new GBNClient();
	}

	//向客户端发送ack
	public void sendAck(int ack) throws IOException {
		String response = " ack:"+ack;
		byte[] responseData = response.getBytes();
		InetAddress responseAddress = datagramPacket.getAddress();
		int responsePort = datagramPacket.getPort();
		datagramPacket = new DatagramPacket(responseData,responseData.length,responseAddress,responsePort);
		datagramSocket.send(datagramPacket);
	}
}
