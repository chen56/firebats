package firebats.bus.benchmarks.ws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observer;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import firebats.bus.RxBus.IRawRequest;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.internal.bus.IBusServer;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsClient;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsServer;
import firebats.test.BenchMonitor;
/**
 * 
 2014-05-14 
 writeAndFlush：
[BenchMonitor] 1985 success/s, 0 error/s , 1985 total 
[BenchMonitor] 24450 success/s, 0 error/s , 26438 total 
[BenchMonitor] 93079 success/s, 0 error/s , 119514 total 
[BenchMonitor] 103090 success/s, 0 error/s , 222607 total 
[BenchMonitor] 111456 success/s, 0 error/s , 334061 total 
[BenchMonitor] 108437 success/s, 0 error/s , 442500 total 
[BenchMonitor] 110317 success/s, 0 error/s , 552815 total 
[BenchMonitor] 117725 success/s, 0 error/s , 670539 total 

write 快一倍，但可能不flush，有空再优化
[BenchMonitor] 2047 success/s, 0 error/s , 2047 total 
[BenchMonitor] 48794 success/s, 0 error/s , 50843 total 
[BenchMonitor] 169968 success/s, 0 error/s , 220813 total 
[BenchMonitor] 186345 success/s, 0 error/s , 407158 total 
[BenchMonitor] 182072 success/s, 0 error/s , 589231 total 
[BenchMonitor] 197908 success/s, 0 error/s , 787134 total 

 */
public class WsClientBenchmarks {
 	static AtomicInteger seq=new AtomicInteger();

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, URISyntaxException {
		final BenchMonitor clientMonitor = BenchMonitor.builder().get();
		IBusServer service = listen(8080);
        service.getInput()
            .observeOn(Schedulers.io())
 		    .subscribeOn(Schedulers.io())
        .subscribe(new Action1<IRawRequest>() {
			@Override
			public void call(IRawRequest m) {
				String data=m.getMessage();
				m.reply(data);
				debug("server "+data);
			}
		},new Action1<Throwable>() {
			@Override
			public void call(Throwable e) {
				e.printStackTrace();
			}
		});
		
 		final RxNettyWsClient x = RxNettyWsClient.create(new URI("ws://localhost:8080/websocket"));
		x.getInput()
 		    .observeOn(Schedulers.io())
 		    .subscribeOn(Schedulers.io())
		  .subscribe(new Observer<String>() {
			@Override
			public void onCompleted() {
			}
			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}
			@Override
			public void onNext(String frame) {
				x.writeAndFlush(json("{'c':'x','cid':'"+seq.getAndIncrement()+"','b':'login'}"));
  				clientMonitor.tickSuccess();
				debug("client receive "+frame);
 			}
		});
		int times=1000;
		for (int i = 0; i < times; i++) {
 			x.writeAndFlush(json("{'c':'x','cid':'"+seq.getAndIncrement()+"','b':'login'}"));
		}
		System.in.read();
	}
 	private static IBusServer listen(int port) {
 		RxNettyWsServer s=RxNettyWsServer.create(port);
 		s.start();
		return s;
	}
	static MemoryDiscovery discovery = new MemoryDiscovery();
	private static String json(String string) {
		return string.replace("'","\"");
	}

	private static void debug(String string) {
//       System.out.println(string);		
	}


}
