package firebats.check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import firebats.check.Check;
import firebats.check.Check.ParamInvalid;
import rx.functions.Action1;

public class TestCheck {
	@Test
	public void error_with_action_pararmeter() {
		try {
			Check.ParamInvalid.check(false,new Action1<ParamInvalid>() {
				@Override
				public void call(ParamInvalid p) {
					p.name="sex";
					p.value=""+null;
				}
			});
            fail();
		} catch (Exception e) {
			assertEquals("param invalid[sex]=[null]",e.getMessage());
		}
	}
	
	@Test
	public void error_with_object_pararmeter() {
		try {
			Check.ParamInvalid.check(false,new Check.ParamInvalid(){{{name="sex";value=""+null;}}});
            fail();
		} catch (Exception e) {
			assertEquals("param invalid[sex]=[null]",e.getMessage());
		}
	}

	@Test
	public void errorCode可以重复注册() {
		Check.e(100000, "..." );
		Check.e(100000, "..." );
	}

	@Test
	public void httpCode() {
		assertEquals(400,Check.ParamInvalid.toCheckError().asHttpCode());
		assertEquals(404,Check.NotFound.toCheckError().asHttpCode());
	}

}
