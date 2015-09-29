package firebats.i18n;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Func1;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import firebats.internal.i18n.I18nWrappers;
import firebats.properties.IProperties;
import firebats.properties.memory.EchoProperties;

public class I18ns{
	private static Logger log=LoggerFactory.getLogger(I18ns.class);
	private Map<Locale,I18n> lang_resource_map=new LinkedHashMap<>();
	private Map<Class<?>,I18nWrappers<?>> class_wrappers_map=new ConcurrentHashMap<Class<?>,I18nWrappers<?>>();
	private volatile I18n defaultI18n;
	private static EchoProperties EchoProperties=new EchoProperties();
	private I18ns(I18n defaultI18n) {
		this.defaultI18n=defaultI18n;
	}
	public static I18ns create(String defaultLang) {
		return create(Locale.forLanguageTag(defaultLang));
	}
	public static I18ns create(Locale defaultLang) {
		return new I18ns(new I18n(defaultLang,EchoProperties));
	}
	
	public void put(String lang,IProperties messageResource){
		put(Locale.forLanguageTag(lang),messageResource);
	}
	public void put(Locale lang,IProperties messageResource){
		I18n i18n=new I18n(lang,messageResource);
		//替换EchoProperties缺省资源
		if(Objects.equal(lang,defaultI18n.getLang())){
			this.defaultI18n=i18n;
		}
		lang_resource_map.put(lang, i18n);
	}
	public boolean isEmpty() {
		return defaultI18n.getResource()==EchoProperties;
	}
	public I18n getOrDefault(String lang) {
		I18n result = lang_resource_map.get(Locale.forLanguageTag(lang));
		return result==null?defaultI18n:result;
	}
	public Optional<I18n> get(String lang) {
		return Optional.fromNullable(lang_resource_map.get(Locale.forLanguageTag(lang)));
	}
	public I18n getDefault(){
		return defaultI18n;
	}
	/**
	 * @param ranges 语言范围，从http头中取出此参数： Accept-Language:en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4
	 */
	public I18n select(String ranges) {
		if(ranges==null)return defaultI18n;
		
		List<LanguageRange> languageRanges;
		try {
			languageRanges = Locale.LanguageRange.parse(ranges);
		} catch (Exception e) {
			//忽略ranges语法导致的错误
			if(log.isDebugEnabled()){
				log.debug(String.format("ranges[%s] Invalid",ranges),e);
			}
			return defaultI18n;
 		}
		if(languageRanges.isEmpty()) return defaultI18n;
//		List<Locale> selections = Locale.filter(languageRanges,lang_resource_map.keySet());
 //		if(selections.isEmpty())return defaultI18n;
//		return lang_resource_map.get(selections.iterator().next());
		Locale lookuped = Locale.lookup(languageRanges,lang_resource_map.keySet());
		//若未找到适合的资源，则返回缺省资源
 		return lookuped==null?defaultI18n:lang_resource_map.get(lookuped);
	}

	/**
	 * select一个类型安全的i18n包装器,内部会缓存包装器以保证不必要的大量对象的创建
	 * 
	 * @param ranges 语言范围，从http头中取出此参数： Accept-Language:en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4
	 */
	public <TWrapper> TWrapper select(String langRanges,Class<TWrapper> wrapperClass,Func1<I18n, TWrapper> wrapperProvider) {
 		@SuppressWarnings("unchecked")
		I18nWrappers<TWrapper> result=(I18nWrappers<TWrapper>)class_wrappers_map.get(wrapperClass);
		if(result==null){
			I18nWrappers<TWrapper> newValue=new I18nWrappers<TWrapper>(this,wrapperProvider);
			@SuppressWarnings("unchecked")
			I18nWrappers<TWrapper> oldValue=(I18nWrappers<TWrapper>)class_wrappers_map.putIfAbsent(wrapperClass, newValue);
			I18nWrappers<TWrapper> useValue = oldValue == null ? newValue:oldValue;
            return useValue.select(langRanges);
		}
		return result.select(langRanges);
	}
}