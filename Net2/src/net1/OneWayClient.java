package net1;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class OneWayClient extends Thread{
	public static final int MAX_LENGTH = 8; //传输数据最大位数
	public static DatagramSocket socket; //socket
	public static int last;	//上一次收到的包的编号
	public static byte[] receive = new byte[MAX_LENGTH]; //接收数组
	public static byte[] send = new byte[MAX_LENGTH]; //发送数组
	public static OutputStream writeFile; //写入文件
	public static InetAddress inetAddress; //获取本机host、IP等
	public static int port; //端口号

	public OneWayClient() {
		try {
			socket = new DatagramSocket(9000);
			last = 1;	//因为第一个需要的是0号packet
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
		int timeOut = 0;	//模拟最多超时的次数，之后恢复 发回时丢失

		int timeOut1 = 0;

		while(true) {
			try{
				DatagramPacket packet1 = new DatagramPacket(receive, receive.length);
				socket.receive(packet1);

				if(timeOut1 > 0) {
					try {
						sleep(3000);
						timeOut1--;
						continue;
					} catch (Exception e) {
						//
					}
				}
				timeOut1--;

				if (flag == false) { //回退last
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
				System.out.println("收到的数据是："+new String(receive) + " oreder是：" + order +" 需要的是："+need);
				//收到的是需要的数据包，则写入文件，回传ack
				if(need == order) { 
					//System.out.print(new String(receive));
					//System.out.print(packet1.getLength() - 1);

					if (flag == true) { //非重传文件 
						writeFile.write(receive, 1, packet1.getLength() - 1);
					} else {
						System.out.println("冗余，丢弃");
					}

					send[0] = need;
					last = order;
					System.out.println("回传的ack是："+need);  
					inetAddress = packet1.getAddress();
					port = packet1.getPort();
					System.out.println("主机名："+inetAddress.getHostName()+" port:"+port);
					DatagramPacket packet2 = new DatagramPacket(send,send.length,inetAddress,port);
					flag = false;
					if(timeOut <= 0){

						socket.send(packet2); 
						flag = true;

					}else {
						System.out.println("模拟超时，回传丢失");
					}
					timeOut--;
				}
				//下边的在正确的程序里用不到，但是写上了，让FSM表现得更完全一些
				else{
					send[0] = need;  //need - order
					System.out.println("传回来的包不是想要的，丢弃。。");
					System.out.println("回传的ack是："+need);
					inetAddress = packet1.getAddress();
					port = packet1.getPort();
					System.out.println("主机名："+inetAddress.getHostName()+" port:"+port);
					DatagramPacket packet2 = new DatagramPacket(send,send.length,inetAddress,port);
					socket.send(packet2);
				}
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		OneWayClient client = new OneWayClient();
		//OneWayServer server = new OneWayServer();
	}

}
