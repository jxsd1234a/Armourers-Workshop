package riskyken.armourersWorkshop.common.items;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.vecmath.Point3i;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import riskyken.armourersWorkshop.api.common.equipment.skin.IEquipmentSkinPart;
import riskyken.armourersWorkshop.api.common.equipment.skin.IEquipmentSkinPartTextured;
import riskyken.armourersWorkshop.api.common.equipment.skin.IEquipmentSkinType;
import riskyken.armourersWorkshop.api.common.painting.IPaintingTool;
import riskyken.armourersWorkshop.api.common.painting.IPantable;
import riskyken.armourersWorkshop.api.common.painting.IPantableBlock;
import riskyken.armourersWorkshop.client.lib.LibItemResources;
import riskyken.armourersWorkshop.common.SkinHelper;
import riskyken.armourersWorkshop.common.blocks.ModBlocks;
import riskyken.armourersWorkshop.common.lib.LibItemNames;
import riskyken.armourersWorkshop.common.lib.LibSounds;
import riskyken.armourersWorkshop.common.network.PacketHandler;
import riskyken.armourersWorkshop.common.network.messages.MessageClientGuiToolOptionUpdate;
import riskyken.armourersWorkshop.common.tileentities.TileEntityArmourerBrain;
import riskyken.armourersWorkshop.common.tileentities.TileEntityBoundingBox;
import riskyken.armourersWorkshop.utils.PaintingNBTHelper;
import riskyken.armourersWorkshop.utils.UtilColour;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemColourPicker extends AbstractModItem implements IPaintingTool {
    
    public ItemColourPicker() {
        super(LibItemNames.COLOUR_PICKER);
    }
    
    @SideOnly(Side.CLIENT)
    private IIcon tipIcon;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(LibItemResources.COLOUR_PICKER);
        tipIcon = register.registerIcon(LibItemResources.COLOUR_PICKER_TIP);
    }
    
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
            int side, float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(x, y, z);
        
        if (player.isSneaking() & block == ModBlocks.colourMixer & getToolHasColour(stack)) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te != null && te instanceof IPantable) {
                if (!world.isRemote) {
                    int colour = getToolColour(stack);
                    ((IPantable)te).setColour(colour);
                }
            }
            return true;
        }
        
        
        if (block instanceof IPantableBlock) {
            if (!world.isRemote) {
                setToolColour(stack, ((IPantableBlock)block).getColour(world, x, y, z));
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, LibSounds.PICKER, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
            }
            return true;
        }
        
        if (block == ModBlocks.boundingBox) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te != null && te instanceof TileEntityBoundingBox && world.isRemote) {
                TileEntityArmourerBrain parent = ((TileEntityBoundingBox)te).getParent();
                if (parent != null) {
                    IEquipmentSkinType skinType = parent.getSkinType();
                    if (skinPartHasTexture(((TileEntityBoundingBox)te).getSkinPart())) {
                        int colour = getColourFromSkin((TileEntityBoundingBox)te, player, world, x, y, z, side);
                        PacketHandler.networkWrapper.sendToServer(new MessageClientGuiToolOptionUpdate((byte)1, colour));
                    }
                }
            }
            if (!world.isRemote) {
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, LibSounds.PICKER, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
            }
            return true;
        }
        return false;
    }
    
    private boolean skinPartHasTexture(IEquipmentSkinPart skinPart) {
        return skinPart instanceof IEquipmentSkinPartTextured;
    }
    
    private int getColourFromSkin(TileEntityBoundingBox te, EntityPlayer player, World world, int x, int y, int z, int side) {
        IEquipmentSkinPartTextured skinPart = (IEquipmentSkinPartTextured) te.getSkinPart();
        Point textureLocation = skinPart.getTextureLocation();
        Point3i textureModelSize = skinPart.getTextureModelSize();
        ForgeDirection blockFace = ForgeDirection.getOrientation(side);
        GameProfile gameProfile = te.getParent().getGameProfile();
        
        byte blockX = te.getGuideX();
        byte blockY = te.getGuideY();
        byte blockZ = te.getGuideZ();
        
        int textureX = textureLocation.x;
        int textureY = textureLocation.y;
        
        int shiftX = 0;
        int shiftY = 0;
        
        switch (blockFace) {
        case EAST:
            textureY += textureModelSize.z;
            shiftX = (byte) (-blockZ + textureModelSize.z - 1);
            shiftY = (byte) (-blockY + textureModelSize.y - 1);
            break;
        case NORTH:
            textureX += textureModelSize.z;
            textureY += textureModelSize.z;
            shiftX = (byte) (-blockX + textureModelSize.x - 1);
            shiftY = (byte) (-blockY + textureModelSize.y - 1);
            break;
        case WEST:
            textureX += textureModelSize.z + textureModelSize.x;
            textureY += textureModelSize.z;
            shiftX = blockZ;
            shiftY = (byte) (-blockY + textureModelSize.y - 1);
            break;
        case SOUTH:
            textureX += textureModelSize.z + textureModelSize.x + textureModelSize.z;
            textureY += textureModelSize.z;
            shiftX = blockX;
            shiftY = (byte) (-blockY + textureModelSize.y - 1);
            break;
        case DOWN:
            textureX += textureModelSize.z + textureModelSize.x;
            shiftX = (byte) (-blockX + textureModelSize.x - 1);
            shiftY = (byte) (-blockZ + textureModelSize.z - 1);
            break;
        case UP:
            textureX += textureModelSize.z;
            shiftX = (byte) (-blockX + textureModelSize.x - 1);
            shiftY = (byte) (-blockZ + textureModelSize.z - 1);
            break;
        default:
            break;
        }
        
        textureX += shiftX;
        textureY += shiftY;
        
        BufferedImage playerSkin = SkinHelper.getBufferedImageSkin(gameProfile);
        int colour = UtilColour.getMinecraftColor(0);
        if (playerSkin != null) {
            colour = playerSkin.getRGB(textureX, textureY);
        }
        return colour;
    }
    
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_) {
        super.addInformation(stack, player, list, p_77624_4_);
        String cGray = EnumChatFormatting.GRAY.toString();
        String cGold = EnumChatFormatting.GOLD.toString();
        if (getToolHasColour(stack)) {
            Color c = new Color(getToolColour(stack));
            String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            list.add(cGold + "Colour: " + cGray + c.getRGB());
            list.add(cGold + "Hex: " + cGray + hex);
        } else {
            list.add("No paint");
        }
    }
    
    @Override
    public boolean requiresMultipleRenderPasses() {
        return true;
    }
    
    @Override
    public int getRenderPasses(int metadata) {
        return 2;
    }
    
    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        if (!getToolHasColour(stack)) {
            return super.getColorFromItemStack(stack, pass);
        }
        
        if (pass == 0) {
            return super.getColorFromItemStack(stack, pass);
        }
        return getToolColour(stack);
    }
    
    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        if (!getToolHasColour(stack)) {
            return itemIcon;
        }
        if (pass == 0) {
            return itemIcon;
        }
        return tipIcon;
    }
    
    @Override
    public boolean getToolHasColour(ItemStack stack) {
        return PaintingNBTHelper.getToolHasColour(stack);
    }

    @Override
    public int getToolColour(ItemStack stack) {
        return PaintingNBTHelper.getToolColour(stack);
    }

    @Override
    public void setToolColour(ItemStack stack, int colour) {
        PaintingNBTHelper.setToolColour(stack, colour);
    }
}
