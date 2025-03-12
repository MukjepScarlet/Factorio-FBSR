package com.demod.fbsr.bs;

import java.awt.geom.Point2D;

import moe.mukjep.fbsr.legacy.LegacyBlueprintTile;
import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSTile {
	public final BSPosition position;
	public final String name;

	public BSTile(JSONObject json) {
		position = BSUtils.position(json, "position");
		name = json.getString("name");
	}

	public BSTile(LegacyBlueprintTile legacy) {
		Point2D.Double pos = legacy.getPosition();
		position = new BSPosition(pos.x, pos.y);
		name = legacy.getName();
	}
}
