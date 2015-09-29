package firebats.internal.i18n;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import firebats.i18n.I18n;
import firebats.i18n.I18ns;
import rx.functions.Func1;

/**
 * 强类型化的语言资源集合，起到cache作用
 */
public class I18nWrappers<TWrapper>{
	private ConcurrentHashMap<Locale,TWrapper> lang_wrapper_map=new ConcurrentHashMap<>();
	private I18ns i18ns;
	private Func1<I18n, TWrapper> provider;
    public I18nWrappers(I18ns langResources,Func1<I18n,TWrapper> resourceProvider) {
		this.i18ns=langResources;
		this.provider=resourceProvider;
	}
	public TWrapper select(String langRanges) {
		I18n i18n=i18ns.select(langRanges);
		TWrapper result=lang_wrapper_map.get(i18n.getLang());
		if(result==null){
			TWrapper newValue=provider.call(i18n);
			TWrapper oldValue=lang_wrapper_map.putIfAbsent(i18n.getLang(), newValue);
			TWrapper useValue = oldValue == null ? newValue:oldValue;
            return useValue;
		}
		return result;
	}
}