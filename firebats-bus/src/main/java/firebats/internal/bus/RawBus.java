package firebats.internal.bus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import firebats.bus.BusMonitor.ServerMonitor;
import firebats.bus.RxBus.Builder;
import firebats.bus.RxBus.IRawRequest;
import firebats.bus.exceptions.BusException;
import firebats.check.Check;
import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStop;
import firebats.discovery.IDiscovery;
import firebats.discovery.Service;
import firebats.internal.bus.json.JacksonHelper;
import firebats.internal.bus.queue.ReplyWaiter;
import firebats.internal.bus.queue.RequestQueue;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsClient;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsServer;
import firebats.net.Uri;

public class RawBus implements IComponentProvider{
	private static Logger log=LoggerFactory.getLogger(RawBus.class);

	@SuppressWarnings("unused")
	private ServerMonitor monitor;
	private final ConcurrentMap<String, Object> subscribers = new ConcurrentHashMap<>();
	private final ConcurrentMap<Uri, IBusClient> clients = new ConcurrentHashMap<>();
	private PublishSubject<String> replies = PublishSubject.create();

	private IDiscovery discovery;
	private IBusServer bindServer;

    private long timeout=5;
    private TimeUnit timeoutTimeUnit=TimeUnit.SECONDS;
	private final long maxSizeOfReplyQueue=200*1000;
	private RequestQueue<Long,ReplyWaiter> replyObserverQueue = RequestQueue.createGuavaCache(timeout,timeoutTimeUnit,maxSizeOfReplyQueue);
//	private RequestQueue<String,ReplyWaiter> replyObserverQueue = RequestQueue.createAndStartDelayQueue(timeoutMills,TimeUnit.MILLISECONDS);
	private Uri uri;
    private ComponentContainer container=Component.newContainer(this);

	public long getTimeout() {
		return timeout;
	}
	public TimeUnit getTimeoutTimeUnit() {
		return timeoutTimeUnit;
	}
	public static final int MaxFramePayloadLength = 655350;
	
 	/*internal*/public RawBus(Builder builder) {
		Preconditions.checkNotNull(builder.discovery, "discovery should not null");
		Preconditions.checkNotNull(builder.uri, "uri should not null");
		Preconditions.checkNotNull(builder.monitor, "monitor should not null");

		this.uri = builder.uri;
		this.monitor = builder.monitor;
		this.discovery = builder.discovery;

		replies
//		.observeOn(Schedulers.io())
//		.subscribeOn(Schedulers.io())
		.subscribe(new Action1<String>() {
 			@Override
			public void call(String textFrame) {
  	            try {
  	            	log.debug("received reply 1: " + textFrame);
  					Long correlationId=RawBus.parseCid(textFrame);
  					if(correlationId!=null){
  	 					ReplyWaiter x=replyObserverQueue.poll(correlationId);
  	 					if(x!=null){
  	 	  	            	log.debug("received reply 2: " + textFrame);
  		 					x.subject.onNext(textFrame);
  		 					x.subject.onCompleted();
  	 	  	            	log.debug("received reply 3 completed: " + textFrame);

  	 					}
  					}
				} catch (Throwable e) {
  	            	log.error("received reply ReplyWaiter error : "+textFrame,e);
				}
			}
		});
		
		replyObserverQueue.evictions()
		.subscribe(new Action1<ReplyWaiter>() {
 			@Override
			public void call(ReplyWaiter replyWaiter) {
 				try {
 					replyWaiter.subject.onError(Check.ServiceTimeout.toCheckError().withDetail("Timeout : "+replyWaiter.message).toException());
				} catch (Exception e) {
					log.error("replyObserverQueue.subscribe",e);
 				}
			}
		});
		
		bindServer=RxNettyWsServer.create(uri.getPortResolveDefault());
		
		container.add(bindServer);
        container.add(
            	Component.newComponent(clients)
            	.on(IStop.class,new IStop(){
    				@Override public void stop() throws Exception {
    					for (IBusClient client : clients.values()) {
    						try {
    							client.close();
    						} catch (Exception e) {
    							log.error("close() "+client.getRemoteURI(),e);
    						}
    					}
    				}
    			})
            );
	}
	@Override public IComponent getComponent() {
 		return container;
	}

    public void start(){
    	container.start();
    }
    
    public void stop(){
    	container.stop();
    }
	public void startAndWait() {
		bindServer.startAndWait();
	}

 	private Observable<IBusClient> getOrConnectAsync(Service service) {
		IBusClient find = clients.get(service.getUri());
		if (find!=null) return Observable.just(find);
		
		final IBusClient newConnection = createClient(service.getUri());
		IBusClient oldConnection = clients.putIfAbsent(service.getUri(),newConnection);
		IBusClient useConnection = oldConnection == null ? newConnection:oldConnection;
 		
		if (useConnection == newConnection) {
			newConnection.getInput().subscribe(new Observer<String>() {
				@Override
				public void onCompleted() {
					//no completed
				}
					@Override
				public void onError(Throwable e) {
					//no error
//					replies.onError(e);
				}
					@Override
				public void onNext(String frame) {
					log.debug("getOrConnectAsync.onNext:"+frame);
					replies.onNext(frame);
				}
			});
			return newConnection.startAsync().map(new Func1<Void, IBusClient>() {
				@Override
				public IBusClient call(Void t1) {
					return newConnection;
				}
			});
		}else{//oldConnection
			return Observable.just(useConnection);
		}
	}

	private IBusClient createClient(Uri uri) {
		return RxNettyWsClient.create(uri.toURI());
	}

 	/**
	 * request/reply 模式,会收到reply
	 */
	public Observable<String> request(String channel, String message) {
		Long correlationId=null;
		try {
			correlationId=RawBus.parseCid(message);
		} catch (Throwable e) {
			return Observable.error(e);
		}
        return request(channel,correlationId,message);
 	}
 	/**
	 * request/reply 模式,会收到reply
	 */
	public Observable<String> request(String channel, final Long correlationId,String message) {
 		final AsyncSubject<String> result=AsyncSubject.create();
 		if(correlationId==null){
			return Observable.error(new BusException("Send Raw Request failed, No CorrelationId:"+message));
		}
		replyObserverQueue.put(correlationId, new ReplyWaiter(result,message));
 		Observable<Void> send = send(channel,message);
  		send.subscribe(new Observer<Void>() {
			@Override
			public void onCompleted() {
				//发送完成不代表已经回复
			}
			@Override
			public void onError(Throwable e) {
				result.onError(e);
				//删除失败的请求
				replyObserverQueue.poll(correlationId);
			}
			@Override
			public void onNext(Void t) {
			}
		 });
         return result.timeout(60,TimeUnit.SECONDS);
 	}

	/**
	 * send 模式,无reply
	 */
	public <OUT> Observable<Void> send(String channel, final String message) {
		Optional<Service> server = discovery.select(channel);
		if(!server.isPresent()){
			Check.NotFound info=new Check.NotFound();
			info.info="Service Not Found";
			String e=String.format("send failed,channel[%s] no subscriber", channel);
			return Observable.error(Check.NotFound.toCheckError(info).withDetail(e).toException());
		}
 		return getOrConnectAsync(server.get())
 	 			.flatMap(new Func1<IBusClient, Observable<Void>>() {
 				@Override
 				public Observable<Void> call(IBusClient t1) {
 					return t1.writeAndFlush(message);
 				}
 			});
 	}
	
	public void _register(String channel){
		Preconditions.checkNotNull(channel, "key should not be null");
		Preconditions.checkState(!this.subscribers.containsKey(channel), "already subscribed [%s]", channel);
		Service service=discovery.newService(channel, uri);
		this.subscribers.putIfAbsent(channel, service);
		discovery.register(service);
	}
	
	/**
	 * 订阅频道消息
	 */
	public Observable<IRawRequest> receive(final String channel) {
		_register(channel);
		return bindServer.getInput()
  				.filter(new Func1<IRawRequest,Boolean>() {
					@Override
					public Boolean call(IRawRequest received) {
 						try {
 							//TODO 多次解析receivedChannel造成性能瓶颈
 							String receivedChannel=RawBus.parseChannel(received.getMessage());
 	  						return Objects.equal(channel,receivedChannel);
						} catch (Throwable e) {
							log.error("parse message error",e);
							return false;
 						}
					}
				})
  		;
  	}
	
	public Observable<IRawRequest> receive() {
		return bindServer.getInput();
  	}

	/**
	 * 转换Bus消息协议文本为Message对象
	 */
	public static Long parseCid(String protocolText) {
		Map<String, String> result = JacksonHelper.find(protocolText, "cid");
	    return Long.parseLong(result.get("cid"));
	}
	public static String parseChannel(String protocolText) {
		Map<String, String> result = JacksonHelper.find(protocolText, "c");
	    return result.get("c");	
	}


}
