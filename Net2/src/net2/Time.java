package net2;

public class Time {
	public volatile int time;

	public  synchronized int getTime() {
		return time;
	}

	public synchronized void setTime(int time) {
		this.time = time;
	}
}
