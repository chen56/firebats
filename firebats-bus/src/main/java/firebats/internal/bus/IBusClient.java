package firebats.internal.bus;

import java.net.URI;

import rx.Observable;

public interface IBusClient {

	public abstract URI getRemoteURI();

	public abstract Observable<Void> writeAndFlush(String message);

	public abstract Observable<String> getInput();

	public abstract Observable<Void> startAsync();

	public abstract void close();

}