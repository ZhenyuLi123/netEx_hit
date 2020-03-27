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
	public static int CONNECT_RETRIES = 5; //���Խ������Ӵ���
	public static int CONNECT_PAUSE = 5; //ÿ�ν������ӵ�ʱ����
	public static int TIMEOUT = 80000;
	public static int BUFSIZ = 1024;

	public static boolean flg2 = false;

	public static OutputStream save = null; //����
	public static OutputStream log = null; //������־


	public static int count = -1;
	public static List<String> requestInfo = new ArrayList<String>();
	public static List<String> cacheInfo;

	Socket socket2 = null; // ����������socket

	InputStream fromClient = null; //�ӿͻ����� ��socket1
	InputStream fromServer = null; //�ӷ������� ��socket2
	OutputStream toServer = null; //ȥ�ͻ��� ��socket1��
	OutputStream toClient = null; //ȥ������ ��socket2��

	PrintWriter tS = null;
	PrintWriter tC = null;
	BufferedReader fS = null;
	BufferedReader fC = null;
	// ��Ӧ����ǰ���ĸ�Stream

	String buffer = ""; //��ȡ����ͷ��
	String URL = ""; // URL
	String host = ""; // host

	int port = 80; // Ĭ�϶˿ں�80
	String findUrl = "";// �ڻ����в���URL
	protected Socket socket1;// ���ͻ��˵�socket

	public myProxy(Socket socket) {
		try {
			//�µ��׽��� = ���׽��֣������д���
			socket1 = socket;
			//��ʼ������Stream
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
			socket1.setSoTimeout(TIMEOUT);// ����socket1ʱ������

			boolean flg = false;

			while(true) {

				buffer = fC.readLine();

				if (buffer == null) {
					continue;
				}


				//System.out.println("�����ĵ�һ�У�" + buffer);

				String[] tokens = buffer.split(" ");
				String flag = tokens[0];

				// https ֱ�ӽ�������
				if(flag.equals("CONNECT")) {
					this.interrupt();
				}

				URL = getURL(buffer);

				//��jwts�ĵ�jwc
				if(URL.equals("http://jwes.hit.edu.cn/")) { //�۸�
					URL = "http://jwts.hit.edu.cn/";
					buffer = "GET " + URL + " HTTP/1.1"; //�ײ����ع�


					//���ع���Ϣ����requestInfo��
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
				//else if (URL.equals("http://www.wenming.cn/")) { //����
				//	URL = ""; 
				//	System.out.println("��ǰ��ҳ�Ѿ�������");
				//}

				//				if(URL.contains("http://jwts.hit.edu.cn/") && flg2 == true) {
				//					break;
				//				}

				int n;

				//��ȡhost����
				n = URL.indexOf("//");
				if (n != -1) {
					host = URL.substring(n + 2); //     //xxxx/ --->  xxxx/
				}
				n = host.indexOf('/');
				if (n != -1) {
					host = host.substring(0, n); //     xxxx/ ---> xxxx
				}

				//��ȡfindUrl
				n = URL.indexOf('?');
				if (n != -1) {
					findUrl = URL.substring(0, n);
				} else {
					findUrl = URL;
				}

				//��ȡ���ܴ��ڵĶ˿ںţ� ������host
				n = host.indexOf(':');
				if (n != -1) {
					port = Integer.parseInt(host.substring(n + 1));
					host = host.substring(0, n);
				}

				int retry = CONNECT_RETRIES;
				while (retry-- != 0 && !host.equals("")) {
					try {
						System.out.println("�������˿ںţ�" + port + "����host��" + host);
						socket2 = new Socket(host, port); // �����������������

						break;
					} catch (Exception e) {
						//
					}

					// �ȴ�
					Thread.sleep(CONNECT_PAUSE);
				}

				//����������ӽ����ɹ������������ݷ��ظ��ͻ���
				if(socket2 != null) {
					socket2.setSoTimeout(TIMEOUT);

					//��ʼ������Stream
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
						System.out.println("�޻���");
						//���͸�server
						while (!buffer.equals("")) {
							buffer += "\r\n"; // ���з�
							if (buffer.contains("jwts.hit.edu.cn")) { //�۸�
								//if (flg == true) { //�۸�
								//System.out.println("���ͼٱ���");
								int k = 0;
								while (requestInfo.size() - k > 0) {
									tS.write(buffer);
									buffer = requestInfo.get(k++); //??? �Ի�
									buffer += "\r\n";
								}
								//flg = false;
								//flg2 = true;
								break;
							} else {
								//System.out.print("�����������£�");
								//if(buffer.contains("User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")) {
								//	break;
								//}
								tS.write(buffer);
								writeLog(buffer.getBytes(), 0, buffer.length());
								System.out.print(buffer);
								buffer = fC.readLine(); //��ȡ�����ĵ���һ��


							}
						}

						tS.write("\r\n");
						writeLog("\r\n".getBytes(), 0, 2);
						tS.flush();

						//��ȡ��������Ӧ
						int length;
						byte bytes[] = new byte[BUFSIZ];
						while (true) {
							try { // ���ӷ������յ�������ת�����ͻ���
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
					else { // �л���
						buffer += "\r\n";
						tS.write(buffer); //���͵�һ��֮���� if modified�ı���



						String str1 = "Host: " + host + "\r\n";
						tS.write(str1);

						String str2 = "If-Modified-Since: " + modifTime + "\r\n";
						tS.write(str2);

						tS.write("\r\n"); 
						tS.flush();

						System.out.println("�л��棬���Ͳ�ѯ�Ƿ��и��µı��ģ�");
						System.out.println(buffer);
						System.out.println(str1); //��ӡһ�¿���
						System.out.println(str2);

						String info = fS.readLine(); //�����ı��ĵ�һ��
						System.out.print("���������ر��ģ�");
						System.out.println(info);

						if(info.contains("Not Modified")) {
							int j = 0;
							System.out.println("ʹ�û����е����ݣ��������£�");
							while(j < cacheInfo.size()) {
								info = cacheInfo.get(j++);
								info += "\r\n";
								System.out.print(info);
								tC.write(info);
							}
							tC.write("\r\n");
							tC.flush();
						} else {
							System.out.println("�и��£�ʹ���µ����ݣ������������£�");
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

	//Ѱ��URL��get�ո��ߵ��ǲ���
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

	//�ڻ����ļ���Ѱ���޸�����
	public String findCache(String str) {
		cacheInfo = new ArrayList<String>();
		String res = null;
		int count = 0;
		boolean flagg = false;
		try {
			// �ڴ洢URL����Ӧ��Ϣ���ļ��в���
			InputStream f = new FileInputStream("save.txt");
			String info = "";
			// ��ȡ����
			while (true) {
				int c = f.read();
				if (c == -1) {
					break; // -1��β
				}
				if (c == '\r') {
					f.read();
					break; // ����һ������
				}
				if (c == '\n') {
					break;
				}
				info = info + (char) c;
			}
			//System.out.println("��һ���õ��Ļ��棺" + info);
			//System.out.println("Ŀ�꣺" + str);
			int m = 0;
			while((m = f.read()) != -1 && info != null) {
				// ȥ����ͬ�ģ�����ҵ���������������ϴ��޸ĵ�ʱ��
				if (info.contains(str)) {
					String info1;
					do {
						//System.out.println("�ҵ�����ͬ��");
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

						//��ӡһ��ÿһ���ջ�����
						//System.out.println(info1);
						if (info1.contains("Last-Modified:")) {
							res = info1.substring(15);
							flagg = true;
						}
						cacheInfo.add(info1);

						if (info1.equals("")) {
							if (flagg == false) {
								System.out.println("��modified��ǩ");
							} else {
								System.out.println("��modified��ǩ");
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
			ServerSocket ssock = new ServerSocket(port); //���׽���
			while (true) {
				Class[] sarg = new Class[1];
				Object[] arg = new Object[1];
				sarg[0] = Socket.class;
				try {
					java.lang.reflect.Constructor cons = clobj
							.getDeclaredConstructor(sarg);
					arg[0] = ssock.accept();
					System.out.println("�����̣߳�"+count++);
					cons.newInstance(arg); // ����HttpProxy�����������ʵ��
				} catch (Exception e) { //�쳣����
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

	// �����õļ�main����
	static public void main(String args[]) throws FileNotFoundException {
		System.out.println("�ڶ˿�8080�������������\n");
		OutputStream save = new FileOutputStream(new File("save.txt"));
		OutputStream log = new FileOutputStream(new File("log.txt"));
		myProxy.save = save;
		myProxy.log = log;
		myProxy.startProxy(8080, myProxy.class);
	}

}
