package net1;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class OneWayServer extends Thread{
	public static int MAX_LENGTH = 8;	//ÿ�ζ�ȡ�ļ��ĵ�����ֽ���
	public static final int TIMEOUT = 3000; //���ó�ʱʱ��	 
	public static byte[] receive = new byte[MAX_LENGTH]; //��������
	public static DatagramSocket socket; //socket
	public static InputStream inputFile = null; //�����ȡ��������
	public static byte order; //˳��
	public static InetAddress inetAddress;
	public static int port; //�˿ں�

	public OneWayServer () {
		try { 
			socket = new DatagramSocket();
			socket.setSoTimeout(TIMEOUT); //��ʱʱ����
			inputFile = new FileInputStream("read.txt"); //��ȡread.txt
			order = 0;	//�տ�ʼ���͵���0���ݰ�
			inetAddress =  InetAddress.getByName("localhost"); //����������ַ
			port = 9000; //�˿ں�����
			start();
		} catch (SocketException | FileNotFoundException e) { 
			e.printStackTrace();
		} catch (UnknownHostException e) { 
			e.printStackTrace();
		}	 
	}
	@Override
	public void run() {
		int timeOut = 1;
		int count = 0; 
		//�������ݰ�
		while(true) {  
			int len;
			try { 
				byte[] sendata = new byte[MAX_LENGTH];
				sendata[0] = order; 
				len = inputFile.read(sendata,1,sendata.length - 1);
				count++;
				// System.out.println(len);
				if(len == -1) { //�ļ��Ѿ��������
					System.out.println("�ļ��Ѿ��������");
					break;
				}

				while(len != -1) { 
					try{
						System.out.println("���͵�"+count+"�����ݱ�");
						DatagramPacket packet = new DatagramPacket(sendata,len + 1,inetAddress,port); //����������ַ��host(server)
						//socket.send(packet);

						if(timeOut-- <= 0){
							socket.send(packet);
						} else {
							SocketTimeoutException e = new SocketTimeoutException();
							throw e;
						}


						DatagramPacket packet2 = new DatagramPacket(receive,receive.length);
						socket.receive(packet2);
						byte ack = receive[0];
						System.out.println("����ȥ�İ��ǣ�"+order+"��������ACK�ǣ�"+ack);
						if(ack == order) {
							if (order == 0) {
								order = (byte) 1;
							}else {
								order = (byte) 0;
							}
							//order = (byte) ((order==0)?1:0);
							break;	//ת����һ��ת̬
						} else {
							//do nothing �������ѭ���� ���´��ļ�
						}
					}catch(SocketTimeoutException e) {
						//��ʱ����Ҫ�ش�
						System.out.println("��ʱ���ش�");
					}
				} 
			} catch (IOException e) { 
				e.printStackTrace();
			} 
		}
	}

	public static void main(String[] args) {
		OneWayServer server = new OneWayServer();
	}
}
