package debugCookies;

import org.apache.http.impl.cookie.BasicClientCookie;

import th.api.common.Ws;
import th.api.common.Ws.WsRequest;

public class Client {
public static void main(String[] args) {
	Ws ws=new Ws();
	BasicClientCookie cookie=new BasicClientCookie("a","b");
	cookie.setPath("/");
	cookie.setDomain("192.168.1.51");
//	WsRequest r = ws.newRequest("http://192.168.1.51:20000/app/x");
	WsRequest r = ws.newRequest("http://static.wildaidchina.org/app/user/login");
	r.addParameter("a"," a aa a ");
	r.addCookie(cookie);
	System.out.println(r.post().getString());
	ws.close();
}
}
