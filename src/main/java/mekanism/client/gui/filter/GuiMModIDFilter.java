package mekanism.client.gui.filter;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.client.gui.button.GuiButtonDisableableImage;
import mekanism.client.gui.button.GuiButtonTranslation;
import mekanism.common.Mekanism;
import mekanism.common.OreDictCache;
import mekanism.common.content.miner.MModIDFilter;
import mekanism.common.network.PacketDigitalMinerGui;
import mekanism.common.network.PacketDigitalMinerGui.MinerGuiPacket;
import mekanism.common.network.PacketEditFilter;
import mekanism.common.network.PacketNewFilter;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.TextComponentUtil;
import mekanism.common.util.text.Translation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiMModIDFilter extends GuiModIDFilter<MModIDFilter, TileEntityDigitalMiner> {

    public GuiMModIDFilter(PlayerEntity player, TileEntityDigitalMiner tile, int index) {
        super(player, tile);
        origFilter = (MModIDFilter) tileEntity.filters.get(index);
        filter = ((MModIDFilter) tileEntity.filters.get(index)).clone();
        updateStackList(filter.getModID());
    }

    public GuiMModIDFilter(PlayerEntity player, TileEntityDigitalMiner tile) {
        super(player, tile);
        isNew = true;
        filter = new MModIDFilter();
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiMModIDFilter.png");
    }

    @Override
    protected void updateStackList(String modName) {
        iterStacks = OreDictCache.getModIDStacks(modName, true);
        stackSwitch = 0;
        stackIndex = -1;
    }

    @Override
    protected void addButtons() {
        buttons.add(saveButton = new GuiButtonTranslation(guiLeft + 27, guiTop + 62, 60, 20, "gui.save", onPress -> {
            if (!text.getText().isEmpty()) {
                setText();
            }
            if (filter.getModID() != null && !filter.getModID().isEmpty()) {
                if (isNew) {
                    Mekanism.packetHandler.sendToServer(new PacketNewFilter(Coord4D.get(tileEntity), filter));
                } else {
                    Mekanism.packetHandler.sendToServer(new PacketEditFilter(Coord4D.get(tileEntity), false, origFilter, filter));
                }
                sendPacketToServer(0);
            } else {
                status = TextComponentUtil.build(EnumColor.DARK_RED, Translation.of("gui.modIDFilter.noID"));
                ticker = 20;
            }
        }));
        buttons.add(deleteButton = new GuiButtonTranslation(guiLeft + 89, guiTop + 62, 60, 20, "gui.delete", onPress -> {
            Mekanism.packetHandler.sendToServer(new PacketEditFilter(Coord4D.get(tileEntity), true, origFilter, null));
            sendPacketToServer(0);
        }));
        buttons.add(backButton = new GuiButtonDisableableImage(guiLeft + 5, guiTop + 5, 11, 11, 176, 11, -11, getGuiLocation(),
              onPress -> sendPacketToServer(isNew ? 4 : 0)));
        buttons.add(replaceButton = new GuiButtonDisableableImage(guiLeft + 148, guiTop + 45, 14, 14, 199, 14, -14, getGuiLocation(),
              onPress -> filter.requireStack = !filter.requireStack));
        buttons.add(checkboxButton = new GuiButtonDisableableImage(guiLeft + 131, guiTop + 47, 12, 12, 187, 12, -12, getGuiLocation(),
              onPress -> setText()));
    }

    @Override
    protected void sendPacketToServer(int guiID) {
        Mekanism.packetHandler.sendToServer(new PacketDigitalMinerGui(MinerGuiPacket.SERVER, Coord4D.get(tileEntity), guiID, 0, 0));
    }
}