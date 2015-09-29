package firebats.bus;

public class BusMonitor {
	public static class ServerMonitor {
		public static final ServerMonitor Dummy = new ServerMonitor(){};
	}
	public static class ClientMonitor {
		public void receive(Object msg) {
		}
		public void error(Throwable cause) {
			
		}
		public static ClientMonitor Dummy=new ClientMonitor(){
		};
	}
}