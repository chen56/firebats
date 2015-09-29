package firebats.net;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestPath {
    @Test
    public void test1(){
        assertEquals("a/b",Path.fromPortableString("a/b").toPortableString());	
        assertEquals(false,Path.fromPortableString("a/b").isAbsolute());	
        assertEquals("/a/b/c",Path.fromPortableString("/a/b").append("c").toPortableString());	
        assertEquals("/a/b/c",Path.fromPortableString("/a/b").append("/c").toPortableString());	
        assertEquals(true,Path.fromPortableString("/a/b").isPrefixOf(Path.fromPortableString("/a/b")));	
        assertEquals(true,Path.fromPortableString("/a/b").isPrefixOf(Path.fromPortableString("/a/b")));	
        assertEquals(false,Path.fromPortableString("/a/b/c").isPrefixOf(Path.fromPortableString("/a/b")));	
        assertEquals(true,Path.fromPortableString("/a").isPrefixOf(Path.fromPortableString("/a/b")));	
    }
}