package net2;

public class Timer extends Thread{
	private Time tm;
	private GBNServer gbnServer;
	//private SRServer srServer;
	public Timer(GBNServer gbnServer, Time tm){
		this.gbnServer = gbnServer;
		this.tm = tm;
	}
	//	public Timer(SRServer srServer,Time tm){
	//		this.srServer = srServer;
	//		this.tm = tm;
	//	}
	@Override
	public void run(){
		do{
			int time = tm.getTime();
			if(time > 0){
				try {
					Thread.sleep(time * 1000);

					//三秒后如果time没有被置为零认为超时
					System.out.println("\n");
					if(gbnServer == null){
						System.out.println("SR客户端等待ACK超时");
						//srServer.timeOut();
					}else{
						System.out.println("GBN客户端等待ACK超时");
						gbnServer.timeOut();
					}
					tm.setTime(0);

				} catch (InterruptedException e) {
				} catch (Exception e) {
				}
			}
		}while (true);
	}
}
