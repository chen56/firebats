package firebats.properties;

import java.net.URI;

import rx.functions.Action1;

import com.google.common.base.Strings;

import firebats.converter.Converter;
import firebats.net.Uri;
import firebats.render.IRender;
import firebats.templates.Template;
/**属性= 配置字段+模板*/
public class Property<TArg,TResult> implements IRender<TArg,TResult>{
	private String key;
	private Converter<String,TResult> converter;
	private IProperties properties;
	private Class<TArg> argClass;

 	public Converter<String, TResult> getConverter() {
		return converter;
	}
	public IProperties getProperties() {
		return properties;
	}
	public Class<TArg> getArgClass() {
		return argClass;
	}
	/*internal*/ Property(IProperties properties,Converter<String, TResult> converter,Class<TArg> argClass, String key) {
 		this.properties=properties;
		this.converter=converter; 
		this.key=key;
		this.argClass=argClass;
	}
 	public Property<TArg,TResult> withDefault(TResult defaultValue) {
 		return new Property<>(properties,converter.withDefault(defaultValue),argClass,key);
	}
 	
 	public <TNewArg> Property<TNewArg,TResult> withArg(Class<TNewArg> argClass){
 		return new Property<>(properties,converter,argClass,key);
 	}
 	
 	public <TNewResult> Property<TArg,TNewResult> withResult(Converter<String,TNewResult> converter ){
 		return new Property<>(properties,converter,argClass,key);
 	}
 	
 	public Property<TArg,Integer> withIntegerResult(){
 		return new Property<>(properties,Converter.String2Integer,argClass,key);
 	}
 	
 	public Property<TArg,String> withStringResult(){
 		return new Property<>(properties,Converter.String2String,argClass,key);
 	}
 	
 	public Property<TArg,Boolean> withBooleanResult(){
 		return new Property<>(properties,Converter.String2Boolean,argClass,key);
 	}
 	
 	public Property<TArg,Byte> withByteResult(){
 		return new Property<>(properties,Converter.String2Byte,argClass,key);
 	}
 	
 	public Property<TArg,Double> withDoubleResult(){
 		return new Property<>(properties,Converter.String2Double,argClass,key);
 	}
 	
 	public Property<TArg,Float> withFloatResult(){
 		return new Property<>(properties,Converter.String2Float,argClass,key);
 	}
 	public Property<TArg,Long> withLongResult(){
 		return new Property<>(properties,Converter.String2Long,argClass,key);
 	}
 	
 	public Property<TArg,Short> withShortResult(){
 		return new Property<>(properties,Converter.String2Short,argClass,key);
 	}
 	
 	public Property<TArg,URI> withURIResult(){
 		return new Property<>(properties,Converter.String2URI,argClass,key);
 	}
 	
 	public Property<TArg,Uri> withUriResult(){
 		return new Property<>(properties,Converter.String2Uri,argClass,key);
 	}
 	
 	public TResult get() {
 		String template=getString();
 		if(template==null){
	 		return converter.isWithDefault()?converter.getDefault():null; 
 		}
 		return getTemplate(template).get();
	}
	@Override
	public TResult get(Action1<TArg> argSetter) {
 		String template=getString();
 		if(template==null){
	 		return converter.isWithDefault()?converter.getDefault():null; 
 		}
		return getTemplate(template).get(argSetter);
	}
	@Override
	public TResult get(TArg arg) {
 		String template=getString();
 		if(template==null){
	 		return converter.isWithDefault()?converter.getDefault():null; 
 		}
		return getTemplate(template).get(arg);
	}
	
 	public void setString(String value){
 		properties.put(key, value);
 	}
 	
 	public void setStringIfEmpty(String value){
 		if(isEmpty()){
 			setString(value);
 		}
 	}
	public String getString() {
 		return properties.get(key);
	}
	@Override
	public String toString() {
		return String.format("%s:%s", key,getString());
	}
	public String getKey() {
		return key;
	}
	public boolean isEmpty(){
		return Strings.isNullOrEmpty(getString());
	}
	private IRender<TArg, TResult> getTemplate(String template) {
 		return Template.of(template,argClass, converter);
	}
}