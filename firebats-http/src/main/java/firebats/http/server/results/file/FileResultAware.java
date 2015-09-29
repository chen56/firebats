package firebats.http.server.results.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import firebats.http.server.ContextAware;

public interface FileResultAware extends ContextAware{
    /**
    * file,   Generates a File result.
    *
    * @param path The path for load.
    */
    default FileResult file(String path){
    	Path p=Paths.get(path);
    	p=p.isAbsolute()?p:getFileBasePath().resolve(p);
        File file = p.toFile();
//        
//        if (!file.exists()) {
//            throw new FileNotFoundException(file.getAbsolutePath());
//        }
        return new FileResult(getContext(),file);
    }
    
    Path getFileBasePath();
}