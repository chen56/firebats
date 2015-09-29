package firebats.http.client;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import com.google.common.net.HostAndPort;

import firebats.net.Uri;

public class RestClient{
	private Uri baseUri;
	private HttpClient<String, ByteBuf> httpClient;
	public static RestClient create(Uri baseUri){
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
        = PipelineConfigurators.httpClientConfigurator();
        HttpClient<String, ByteBuf> client = RxNetty.<String, ByteBuf>newHttpClientBuilder(baseUri.getHost(),baseUri.getPortResolveDefault())
                .pipelineConfigurator(pipelineConfigurator)
                .build();

		return new RestClient(baseUri,client);
	}
	private RestClient(Uri baseUri,HttpClient<String, ByteBuf> client){
		this.baseUri=baseUri;
		this.httpClient=client;
	}
	public HostAndPort getHostPort() {
		return HostAndPort.fromParts(baseUri.getHost(),baseUri.getPort());
	}
	public HttpClient<String, ByteBuf> getHttpClient() {
		return httpClient;
	}
}