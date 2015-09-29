package firebats.converter;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.base.Function;

import firebats.net.Uri;

public class Converter<F,T> implements Function<F, T> {
	private Function<F, T> func;
	private T defaultValue;
	private boolean hasDefault;
	private Converter(Function<F,T> func,boolean useDefaultValue,T defaultValue){
		this.func=func;
		this.hasDefault=useDefaultValue;
		this.defaultValue=defaultValue;
	}
	public Converter<F, T> withDefault(T defaultValue){
		return new Converter<F,T>(func,true,defaultValue);
	}
	public Converter<F, T> withoutDefault(){
		return new Converter<F,T>(func,false,defaultValue);
	}
	@Override
	public T apply(F input) {
		try {
			T result = this.func.apply(input);
			return result==null && defaultValue!=null ? defaultValue : result;
		} catch (RuntimeException e) {
			if(hasDefault){
				return defaultValue;
			}else{
				throw e;
			}
		}
	}
	public boolean isWithDefault(){
		return hasDefault;
	}
	public T getDefault(){
		return defaultValue;
	}
	public static <F,T> Converter<F,T> of(Function<F,T> func){
		return new Converter<F,T>(func,false,null);
	}
	
	public static Converter<String, String> String2String=Converter.of(new Function<String, String>() {
		@Override
		public String apply(String input) {
			return input;
		}
	});
	
 	public static Converter<String, URI> String2URI= Converter.of(new Function<String, URI>() {
			@Override
			public URI apply(String input) {
				try {
					return new URI(input);
				} catch (URISyntaxException e) {
					throw new RuntimeException(e.getMessage(),e);
				}
			}
		});
	
 	public static Converter<String, Uri> String2Uri= Converter.of(new Function<String, Uri>() {
		@Override
		public Uri apply(String input) {
			return Uri.parse(input);
		}
	});


	
	public static Converter<String, Short> String2Short=Converter.of(new Function<String, Short>() {
			@Override
			public Short apply(String input) {
				return Short.valueOf(input);
			}
		});
	
	public static Converter<String, Byte> String2Byte=Converter.of(new Function<String, Byte>() {
			@Override
			public Byte apply(String input) {
				return Byte.valueOf(input);
			}
		});
	

	public static Converter<String, Integer> String2Integer=Converter.of(new Function<String, Integer>() {
			@Override
			public Integer apply(String input) {
				return Integer.valueOf(input);
			}
		});
	

	public static Converter<String, Long> String2Long= Converter.of(new Function<String, Long>() {
			@Override
			public Long apply(String input) {
				return Long.valueOf(input);
			}
		});
	

	public static Converter<String, Double> String2Double= Converter.of(new Function<String, Double>() {
			@Override
			public Double apply(String input) {
				return Double.valueOf(input);
			}
		});
	

	
	public static Converter<String, Float> String2Float= Converter.of(new Function<String, Float>() {
			@Override
			public Float apply(String input) {
				return Float.valueOf(input);
			}
		});
	
	
	public static Converter<String, Boolean> String2Boolean= Converter.of(new Function<String, Boolean>() {
			@Override
			public Boolean apply(String input) {
				return Boolean.valueOf(input);
			}
		});
	

}