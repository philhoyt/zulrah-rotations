package com.zulrahrotations;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ZulrahRotationsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ZulrahRotationsPlugin.class);
		RuneLite.main(args);
	}
}
