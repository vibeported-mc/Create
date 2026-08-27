package com.simibubi.create.foundation.gui.menu;

import net.createmod.catnip.api.network.NetworkHelper;


public interface IClearableMenu {

	default void sendClearPacket() {
		NetworkHelper.INSTANCE.sendToServer(ClearMenuPacket.INSTANCE);
	}

	void clearContents();

}
