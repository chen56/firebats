package firebats.i18n;

import java.util.Locale;

import firebats.properties.IProperties;

public class I18n{
	private IProperties resource;
	private Locale lang;
	/*internal*/ I18n(Locale lang,IProperties messageResource){
		this.lang=lang;
		this.resource=messageResource;
	}
	
	public IProperties getResource() {
		return resource;
	}
	public Locale getLang() {
		return lang;
	}
	public String get(String messageKey) {
		return resource.get(messageKey);
	}
	@Override public String toString() {
		return super.toString();
	}
}