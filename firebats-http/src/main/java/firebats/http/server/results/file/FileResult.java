package firebats.http.server.results.file;

import java.io.File;
import java.nio.file.Path;

import rx.Observable;
import firebats.http.server.Context;
import firebats.http.server.exts.result.HttpResult;

public class FileResult extends HttpResult<FileResult>{
	private File file;
	/**internal*/ FileResult(Context ctx,File file) {
		super(ctx);
 		this.file=file;
 	}
	@Override
	public Observable<Void> render() {
		throw new RuntimeException("not impl");
// 		ctx.getResponse().writeString(body);
	}
	@Override
	public String toString() {
 		return "FileResult:"+file;
	}
	public static FileResultAware factory(Context ctx,Path fileBasePath) {
		return new FileResultAware(){
			@Override
			public Context getContext() {
				return ctx;
			}
 			@Override
			public Path getFileBasePath() {
 				return fileBasePath;
			}};
	}
	@Override
	public Object getDebugableResult() {
 		return ""+file;
	}
}