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

public class Client extends Thread{
	public static int MAX_LENGTH = 8;	//每次读取文件的的最大字节数
	public static final int TIMEOUT = 3000; //设置超时时间	 
	public static byte[] receive = new byte[MAX_LENGTH];
	public static DatagramSocket socket;
	public static InputStream inputFile = null; //从这读取传送数据
	public static byte order; //顺序
	public static InetAddress inetAddress;
	public static int port; //端口号

	public Client () {
		try { 
			socket = new DatagramSocket(8000);
			socket.setSoTimeout(TIMEOUT); //超时时间间隔
			inputFile = new FileInputStream("read.txt"); //读取read.txt
			order = 0;	//刚开始发送的是0数据包
			inetAddress =  InetAddress.getByName("localhost"); //本机主机地址
			port = 9000; //端口号设置
			start();
		} catch (SocketException | FileNotFoundException e) { 
			e.printStackTrace();
		} catch (UnknownHostException e) { 
			e.printStackTrace();
		}	 
	}
	@Override
	public void run() {
		//int timeOut = 1;
		int count = 0; 
		//创建数据包
		while(true) {  
			int len;
			try { 
				byte[] sendata = new byte[MAX_LENGTH];
				sendata[0] = order; 
				len = inputFile.read(sendata,1,sendata.length - 1);
				count++;
				// System.out.println(len);
				if(len == -1) { //文件已经传送完毕
					System.out.println("文件已经传送完毕");
					break;
				}

				while(len != -1) { 
					try{
						System.out.println("发送第"+count+"个数据报");
						DatagramPacket packet = new DatagramPacket(sendata,len + 1,inetAddress,port);
						socket.send(packet);

						/*if(timeOut-- <= 0){
							socket.send(packet);
						} else {
							SocketTimeoutException e = new SocketTimeoutException();
							throw e;
						}*/


						DatagramPacket packet2 = new DatagramPacket(receive,receive.length);
						socket.receive(packet2);
						byte ack = receive[0];
						System.out.println("传出去的包是："+order+"传回来的ACK是："+ack);
						if(ack == order) {
							if (order == 0) {
								order = (byte) 1;
							}else {
								order = (byte) 0;
							}
							//order = (byte) ((order==0)?1:0);
							break;	//转到下一个转态
						} else {
							//do nothing 还在这次循环中 重新传文件
						}
					}catch(SocketTimeoutException e) {
						//超时，需要重传
						System.out.println("超时，重传");
					}
				} 
			} catch (IOException e) { 
				e.printStackTrace();
			} 
		}
	}
}
