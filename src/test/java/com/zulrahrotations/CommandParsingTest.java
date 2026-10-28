package com.zulrahrotations;

import com.zulrahrotations.data.Rotation;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class CommandParsingTest
{
	@Test
	public void acceptsNumbersAndNames()
	{
		assertSame(Rotation.CRIMSON_A, ZulrahRotationsPlugin.parseRotation("1"));
		assertSame(Rotation.CRIMSON_B, ZulrahRotationsPlugin.parseRotation(" 2 "));
		assertSame(Rotation.SERP, ZulrahRotationsPlugin.parseRotation("3"));
		assertSame(Rotation.TANZ, ZulrahRotationsPlugin.parseRotation("4"));
		assertSame(Rotation.CRIMSON_A, ZulrahRotationsPlugin.parseRotation("A"));
		assertSame(Rotation.CRIMSON_B, ZulrahRotationsPlugin.parseRotation("b"));
		assertSame(Rotation.SERP, ZulrahRotationsPlugin.parseRotation("Serp"));
		assertSame(Rotation.TANZ, ZulrahRotationsPlugin.parseRotation("tanzanite"));
	}

	@Test
	public void rejectsGarbage()
	{
		assertNull(ZulrahRotationsPlugin.parseRotation("0"));
		assertNull(ZulrahRotationsPlugin.parseRotation("5"));
		assertNull(ZulrahRotationsPlugin.parseRotation("x"));
		assertNull(ZulrahRotationsPlugin.parseRotation(""));
	}
}
