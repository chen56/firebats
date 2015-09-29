package firebats.net;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Test;

import firebats.net.ConnectionString;

public class TestConnectionString {
    @Test
    public void parse_完整格式(){
    	ConnectionString c=ConnectionString.parse("kafka://host1:1000,host2:2000/a/b");
    	assertEquals("/a/b",c.getPath());
    	assertEquals("kafka",c.getScheme());
    	assertEquals("host1:1000,host2:2000",c.getHostPortsString());
    }
    
    @Test
    public void parse_noScheme() throws URISyntaxException{
    	ConnectionString c=ConnectionString.parse("host1:1000,host2:2000/a/b");
    	assertEquals("/a/b",c.getPath());
    	assertEquals("temp",c.getScheme());
    	assertEquals("host1:1000,host2:2000",c.getHostPortsString());
    }
    
    @Test
    public void parse_noPath() throws URISyntaxException{
    	ConnectionString c=ConnectionString.parse("host1:1000,host2:2000");
    	assertEquals("/",c.getPath());
    	assertEquals("temp",c.getScheme());
    	assertEquals("host1:1000,host2:2000",c.getHostPortsString());
    }

}