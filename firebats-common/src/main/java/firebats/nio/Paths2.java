package firebats.nio;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Function;

import firebats.converter.Converter;

public class Paths2 {
 	public static Converter<String, Path> String2Path() {
		return Converter.of(new Function<String, Path>() {
			@Override
			public Path apply(String input) {
				return Paths.get(input);
			}
		});
	}
 	public static Path path(String first, String... more){
 		return Paths.get(first, more);
 	} 
    public static Path path(URI uri) {
 		return Paths.get(uri);
 	} 
}
