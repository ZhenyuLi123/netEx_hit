package net1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class myProxy extends Thread {
	public static int CONNECT_RETRIES = 5; //尝试建立连接次数
	public static int CONNECT_PAUSE = 5; //每次建立连接的时间间隔
	public static int TIMEOUT = 80000;
	public static int BUFSIZ = 1024;

	public static boolean flg2 = false;

	public static OutputStream save = null; //缓存
	public static OutputStream log = null; //所有日志


	public static int count = -1;
	public static List<String> requestInfo = new ArrayList<String>();
	public static List<String> cacheInfo;

	Socket socket2 = null; // 连服务器的socket

	InputStream fromClient = null; //从客户端来 到socket1
	InputStream fromServer = null; //从服务器来 到socket2
	OutputStream toServer = null; //去客户端 从socket1出
	OutputStream toClient = null; //去服务器 从socket2出

	PrintWriter tS = null;
	PrintWriter tC = null;
	BufferedReader fS = null;
	BufferedReader fC = null;
	// 对应处理前面四个Stream

	String buffer = ""; //读取请求头部
	String URL = ""; // URL
	String host = ""; // host

	int port = 80; // 默认端口号80
	String findUrl = "";// 在缓存中查找URL
	protected Socket socket1;// 连客户端的socket

	public myProxy(Socket socket) {
		try {
			//新的套接字 = 主套接字，来进行处理
			socket1 = socket;
			//初始化处理Stream
			fromClient = socket1.getInputStream();
			fC = new BufferedReader(new InputStreamReader(fromClient));

			toClient = socket1.getOutputStream();
			tC = new PrintWriter(toClient);

			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeSave(byte[] bytes, int offset, int len) throws IOException{
		for(int i = 0; i < len; i++) {
			save.write((char) bytes[offset + i]);
		}
	}

	public void writeLog(byte[] bytes, int offset, int len) throws IOException{
		for(int i = 0; i < len; i++) {
			log.write((char) bytes[offset + i]);
		}
	}

	@Override
	public void run() {

		try {
			socket1.setSoTimeout(TIMEOUT);// 设置socket1时间上限

			boolean flg = false;

			while(true) {

				buffer = fC.readLine();

				if (buffer == null) {
					continue;
				}


				//System.out.println("请求报文第一行：" + buffer);

				String[] tokens = buffer.split(" ");
				String flag = tokens[0];

				// https 直接结束进程
				if(flag.equals("CONNECT")) {
					this.interrupt();
				}

				URL = getURL(buffer);

				//从jwts改到jwc
				if(URL.equals("http://jwes.hit.edu.cn/")) { //篡改
					URL = "http://jwts.hit.edu.cn/";
					buffer = "GET " + URL + " HTTP/1.1"; //首部行重构


					//将重构信息存在requestInfo中
					requestInfo.add("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
					requestInfo.add("Accept-Language: zh-CN");
					requestInfo.add("Upgrade-Insecure-Requests: 1");
					requestInfo.add("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18362");
					requestInfo.add("Accept-Encoding: gzip, deflate");
					requestInfo.add("Host: jwts.hit.edu.cn");
					requestInfo.add("Proxy-Connection: Keep-Alive");
					requestInfo.add("Cookie: _ga=GA1.3.1857927844.1562668689");


					flg = true;

				} 
				//else if (URL.equals("http://www.wenming.cn/")) { //屏蔽
				//	URL = ""; 
				//	System.out.println("当前网页已经被屏蔽");
				//}

				//				if(URL.contains("http://jwts.hit.edu.cn/") && flg2 == true) {
				//					break;
				//				}

				int n;

				//提取host部分
				n = URL.indexOf("//");
				if (n != -1) {
					host = URL.substring(n + 2); //     //xxxx/ --->  xxxx/
				}
				n = host.indexOf('/');
				if (n != -1) {
					host = host.substring(0, n); //     xxxx/ ---> xxxx
				}

				//提取findUrl
				n = URL.indexOf('?');
				if (n != -1) {
					findUrl = URL.substring(0, n);
				} else {
					findUrl = URL;
				}

				//提取可能存在的端口号， 并处理host
				n = host.indexOf(':');
				if (n != -1) {
					port = Integer.parseInt(host.substring(n + 1));
					host = host.substring(0, n);
				}

				int retry = CONNECT_RETRIES;
				while (retry-- != 0 && !host.equals("")) {
					try {
						System.out.println("服务器端口号：" + port + "主机host：" + host);
						socket2 = new Socket(host, port); // 建立与服务器的连接

						break;
					} catch (Exception e) {
						//
					}

					// 等待
					Thread.sleep(CONNECT_PAUSE);
				}

				//与服务器连接建立成功，将所有数据返回给客户端
				if(socket2 != null) {
					socket2.setSoTimeout(TIMEOUT);

					//初始化处理Stream
					fromServer = socket2.getInputStream();
					fS = new BufferedReader(new InputStreamReader(fromServer));
					toServer = socket2.getOutputStream();
					tS = new PrintWriter(toServer);

					String modifTime = findCache(findUrl);
					//String modifTime = null;

					writeLog(buffer.getBytes(), 0, buffer.length());
					writeSave(buffer.getBytes(), 0, buffer.length());
					writeSave("\r\n".getBytes(), 0, 2);

					if(modifTime == null) {
						System.out.println("无缓存");
						//发送给server
						while (!buffer.equals("")) {
							buffer += "\r\n"; // 换行符
							if (buffer.contains("jwts.hit.edu.cn")) { //篡改
								//if (flg == true) { //篡改
								//System.out.println("发送假报文");
								int k = 0;
								while (requestInfo.size() - k > 0) {
									tS.write(buffer);
									buffer = requestInfo.get(k++); //??? 迷惑
									buffer += "\r\n";
								}
								//flg = false;
								//flg2 = true;
								break;
							} else {
								//System.out.print("发送请求如下：");
								//if(buffer.contains("User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")) {
								//	break;
								//}
								tS.write(buffer);
								writeLog(buffer.getBytes(), 0, buffer.length());
								System.out.print(buffer);
								buffer = fC.readLine(); //读取请求报文的下一行


							}
						}

						tS.write("\r\n");
						writeLog("\r\n".getBytes(), 0, 2);
						tS.flush();

						//读取服务器响应
						int length;
						byte bytes[] = new byte[BUFSIZ];
						while (true) {
							try { // 将从服务器收到的数据转发给客户端
								length = fromServer.read(bytes);
								if (length > 0) {
									try {
										toClient.write(bytes, 0, length);
										writeLog(bytes, 0, length);
										writeSave(bytes, 0, length);
									} catch (Exception e) {
										//
									}

								} else {
									break;
								}
							} catch (Exception e) {
								// no print
							}
						}

						tC.write("\r\n");
						tC.flush();
						writeSave("\r\n".getBytes(), 0, 2);
					}
					else { // 有缓存
						buffer += "\r\n";
						tS.write(buffer); //发送第一行之后构造 if modified的报文



						String str1 = "Host: " + host + "\r\n";
						tS.write(str1);

						String str2 = "If-Modified-Since: " + modifTime + "\r\n";
						tS.write(str2);

						tS.write("\r\n"); 
						tS.flush();

						System.out.println("有缓存，发送查询是否有更新的报文：");
						System.out.println(buffer);
						System.out.println(str1); //打印一下康康
						System.out.println(str2);

						String info = fS.readLine(); //回来的报文第一行
						System.out.print("服务器发回报文：");
						System.out.println(info);

						if(info.contains("Not Modified")) {
							int j = 0;
							System.out.println("使用缓存中的数据，数据如下：");
							while(j < cacheInfo.size()) {
								info = cacheInfo.get(j++);
								info += "\r\n";
								System.out.print(info);
								tC.write(info);
							}
							tC.write("\r\n");
							tC.flush();
						} else {
							System.out.println("有更新，使用新的数据，更新数据如下：");
							while(!info.equals("")) {
								info += "\r\n";
								System.out.print(info);
								tC.write(info);
								info = fS.readLine();
							}

							info = fS.readLine();
							while(!info.equals("")) {
								info += "\r\n";
								System.out.print(info);
								tC.write(info);
								info = fS.readLine();
							}
							tC.write("\r\n");
							tC.flush();
						}
					}
				}

			}



		} catch (Exception e) {
			//do nothing
		}
	}

	//寻找URL，get空格后边的那部分
	public String getURL(String str) {
		String [] tokens = buffer.split(" ");
		String URL = "";
		if (tokens[0].equals("GET")) {
			for (int index = 0; index < tokens.length; index++) {
				if (tokens[index].startsWith("http://")) {
					URL = tokens[index];
					break;
				}
			}
		}
		return URL;
	}

	//在缓存文件中寻找修改日期
	public String findCache(String str) {
		cacheInfo = new ArrayList<String>();
		String res = null;
		int count = 0;
		boolean flagg = false;
		try {
			// 在存储URL和相应信息的文件中查找
			InputStream f = new FileInputStream("save.txt");
			String info = "";
			// 读取数据
			while (true) {
				int c = f.read();
				if (c == -1) {
					break; // -1结尾
				}
				if (c == '\r') {
					f.read();
					break; // 读入一行数据
				}
				if (c == '\n') {
					break;
				}
				info = info + (char) c;
			}
			//System.out.println("第一条得到的缓存：" + info);
			//System.out.println("目标：" + str);
			int m = 0;
			while((m = f.read()) != -1 && info != null) {
				// 去找相同的，如果找到了在他下面就是上次修改的时间
				if (info.contains(str)) {
					String info1;
					do {
						//System.out.println("找到了相同的");
						info1 = "";
						if (m != '\r' && m != '\n') {
							info1 += (char) m;
						}
						while (true) {
							m = f.read();
							if (m == -1) {
								break;
							}
							if (m == '\r') {
								f.read();
								break;
							}
							if (m == '\n') {
								break;
							}
							info1 += (char) m;
						}

						//打印一下每一行收回来的
						//System.out.println(info1);
						if (info1.contains("Last-Modified:")) {
							res = info1.substring(15);
							flagg = true;
						}
						cacheInfo.add(info1);

						if (info1.equals("")) {
							if (flagg == false) {
								System.out.println("无modified标签");
							} else {
								System.out.println("有modified标签");
							}

							return res;
						}
					} while (!info1.equals("") && info1 != null && m != -1);
				}

				info = "";
				while(true) {
					if (m == -1) {
						break;
					}
					if (m == '\r') {
						f.read();
						break;
					}
					if (m == '\n') {
						break;
					}
					info += (char) m;
					m = f.read();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	public static void startProxy(int port, Class clobj) {
		try {
			ServerSocket ssock = new ServerSocket(port); //主套接字
			while (true) {
				Class[] sarg = new Class[1];
				Object[] arg = new Object[1];
				sarg[0] = Socket.class;
				try {
					java.lang.reflect.Constructor cons = clobj
							.getDeclaredConstructor(sarg);
					arg[0] = ssock.accept();
					System.out.println("启动线程："+count++);
					cons.newInstance(arg); // 创建HttpProxy或其派生类的实例
				} catch (Exception e) { //异常处理
					Socket esock = (Socket) arg[0];
					try {
						esock.close();
					} catch (Exception ec) {
					}
				}
			}
		} catch (IOException e) {
			System.out.println("\nStartProxy Exception:");
			e.printStackTrace();
		}
	}

	// 测试用的简单main方法
	static public void main(String args[]) throws FileNotFoundException {
		System.out.println("在端口8080启动代理服务器\n");
		OutputStream save = new FileOutputStream(new File("save.txt"));
		OutputStream log = new FileOutputStream(new File("log.txt"));
		myProxy.save = save;
		myProxy.log = log;
		myProxy.startProxy(8080, myProxy.class);
	}

}
