package firebats.bus.benchmarks.ws;

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
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

import com.google.common.base.Optional;

import firebats.bus.RxBus;
import firebats.bus.RxBus.IRawRequest;
import firebats.bus.exceptions.BusException;
import firebats.discovery.Service;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.internal.bus.IBusClient;
import firebats.internal.bus.RawBus;
import firebats.internal.bus.queue.RequestQueue;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsClient;
import firebats.test.BenchMonitor;

public class BusServer_WsClient {
	static long timeoutMills = 5000;
	static long maxSizeOfReplyQueue = 1000*1000;
	static RequestQueue<Long,ReplyWaiter> replyObserverQueue = RequestQueue.createGuavaCache(timeoutMills,TimeUnit.MILLISECONDS,maxSizeOfReplyQueue);
 
 	static BenchMonitor clientMonitor;
	static IBusClient client;
	static AtomicInteger seq = new AtomicInteger();
	static PublishSubject<String> replies = PublishSubject.create();

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException, URISyntaxException {
		
		replyObserverQueue.evictions()
//		        .observeOn(Schedulers.io())
//				.subscribeOn(Schedulers.io())
				.onErrorResumeNext(Observable.<ReplyWaiter>empty())
				.subscribe(new Action1<ReplyWaiter>() {
					BusException sharedEvictionError = new BusException("reply timeout or memory eviction");
 					@Override
					public void call(ReplyWaiter next) {
						next.subject.onError(sharedEvictionError);
						next.subject.onCompleted();
					}
				});

		clientMonitor = BenchMonitor.builder().get();
		RxBus service = listen(8080);
		service.rawReceive("x")
//		        .observeOn(Schedulers.io())
//				.subscribeOn(Schedulers.io())
				.subscribe(new Action1<IRawRequest>() {
					@Override
					public void call(IRawRequest m) {
						String data = m.getMessage();
//						debug("server : " + data);
						m.reply(data);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable e) {
						e.printStackTrace();
					}
				});

 		client = RxNettyWsClient.create(new URI("ws://localhost:8080/websocket"));
 		client.startAsync();
		replies.asObservable()
		       .onErrorResumeNext(Observable.<String>empty())
		       .subscribe(
				new Action1<String>() {
					@Override
					public void call(String textFrame) {
						Long correlationId = RawBus.parseCid(textFrame);
						ReplyWaiter x = replyObserverQueue.poll(correlationId);
						x.subject.onNext(textFrame);
					}
				});

		client.getInput()
		        .observeOn(Schedulers.io())
				.subscribeOn(Schedulers.trampoline())
				.subscribe(new Observer<String>() {
					@Override
					public void onCompleted() {
					}

					@Override
					public void onError(Throwable e) {
						replies.onError(e);
						e.printStackTrace();
					}

					@Override
					public void onNext(String frame) {
						replies.onNext(frame);
					}
				});
		int times = 1000;
		for (int i = 0; i < times; i++) {
			writeAndFlush();
		}
		System.in.read();
	}

	private static void writeAndFlush() {
		String message=json("{'c':'x','cid':'" + seq.getAndIncrement()+ "','b':'login'}");
 		Observer<String> ob = new Observer<String>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}

			@Override
			public void onNext(String frame) {
				debug("received reply message: " + frame);
				writeAndFlush();
				clientMonitor.tickSuccess();
			}
		};
//		replyObserverQueue.put(message, new ReplyWaiter(ob));
//		client.writeAndFlush(message);
		
		rawRequest("x",message).subscribe(ob);
	}
	public static <OUT> Observable<Void> rawSend(String channel, String message) {
		 Optional<Service> service = discovery.select(channel);
		if(!service.isPresent()){
			return Observable.error(new BusException(String.format("send failed,channel[%s] no subscriber", channel)));
		}
		IBusClient connection = getOrConnect(service.get());
 		return connection.writeAndFlush(message);
	}

	private static IBusClient getOrConnect(Service server) {
 		return client;
	}

 	/**
	 * request/reply 模式,会收到reply
	 */
	public static Observable<String> rawRequest(String channel, String message) {
 		final ReplaySubject<String> replyObserver=ReplaySubject.create();
		final Long correlationId=RawBus.parseCid(message);
		if(correlationId==null){
			replyObserver.onError(new BusException("Send Raw Request failed, No CorrelationId:"+message));
			replyObserver.onCompleted();
		}
		replyObserverQueue.put(correlationId, new ReplyWaiter(replyObserver));

		Observable<Void> send = rawSend(channel,message);
		//TODO ReplaySubject > oBSERVABLE.CREATE
 		send.subscribe(new Observer<Void>() {
			@Override
			public void onCompleted() {
				//发送完成不代表已经回复
			}
			@Override
			public void onError(Throwable e) {
				replyObserver.onError(e);
				replyObserver.onCompleted();
				//如果发送失败，则没有必要再等待reply
				replyObserverQueue.poll(correlationId);
			}
			@Override
			public void onNext(Void t) {
			}
		});
         return replyObserver;
 	}
 	
	private static RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}

	static MemoryDiscovery discovery = new MemoryDiscovery();

	private static String json(String string) {
		return string.replace("'", "\"");
		// return "x";
	}

	private static void debug(String string) {
//		 System.out.println(string);
	}

	private static class ReplyWaiter {
		private Observer<String> subject;

		private ReplyWaiter(Observer<String> subject) {
			this.subject = subject;
		}
	}

}
