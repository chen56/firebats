package firebats.bus.benchmarks.ws;

import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import firebats.bus.RxBus.IRawRequest;
import firebats.bus.exceptions.BusException;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.internal.bus.IBusClient;
import firebats.internal.bus.IBusServer;
import firebats.internal.bus.queue.RequestQueue;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsClient;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsServer;
import firebats.test.BenchMonitor;
/**
 * 
 2014-05-14 

 */
public class WsClientReplyBenchmarks_GuavaCache {
 	static long timeoutMills=5000;
	static long maxSizeOfReplyQueue=100000;
	static RequestQueue<String,ReplyWaiter> replyQueue = RequestQueue.createGuavaCache(timeoutMills,TimeUnit.MILLISECONDS,maxSizeOfReplyQueue);
	static BenchMonitor clientMonitor;
 	static IBusClient client;
 	static AtomicInteger seq=new AtomicInteger();
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, URISyntaxException {
  		replyQueue.evictions()
// 		  .observeOn(Schedulers.io())
// 		  .subscribeOn(Schedulers.io())
 		  .onErrorResumeNext(Observable.<ReplyWaiter>empty()).subscribe(new Action1<ReplyWaiter>() {
			BusException sharedEvictionError=new BusException("reply timeout or memory eviction");
			@Override
			public void call(ReplyWaiter next) {
				next.subject.onError(sharedEvictionError);
				next.subject.onCompleted();
			}
		});
		
		clientMonitor = BenchMonitor.builder().get();
		IBusServer service = listen(8080);
        service.getInput()
// 		    .observeOn(Schedulers.io())
// 		    .subscribeOn(Schedulers.io())
        .subscribe(new Action1<IRawRequest>() {
			@Override
			public void call(IRawRequest m) {
				String data=m.getMessage();
				//reply
//				Message.parse(data, new TypeToken<String>(){});
				m.reply(data);
//				debug("server : "+data);
			}
		},new Action1<Throwable>() {
			@Override
			public void call(Throwable e) {
				e.printStackTrace();
			}
		});
		
 		client = RxNettyWsClient.create(new URI("ws://localhost:8080/websocket"));
 		client.getInput()
 		    .observeOn(Schedulers.io())
 		    .subscribeOn(Schedulers.trampoline())
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
				writeAndFlush(json("{'c':'x','cid':'"+seq.getAndIncrement()+"','b':'login'}"));
   				ReplyWaiter h = replyQueue.poll(frame);
 				if(h!=null){
 		 			h.subject.onNext(frame);
  				}
 			}
		});
		int times=1000;
		for (int i = 0; i < times; i++) {
			writeAndFlush(json("{'c':'x','cid':'"+seq.getAndIncrement()+"','b':'login'}"));
		}
		System.in.read();
	}
 	
	private static void writeAndFlush(String message){
//		Message.parse(message, new TypeToken<String>(){});
		replyQueue.put(message, new ReplyWaiter(new Observer<String>() {
				@Override
				public void onCompleted() {
				}
				@Override
				public void onError(Throwable e) {
					e.printStackTrace();
				}
				@Override
				public void onNext(String frame) {
					debug("client receive reply handler: "+frame);
	 				clientMonitor.tickSuccess();
				}
			}));
		client.writeAndFlush(message);
	}
	
 	private static IBusServer listen(int port) {
 		RxNettyWsServer s=RxNettyWsServer.create(port);
 		s.start();
		return s;
	}
	static MemoryDiscovery discovery = new MemoryDiscovery();
	private static String json(String string) {
		return string.replace("'","\"");
//		return "x";
	}

	private static void debug(String string) {
//       System.out.println(string);		
	}
	private static class ReplyWaiter{
		private Observer<String> subject;
		private ReplyWaiter(Observer<String> subject){
			this.subject=subject;
		}
	}


}
