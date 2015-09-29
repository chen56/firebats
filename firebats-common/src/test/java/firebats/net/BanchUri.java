package firebats.net;

import firebats.net.Uri;
import firebats.test.BenchMonitor;
import firebats.test.BenchMonitor.Task;

public class BanchUri {
    public static void main(String[] args) {
 		BenchMonitor.run(1000_000, "parse", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				Uri.buildFrom("http://host:8000/a?x=1").build().toURI();
 			}
		});
	}
}
