package firebats.http.server.exts.form;

import firebats.http.server.ContextAware;

public interface FormAware extends ContextAware{
    /**
    * get form.
    */
    default Form getForm(){
		return FormExt.get(getContext());
    }
}