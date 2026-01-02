package com.minecrafttas.tasmod.mixin;

import com.minecrafttas.mctcommon.networking.Client;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

@Mixin(GuiMainMenu.class)
public class MixinMainMenu extends GuiScreen {
    private GuiTextField hostTextField;

    @Inject(at = @At("RETURN"), method = "initGui")
    private void addServerConnect(CallbackInfo ci) {
        this.buttonList.add(new GuiButton(69, 200, 0, 50, 20, I18n.format("gui.tasmod.mainmenu.button.connect", new Object[0])));
        this.hostTextField= new GuiTextField(70, this.fontRenderer, 0, 0, 200, 20);
        if (TASmodClient.client != null) {
            this.hostTextField.setText(TASmodClient.client.getIp() + ":" + TASmodClient.client.getPort());
        }
    }

    @Inject(at=@At("RETURN"), method="drawScreen")
    private void drawScreen(CallbackInfo ci) {
        this.hostTextField.drawTextBox();

        String s;
        if (TASmodClient.client == null) {
            s = "gui.tasmod.mainmenu.status.disconnected";
        } else {
            switch (TASmodClient.client.clientState) {
                case DISCONNECTED:
                    s = "gui.tasmod.mainmenu.status.disconnected";
                    break;
                case CONNECTING:
                    s = "gui.tasmod.mainmenu.status.connecting";
                    break;
                case AUTHENTICATING:
                    s = "gui.tasmod.mainmenu.status.authenticating";
                    break;
                case CONNECTED:
                    s = "gui.tasmod.mainmenu.status.connected";
                    break;
                default:
                    s = "gui.tasmod.mainmenu.status.disconnected";
            }
        }

        String text = I18n.format(s, new Object[0]);
        int textWidth = this.fontRenderer.getStringWidth(text);
        this.drawString(this.fontRenderer, text, this.width - textWidth - 2, 5, -1);
    }

    @Inject(at=@At("RETURN"), method = "keyTyped")
    private void keyTyped(char c, int i, CallbackInfo ci) {
        if (this.hostTextField.isFocused()) {
            this.hostTextField.textboxKeyTyped(c, i);
            //this.hostText = this.hostTextField.getText();
        }
    }

    @Inject(at=@At("RETURN"), method="mouseClicked")
    private void mouseClicked(int i, int j, int k, CallbackInfo ci) {
        this.hostTextField.mouseClicked(i, j, k);
    }

    @Inject(at=@At("RETURN"), method = "actionPerformed")
    private void actionPerfomed(GuiButton guiButton, CallbackInfo ci) {
        if (guiButton.enabled) {
            if (guiButton.id == 69) {
                connect(this.hostTextField.getText());
            }
        }
    }

    private void connect(String string) {
        String[] parts = string.split(":");

        if (parts.length == 2) {
            String host = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
                connect(host, port);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid port number: " + parts[1]);
            }
        } else if (parts.length == 1) {
            connect(parts[0], TASmod.networkingport - 1);
        } else {
            LOGGER.error("too many parts");
        }
    }
    private void connect(String host, int port) {
        if (TASmodClient.client != null) {
            if (TASmodClient.client.getIp().equals(host) && TASmodClient.client.getPort() == port) return;

            TASmodClient.client.disconnect();
            TASmodClient.client = null;
        }
        try {
            TASmodClient.client = new Client(host, port, TASmodPackets.values(), mc.getSession().getUsername(), true);
        } catch (Exception e) {
            LOGGER.error("Unable to connect TASmod client: {}", e);
        }
    }
}
