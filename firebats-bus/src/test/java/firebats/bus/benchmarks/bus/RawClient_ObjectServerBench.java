package firebats.bus.benchmarks.bus;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.test.BenchMonitor;

public class RawClient_ObjectServerBench {
	static BenchMonitor clientMonitor;
	static RxBus client;
	static AtomicInteger seq = new AtomicInteger();
	final static MessageSpec<String, String> X_REQUEST= Message.newRequestSpec("x",String.class,String.class)
			 ;

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException {
		clientMonitor = BenchMonitor.builder().get();
		RxBus service = listen(8080);
		
        service.receive(X_REQUEST,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just("reply"); 
			}
		});

		client = listen(8081);
		int times = 1000;
		for (int i = 0; i < times; i++) {
			writeAndFlush();
		}
		System.in.read();
	}

	private static void writeAndFlush() {
		String message = json("{'c':'x','cid':'" + seq.getAndIncrement()
				+ "','b':'login'}");

		Observer<String> o = new Observer<String>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}

			@Override
			public void onNext(String frame) {
				debug("client receive reply handler: " + frame);
				clientMonitor.tickSuccess();
				writeAndFlush();
			}
		};
		client.rawRequest("x", message).subscribe(o);
	}

	private static RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}

	static MemoryDiscovery discovery = new MemoryDiscovery();

	private static String json(String string) {
		return string.replace("'", "\"");
//		 return "x";
	}

	private static void debug(String string) {
//		 System.out.println(string);
	}
}
