package firebats.http.server.exts.files;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.RequestHandlerWithErrorMapper;
import io.reactivex.netty.protocol.http.server.file.FileErrorResponseMapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import rx.Observable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.internal.http.server.rxnetty.ext.DirectoryRequestHandler;
/**
 * 静态文件服务
 *<br/>
 * 如果请求path为期望的前缀,则进入文件服务，否则执行链的下一个ext 。
 *<br/>
 * 假设你的文件放在下面路径： 
 * <ul>
 *   <li>/home/chen/resources/static/a.html</li>
 *   <li>/home/chen/resources/static/a/b.html</li>
 * </ul>
 *<br/>
 * 想通过下面地址访问：
 * <ul>
 *   <li>http://localhost:25000/x/y/a.html</li>
 *   <li>http://localhost:25000/x/y/a/b.html</li>
 * </ul>
 *<br/>
 * 则可以把/x/y映射到/home/chen/resources/static/目录：
 *<br/>
 * chain.link(new FilesExt("/x/y","/home/chen/resources/static"));
 */
public class FilesExt implements Ext<Context,Context>{
	private String path;
	private String files;
	
	final RequestHandlerWithErrorMapper<ByteBuf, ByteBuf> fileHandler;
	
	@Override
	public Observable<?> call(Chain<Context,Context> chain,final Context ctx) {
		if(decodeURL(ctx.getRequest().getPath()).startsWith(path)){
			return fileHandler.handle(ctx.getRequest(), ctx.getResponse());
		}else{
			return chain.next(ctx);
		}
	}
	public FilesExt(final String pathPrefix,final String files){
		this.path=pathPrefix;
	    this.files=files;
	    fileHandler= RequestHandlerWithErrorMapper.from(
	            new DirectoryRequestHandler(this.files,path),
	            new FileErrorResponseMapper());
	}
	private static String decodeURL(String data) {
		if (Strings.isNullOrEmpty(data)) { return data; }
		try {
			return URLDecoder.decode(data, Charsets.UTF_8.name());
		} catch (UnsupportedEncodingException wontHappen) {
			throw new IllegalStateException(wontHappen);
		}
	}
	@Override
	public String toString(){
		return "FormExt";
	}
}