package firebats.http.server.old;

import io.netty.buffer.ByteBuf;
import firebats.http.server.exts.form.Form;

/**
 * 
 *  @Deprecated 替代为：使用ext自行创建
 * */
@Deprecated
public class FormContext extends RequestContext{
	private Form form;
 	public FormContext(ByteBuf content, RequestContext requestContext) {
		super(requestContext);
 		form=Form.decode(this, content);
	}
    protected FormContext(FormContext context){
    	super(context);
    	this.form=context.getForm();
    }
	public Form getForm() {
		return form;
	}
}