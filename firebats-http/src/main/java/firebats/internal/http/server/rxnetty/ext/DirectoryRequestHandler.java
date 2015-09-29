package firebats.internal.http.server.rxnetty.ext;


 
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.reactivex.netty.protocol.http.server.HttpError;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.file.AbstractFileRequestHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action0;

/**
 * FileRequestHandler that reads files from the file system
 * 
 * @author elandau
 */
public class DirectoryRequestHandler extends AbstractFileRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryRequestHandler.class);
    
    private static final int CHUNK_SIZE = 8192;
    
    private final Path prefix;
	private Path root;
    public DirectoryRequestHandler(String root,String prefix) {
        this.root=Paths.get(root);
        this.prefix=Paths.get(prefix);
    }
    
    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        // We don't support GET.  
        if (!request.getHttpMethod().equals(GET)) {
            return Observable.error(new HttpError(METHOD_NOT_ALLOWED));
        }
        logger.debug("getUri "+request.getUri());
        logger.debug("getPath "+request.getPath());

        String sanitizedUri = sanitizeUri(request.getPath());

        if (sanitizedUri == null) {
            return Observable.error(new HttpError(FORBIDDEN));
        }
        logger.debug("sanitizedUri "+sanitizedUri);
        URI uri=null;
        try {
        	uri = resolveUri(sanitizedUri);
		} catch (Exception e) {
			logger.debug("resolveUri error:"+sanitizedUri,e);
            return Observable.error(new HttpError(NOT_FOUND));
		}
         if (uri == null) {
            return Observable.error(new HttpError(NOT_FOUND));
        }
        File file = new File(uri);
        logger.debug("uri "+uri); 
        logger.debug("file "+file.getAbsolutePath());

        if (file.isHidden() || !file.exists()) {
            return Observable.error(new HttpError(NOT_FOUND));
        }

        if (file.isDirectory()) {
            return Observable.error(new HttpError(FORBIDDEN));
        }
        
        if (!file.isFile()) {
            return Observable.error(new HttpError(FORBIDDEN));
        }

        return write(request, response, uri, file);
    }


    protected URI resolveUri(String path) {
     	Path x = prefix.relativize(Paths.get(path));
    	Path filename = root.resolve(x);
    	logger.debug("aaaaaaaaa "+path+" > " +x+" > "+filename.toFile().getAbsolutePath()+" > "+filename.toUri());

    	URI uri = filename.toUri();
        try {
             File file = new File(filename.toUri());
            if (!file.exists()) {
            	logger.debug("File '{}' not found", filename);
                return null;
            }
            return uri;
        } catch (Throwable	 e) {
        	logger.debug("Error resolving uri for '{}'", filename);
            return null;
        }
    }
    
	public static Observable<Void> write(HttpServerRequest<ByteBuf> request,
			HttpServerResponse<ByteBuf> response, URI uri, File file) {
        RandomAccessFile raf = null;
		long fileLength;
        try {
            raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
        }
        catch (Exception e) {
            logger.warn("Error accessing file {}", uri, e);
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e1) {
                    logger.warn("Error closing file {}", uri, e1);
                }
            }
            return Observable.error(e);
        }
        
        // Cache Validation
        String ifModifiedSince = request.getHeaders().get(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
            } catch (ParseException e) {
                logger.warn("Failed to parse {} header", IF_MODIFIED_SINCE);
            }

            if (ifModifiedSinceDate != null) {
                // Only compare up to the second because the datetime format we send to the client
                // does not have milliseconds
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    response.setStatus(NOT_MODIFIED);
                    setDateHeader(response, dateFormatter);
                    return response.close().doOnTerminate(new CloseFile(raf));
                }
            }
        }
        
        response.setStatus(OK);
        response.getHeaders().setContentLength(fileLength);
        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);
        
        if (request.getHeaders().isKeepAlive()) {
            response.getHeaders().set(CONNECTION, KEEP_ALIVE);
        }
        
        if (response.getChannel().pipeline().get(SslHandler.class) == null) {
            response.writeFileRegion(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
        }
        else {
            try {
                response.writeChunkedInput(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, CHUNK_SIZE)));
            } catch (IOException e) {
                logger.warn("Failed to write chunked file {}", e);
                return Observable.error(e);
            }
        }
        
        return response.close().doOnTerminate(new CloseFile(raf));
	}
	private static class CloseFile implements Action0 {
		private RandomAccessFile raf;
 		public CloseFile(RandomAccessFile raf) {
			this.raf = raf;
		}
		@Override
		public void call() {
			try {
				raf.close();
			} catch (IOException e) {
				logger.warn("Failed to close file {}", e);
			}
		}
	}

    public static String sanitizeUri(String uri)  {
        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(String.format("Unable to decode URI '%s'", uri), e);
        }

        if (!uri.startsWith("/")) {
            return null;
        }

        // Convert file separators.
        uri = uri.replace('/', File.separatorChar);

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if (uri.contains(File.separator + '.') ||
            uri.contains('.' + File.separator) ||
            uri.startsWith(".") || uri.endsWith(".") ||
            INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        // Convert to absolute path.
        return uri;
    }  

}
