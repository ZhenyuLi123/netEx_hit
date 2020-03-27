package net1;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server extends Thread{
	public static final int MAX_LENGTH = 8;
	public static DatagramSocket socket;
	public static int last;	//��һ���յ��İ��ı��
	public static byte[] receive = new byte[MAX_LENGTH];
	public static byte[] send = new byte[MAX_LENGTH];
	public static OutputStream writeFile;
	public static InetAddress inetAddress;
	public static int port;

	public Server() {
		try {
			socket = new DatagramSocket(9000);
			last = 1;	//��Ϊ��һ����Ҫ����0��packet
			writeFile = new FileOutputStream("receive.txt"); 
			receive[0] = 1;
			start();
		} catch (SocketException e) { 
			e.printStackTrace();
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		}  
	}
	@Override
	public void run() {
		boolean flag = true;
		int timeOut = 1;	//ģ����೬ʱ�Ĵ�����֮��ָ� ����ʱ��ʧ

		int timeOut1 = 0;

		while(true) {
			try{
				DatagramPacket packet1 = new DatagramPacket(receive, receive.length);
				socket.receive(packet1);

				if(timeOut1 -- > 0) {
					try {
						sleep(3000);
						continue;
					} catch (Exception e) {
						//
					}
				}

				if (flag == false) { //����last
					if(last == 0) {
						last =  1;
					}else {
						last =  0;
					}
				}

				byte order = receive[0]; 
				byte need;
				if(last == 0) {
					need = (byte) 1;
				}else {
					need = (byte) 0;
				}
				//byte need = (byte)((last==0)?1:0);
				System.out.println("�յ������ݰ��ǣ�"+new String(receive) + " oreder�ǣ�" + order +" ��Ҫ���ǣ�"+need);
				//�յ�������Ҫ�����ݰ�����д���ļ����ش�ack
				if(need == order) { 
					//System.out.print(new String(receive));
					//System.out.print(packet1.getLength() - 1);

					if (flag == true) { //���ش��ļ� 
						writeFile.write(receive, 1, packet1.getLength() - 1);
					} else {
						System.out.println("���࣬����");
					}

					send[0] = need;
					last = order;
					System.out.println("�ش���ack�ǣ�"+need);  
					inetAddress = packet1.getAddress();
					port = packet1.getPort();
					System.out.println("��������"+inetAddress.getHostName()+" port:"+port);
					DatagramPacket packet2 = new DatagramPacket(send,send.length,inetAddress,port);
					flag = false;
					if(timeOut-- <= 0){

						socket.send(packet2); 
						flag = true;

					}else {
						System.out.println("ģ�ⳬʱ���ش���ʧ");
					}
				}
				//�±ߵ�����ȷ�ĳ������ò���������д���ˣ���FSM���ֵø���ȫһЩ
				else{
					send[0] = need;  //need - order
					System.out.println("�������İ�������Ҫ�ģ���������");
					System.out.println("�ش���ack�ǣ�"+need);
					inetAddress = packet1.getAddress();
					port = packet1.getPort();
					System.out.println("��������"+inetAddress.getHostName()+" port:"+port);
					DatagramPacket packet2 = new DatagramPacket(send,send.length,inetAddress,port);
					socket.send(packet2);
				}
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		Client client = new Client();
	}

}
