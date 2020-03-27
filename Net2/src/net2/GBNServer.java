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
			//��������˷�������
			sendData();
			//�ӷ������˽���ACK
			byte[] bytes = new byte[4096];
			datagramPacket = new DatagramPacket(bytes, bytes.length);
			datagramSocket.receive(datagramPacket);
			String fromServer = new String(bytes, 0, bytes.length);
			int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ack:")+4).trim());
			base = ack+1;
			if(base == nextSeq){ //���ڵ�һλ����һ��Ҫ���͵�֡�غ� �ȴ�sendData��������ʱ��
				//ֹͣ��ʱ��
				tm.setTime(0);
			}else {
				//��ʼ��ʱ�� 3����������ݶ�ʧ
				tm.setTime(3);
			}
			System.out.println("�ӿͻ��˻�õ�����:" + fromServer);
			System.out.println("\n");
		}

	}

	public static void main(String[] args) throws Exception {
		gbnServer = new GBNServer();

	}

	/**
	 * ���������������
	 *
	 * @throws Exception
	 */
	private void sendData() throws Exception {
		inetAddress = InetAddress.getLocalHost();
		while (nextSeq < base + N && nextSeq <= 10) {
			//�������Ϊ3�����ݣ�ģ�����ݶ�ʧ
			if(nextSeq == 3) {
				nextSeq++;
				continue;
			}

			String clientData = "���������͵����ݱ��:" + nextSeq;
			System.out.println("��ͻ��˷��͵�����:"+nextSeq);

			byte[] data = clientData.getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
			datagramSocket.send(datagramPacket);

			if(nextSeq == base){
				//��ʼ��ʱ
				tm.setTime(3);
			}
			nextSeq++;
		}
	}

	/**
	 * ��ʱ�����ش�
	 */
	public void timeOut() throws Exception {
		for(int i = base;i < nextSeq;i++){
			String clientData = "���������·��͵����ݱ��:" + i;
			System.out.println("��ͻ������·��͵�����:" + i);
			byte[] data = clientData.getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
			datagramSocket.send(datagramPacket);
		}
	}
}
