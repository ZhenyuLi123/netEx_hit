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

					//��������timeû�б���Ϊ����Ϊ��ʱ
					System.out.println("\n");
					if(gbnServer == null){
						System.out.println("SR�ͻ��˵ȴ�ACK��ʱ");
						//srServer.timeOut();
					}else{
						System.out.println("GBN�ͻ��˵ȴ�ACK��ʱ");
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
