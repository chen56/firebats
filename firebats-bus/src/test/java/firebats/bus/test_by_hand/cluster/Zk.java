package firebats.bus.test_by_hand.cluster;

import org.apache.curator.test.TestingServer;

public class Zk {
	public static void main(String[] args) throws Exception {
		new TestingServer(2181).start();
		System.in.read();
	}
}
