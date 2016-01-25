package me.ichun.mods.ichunutil.client.core.event;

import me.ichun.mods.ichunutil.client.entity.EntityLatchedRenderer;
import me.ichun.mods.ichunutil.client.keybind.KeyBind;
import me.ichun.mods.ichunutil.client.module.eula.WindowAnnoy;
import me.ichun.mods.ichunutil.client.module.patron.LayerPatronEffect;
import me.ichun.mods.ichunutil.client.module.patron.ModelVoxel;
import me.ichun.mods.ichunutil.client.module.patron.PatronTracker;
import me.ichun.mods.ichunutil.client.module.update.GuiUpdateNotifier;
import me.ichun.mods.ichunutil.client.render.RendererHelper;
import me.ichun.mods.ichunutil.client.render.entity.RenderLatchedRenderer;
import me.ichun.mods.ichunutil.client.render.item.ItemRenderingHelper;
import me.ichun.mods.ichunutil.common.core.config.ConfigBase;
import me.ichun.mods.ichunutil.common.core.config.ConfigHandler;
import me.ichun.mods.ichunutil.common.core.tracker.EntityTrackerRegistry;
import me.ichun.mods.ichunutil.common.core.util.ObfHelper;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.ichunutil.common.module.patron.PatronInfo;
import me.ichun.mods.ichunutil.common.packet.mod.PacketPatronInfo;
import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EventHandlerClient
{
    public boolean hasShownFirstGui;
    public boolean connectingToServer;

    public int ticks;
    public float renderTick;

    public int screenWidth;
    public int screenHeight;

    public ArrayList<KeyBind> keyBindList = new ArrayList<KeyBind>();
    public HashMap<KeyBinding, KeyBind> mcKeyBindList = new HashMap<KeyBinding, KeyBind>();

    public EntityTrackerRegistry entityTrackerRegistry = new EntityTrackerRegistry();

    public ArrayList<EntityLatchedRenderer> latchedRendererEntities = new ArrayList<EntityLatchedRenderer>();

    //Module stuff
    //Ding module
    public boolean dingPlayedSound;

    //EULA module
    public boolean eulaDrawEulaNotice = !iChunUtil.config.eulaAcknowledged.equals(RandomStringUtils.random(20, 32, 127, false, false, null, (new Random(Math.abs(Minecraft.getMinecraft().getSession().getPlayerID().replaceAll("-", "").hashCode() + (Math.abs("iChunUtilEULA".hashCode()))))))) && !ObfHelper.obfuscated();
    public WindowAnnoy eulaWindow = new WindowAnnoy();

    //Patron module
    public boolean patronUpdateServerAsPatron;
    public ArrayList<PatronInfo> patrons = new ArrayList<PatronInfo>();

    public HashMap<ResourceLocation, BufferedImage[]> patronRestitchedSkins = new HashMap<ResourceLocation, BufferedImage[]>();
    public HashMap<ResourceLocation, int[]> patronRestitchedSkinsId = new HashMap<ResourceLocation, int[]>();
    public ModelVoxel patronModelVoxel = new ModelVoxel();
    //End Module Stuff

    public EventHandlerClient()
    {
        Minecraft mc = Minecraft.getMinecraft();
        screenWidth = mc.displayWidth;
        screenHeight = mc.displayHeight;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRendererSafeCompatibility(RendererSafeCompatibilityEvent event)
    {
        RenderPlayer renderPlayer = Minecraft.getMinecraft().getRenderManager().skinMap.get("default");
        renderPlayer.addLayer(new LayerPatronEffect(renderPlayer));
        renderPlayer = Minecraft.getMinecraft().getRenderManager().skinMap.get("slim");
        renderPlayer.addLayer(new LayerPatronEffect(renderPlayer));
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();

        renderTick = event.renderTickTime;
        if(event.phase == TickEvent.Phase.START)
        {
            if(screenWidth != mc.displayWidth || screenHeight != mc.displayHeight)
            {
                screenWidth = mc.displayWidth;
                screenHeight = mc.displayHeight;

                for(Framebuffer buffer : RendererHelper.frameBuffers)
                {
                    buffer.createBindFramebuffer(screenWidth, screenHeight);
                }
            }

            ItemRenderingHelper.handlePreRender(mc);

            for(int i = latchedRendererEntities.size() - 1; i >= 0; i--)
            {
                EntityLatchedRenderer latchedRenderer = latchedRendererEntities.get(i);
                if(latchedRenderer.latchedEnt != null && (mc.theWorld.getWorldTime() - latchedRenderer.lastUpdate) > 10L)
                {
                    latchedRenderer.setLocationAndAngles(latchedRenderer.latchedEnt.posX, latchedRenderer.latchedEnt.posY, latchedRenderer.latchedEnt.posZ, latchedRenderer.latchedEnt.rotationYaw, latchedRenderer.latchedEnt.rotationPitch);
                    latchedRenderer.relocationTries++;
                    if(latchedRenderer.relocationTries > 5)
                    {
                        latchedRenderer.setDead();
                    }
                }
                if(latchedRenderer.latchedEnt != null && !latchedRenderer.latchedEnt.isDead && !latchedRenderer.isDead)
                {
                    latchedRenderer.updatePos();
                }
                else
                {
                    latchedRenderer.setDead();;
                    latchedRendererEntities.remove(i);
                }
            }
        }
        else
        {
            if(eulaDrawEulaNotice)
            {
                ScaledResolution reso = new ScaledResolution(mc);
                int i = Mouse.getX() * reso.getScaledWidth() / mc.displayWidth;
                int j = reso.getScaledHeight() - Mouse.getY() * reso.getScaledHeight() / mc.displayHeight - 1;

                eulaWindow.posX = reso.getScaledWidth() - eulaWindow.width;
                eulaWindow.posY = 0;
                if(eulaWindow.workspace.getFontRenderer() == null)
                {
                    eulaWindow.workspace.setWorldAndResolution(mc, mc.displayWidth, mc.displayHeight);
                    eulaWindow.workspace.initGui();
                }
                eulaWindow.draw(i - eulaWindow.posX, j - eulaWindow.posY);
                if(Mouse.isButtonDown(0))
                {
                    eulaWindow.onClick(i - eulaWindow.posX, j - eulaWindow.posY, 0);
                }
            }
            GuiUpdateNotifier.update();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(event.phase.equals(TickEvent.Phase.END))
        {
            if(mc.theWorld != null)
            {
                if(connectingToServer)
                {
                    connectingToServer = false;
                    MinecraftForge.EVENT_BUS.post(new ServerPacketableEvent());
                }
                if(patronUpdateServerAsPatron)
                {
                    patronUpdateServerAsPatron = false;
                    iChunUtil.channel.sendToServer(new PacketPatronInfo(iChunUtil.proxy.getPlayerId(), iChunUtil.config.patronRewardType, iChunUtil.config.showPatronReward == 1));
                }
                for(KeyBind bind : keyBindList)
                {
                    bind.tick();
                }
                for(Map.Entry<KeyBinding, KeyBind> e : mcKeyBindList.entrySet())
                {
                    if(e.getValue().keyIndex != e.getKey().getKeyCode())
                    {
                        e.setValue(new KeyBind(e.getKey().getKeyCode()));
                    }
                    e.getValue().tick();
                }

                if(!mc.isGamePaused())
                {
                    entityTrackerRegistry.tick();
                }
            }
            ticks++;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(event.side.isClient() && event.phase == TickEvent.Phase.END)
        {
            Minecraft mc = Minecraft.getMinecraft();

            ItemRenderingHelper.handlePlayerTick(mc, event.player);
        }
    }

    @SubscribeEvent
    public void onClientConnection(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        connectingToServer = true;

        if(iChunUtil.userIsPatron)
        {
            patronUpdateServerAsPatron = true;
        }

        for(ConfigBase conf : ConfigHandler.configs)
        {
            conf.storeSession();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        patrons.clear();
        entityTrackerRegistry.trackerEntries.clear();
        latchedRendererEntities.clear();

        for(ConfigBase conf : ConfigHandler.configs)
        {
            conf.resetSession();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        if(event.gui instanceof GuiMainMenu && !dingPlayedSound)
        {
            dingPlayedSound = true;
            if(iChunUtil.config.dingEnabled == 1 && !Loader.isModLoaded("Ding"))
            {
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(iChunUtil.config.dingSoundName), (iChunUtil.config.dingSoundPitch / 100F)));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event)
    {
        if(!hasShownFirstGui)
        {
            hasShownFirstGui = true;
            MinecraftForge.EVENT_BUS.post(new RendererSafeCompatibilityEvent());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntityJoinWorldEvent event)
    {
        if(event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer)
        {
            EntityLatchedRenderer latchedRenderer = new EntityLatchedRenderer(event.entity.worldObj, event.entity);
            event.entity.worldObj.spawnEntityInWorld(latchedRenderer);
            latchedRendererEntities.add(latchedRenderer);
        }
    }

    @SubscribeEvent
    public void onLatchedRendererUpdate(EntityLatchedRenderer.EntityLatchedRendererUpdateEvent event)
    {
        if(event.ent.latchedEnt instanceof EntityPlayer)
        {
            EntityPlayer parent = (EntityPlayer)event.ent.latchedEnt;
            PatronInfo info = getPatronInfo(parent);
            if(info != null && info.showEffect && info.effectType == 5)
            {
                double moX = parent.posX - parent.prevPosX;
                double moZ = parent.posZ - parent.prevPosZ;
                if(Math.sqrt(moX * moX + moZ * moZ) > 0.11D)
                {
                    int i = MathHelper.floor_double(parent.posX);
                    int j = MathHelper.floor_double(parent.posY - 0.20000000298023224D);
                    int k = MathHelper.floor_double(parent.posZ);
                    BlockPos blockpos = new BlockPos(i, j, k);
                    IBlockState iblockstate = event.ent.worldObj.getBlockState(blockpos);
                    Block block = iblockstate.getBlock();
                    if(block.getRenderType() != -1)
                    {
                        if(parent.isSprinting())
                        {
                            for(int kk = 0; kk < 2; kk++)
                            {
                                event.ent.worldObj.spawnParticle(EnumParticleTypes.BLOCK_DUST, parent.posX + ((double)parent.getRNG().nextFloat() - 0.5D) * (double)parent.width, parent.getEntityBoundingBox().minY + 0.1D, parent.posZ + ((double)parent.getRNG().nextFloat() - 0.5D) * (double)parent.width, -moX * 0.8D, 0.2D + 0.3D * parent.getRNG().nextDouble(), -moZ * 0.8D, Block.getStateId(iblockstate));
                            }
                        }
                        for(int kk = 0; kk < 3; kk++)
                        {
                            double d0 = event.ent.worldObj.rand.nextGaussian() * 0.1D;
                            double d2 = event.ent.worldObj.rand.nextGaussian() * 0.1D;
                            event.ent.worldObj.spawnParticle(EnumParticleTypes.FLAME, parent.posX + d0, parent.posY + 0.1F, parent.posZ + d2, 0D, parent.isSprinting() ? parent.getRNG().nextFloat() * 0.05D : 0.0125D, 0D);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLatchedRendererRender(RenderLatchedRenderer.RenderLatchedRendererEvent event)
    {
        if(event.ent.latchedEnt instanceof AbstractClientPlayer)
        {
            AbstractClientPlayer parent = (AbstractClientPlayer)event.ent.latchedEnt;

            if(!(parent.getName().equals(Minecraft.getMinecraft().getRenderViewEntity().getName()) && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0))
            {
                GlStateManager.pushMatrix();

                EntityTrackerRegistry.Entry entry = entityTrackerRegistry.getOrCreateEntry(parent, 100).addAdditionalTrackerInfo(PatronTracker.class);

                ArrayList<EntityTrackerRegistry.EntityInfo> loc = entry.trackedInfo;

                ResourceLocation rl = parent.getLocationSkin();

                if(loc.size() > 21)
                {
                    for(EntityTrackerRegistry.IAdditionalTrackerInfo tracker : loc.get(20).additionalInfo)
                    {
                        if(tracker instanceof PatronTracker && ((PatronTracker)tracker).txLocation != null)
                        {
                            rl = ((PatronTracker)tracker).txLocation;
                        }
                    }
                }

                PatronInfo info = getPatronInfo(parent);
                if(info != null && info.showEffect)
                {
                    switch(info.effectType)
                    {
                        case 1:
                        {
                            BufferedImage[] skins = patronRestitchedSkins.get(rl);

                            if(skins == null)
                            {
                                ITextureObject obj = Minecraft.getMinecraft().getTextureManager().getTexture(rl);
                                if(obj instanceof ThreadDownloadImageData)
                                {
                                    try
                                    {
                                        BufferedImage img = ((ThreadDownloadImageData)obj).bufferedImage;
                                        if(img != null)
                                        {
                                            int[] imgId = new int[4];
                                            skins = new BufferedImage[4];

                                            int[] dimsX = new int[] { 4, 4, 8, 8 };
                                            int[] dimsZ = new int[] { 4, 4, 4, 8 };
                                            int[] dimsY = new int[] { 12, 12, 12, 8 };

                                            int[] startX = new int[] { 0, 40, 16, 0 };
                                            int[] startY = new int[] { 16, 16, 16, 0 };

                                            for(int j = 0; j < dimsX.length; j++)
                                            {
                                                int[] dim = new int[] { dimsX[j], dimsY[j], dimsZ[j] };
                                                int[] rots = new int[] { -90, 180, 0, 0, 90, 0, -90, 180, 90 };
                                                BufferedImage tmp = new BufferedImage(48, 24, 1);

                                                Graphics2D gfx = tmp.createGraphics();

                                                int[] xSource = new int[] { dim[2], dim[2], dim[2] + dim[0] + dim[2], 0, dim[2] + dim[0], dim[2] + dim[0], dim[2] + dim[0], dim[2] + dim[0], 				dim[2]};
                                                int[] ySource = new int[] { 0, 0, dim[2], dim[2], 0, 0, 0, 0,  					0 };

                                                int[] xCoord = new int[] { dim[0], dim[0] + dim[2] + dim[0] + dim[2], 0, dim[0] + dim[2] + dim[0] + dim[2] + dim[0], dim[0], dim[0] + dim[2], dim[0] + dim[2] + dim[0], dim[0] + dim[2] + dim[0] + dim[2], 				dim[2] + dim[0] + dim[2] };
                                                int[] yCoord = new int[] { 0, 0, dim[2], dim[2], dim[2] + dim[1], dim[2] + dim[1], dim[2] + dim[1], dim[2] + dim[1], 				0 };

                                                int[] dimX = new int[] { dim[0], dim[0], dim[0], dim[2], dim[0], dim[0], dim[0], dim[0], 					dim[0] };
                                                int[] dimY = new int[] { dim[2], dim[2], dim[1], dim[1], dim[2], dim[2], dim[2], dim[2], 					dim[2] };

                                                for(int i = 0; i < rots.length; i++)
                                                {
                                                    if(i == rots.length - 1)
                                                    {
                                                        gfx.drawImage(img, dim[0], 0, dim[0] + dim[2] + dim[0] + dim[2] + dim[0], dim[2] + dim[1], startX[j], startY[j], startX[j] + (2 * dim[0] + 2 * dim[2]), startY[j] + dim[1] + dim[2], null);
                                                    }

                                                    BufferedImage temp = img.getSubimage(startX[j] + xSource[i], startY[j] + ySource[i], dimX[i], dimY[i]); //new BufferedImage(img.getWidth(), img.getHeight(), 1);

                                                    BufferedImage temp1 = new BufferedImage(dimX[i], dimY[i], 1);

                                                    Graphics2D gfx1 = temp1.createGraphics();
                                                    gfx1.rotate(Math.toRadians(rots[i]), dimX[i] / 2, dimY[i] / 2);
                                                    gfx1.drawImage(temp, 0, 0, dimX[i], dimY[i], 0, 0, dimX[i], dimY[i], null);
                                                    gfx1.dispose();

                                                    gfx.drawImage(temp1, xCoord[i], yCoord[i], xCoord[i] + dimX[i], yCoord[i] + dimY[i], 0, 0, dimX[i], dimY[i], null);
                                                }

                                                imgId[j] = TextureUtil.uploadTextureImage(TextureUtil.glGenTextures(), tmp);
                                                skins[j] = tmp;
                                            }

                                            patronRestitchedSkinsId.put(rl, imgId);
                                            patronRestitchedSkins.put(rl, skins);
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            patronModelVoxel.renderPlayer(event.ent, 0, parent.hashCode(), loc, event.x, event.y, event.z, 0.0625F, event.f1, patronRestitchedSkinsId.get(rl));

                            break;
                        }
                        case 3:
                        {
                            if(loc.size() > 6)
                            {
                                Render render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(parent);
                                RenderPlayer renderPlayer = (RenderPlayer)render;
                                ModelBase biped = renderPlayer.mainModel;

                                int ii = parent.getBrightnessForRender(event.f1);
                                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)(ii % 65536) / 1.0F, (float)(ii / 65536) / 1.0F);

                                GlStateManager.enableBlend();
                                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                                for(int i = 1; i < 6; i++)
                                {
                                    EntityTrackerRegistry.EntityInfo entInfo = loc.get(i);

                                    for(EntityTrackerRegistry.IAdditionalTrackerInfo tracker : entInfo.additionalInfo)
                                    {
                                        if(tracker instanceof PatronTracker && ((PatronTracker)tracker).txLocation != null)
                                        {
                                            rl = ((PatronTracker)tracker).txLocation;
                                        }
                                    }

                                    GlStateManager.pushMatrix();

                                    double tX = event.ent.prevPosX + (event.ent.posX - event.ent.prevPosX) * event.f1;
                                    double tY = event.ent.prevPosY + (event.ent.posY - event.ent.prevPosY) * event.f1;
                                    double tZ = event.ent.prevPosZ + (event.ent.posZ - event.ent.prevPosZ) * event.f1;
                                    GlStateManager.translate(entInfo.posX - tX + event.x, entInfo.posY - tY + event.y, entInfo.posZ - tZ + event.z);

                                    GlStateManager.rotate(entInfo.renderYawOffset, 0.0F, -1.0F, 0.0F);

                                    float scalee = 0.9375F;
                                    GlStateManager.scale(scalee, -scalee, -scalee);

                                    GlStateManager.translate(0.0F, -1.5F, 0.0F);

                                    float alpha = 1.0F - MathHelper.clamp_float(((i - 1) + event.f1) / 5F, 0.0F, 1.0F);//1.0F - MathHelper.clamp_float(((float)(loc.size() - 2 - i) + f1) / (float)((loc.size() - 2) > 5 ? 5 : loc.size() - 2), 0.0F, 1.0F);

                                    GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

                                    Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
                                    float f2 = entInfo.renderYawOffset;
                                    float f3 = entInfo.rotationYawHead;

                                    float f7 = entInfo.limbSwingAmount;

                                    float f8 = entInfo.limbSwing - entInfo.limbSwingAmount;

                                    if (f7 > 1.0F)
                                    {
                                        f7 = 1.0F;
                                    }

                                    float f4 = (float)parent.ticksExisted - i + event.f1;

                                    float f5 = entInfo.rotationPitch;

                                    biped.render(parent, f8, f7, f4, f3 - f2, f5, 0.0625F);

                                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                                    GlStateManager.popMatrix();
                                }

                                GlStateManager.disableBlend();
                            }
                            break;
                        }
                    }
                }
                GlStateManager.popMatrix();
            }
        }
    }

    public PatronInfo getPatronInfo(EntityPlayer player)
    {
        EntityPlayer oriPlayer = player;
        if(iChunUtil.hasMorphMod())
        {
            EntityLivingBase ent = MorphApi.getApiImpl().getMorphEntity(player.worldObj, player.getName(), Side.CLIENT);
            if(ent != null) //is morphed
            {
                if(!(ent instanceof EntityPlayer) || MorphApi.getApiImpl().morphProgress(player.getName(), Side.CLIENT) < 1.0F)
                {
                    return null;
                }
                player = (EntityPlayer)ent;
            }
        }
        PatronInfo info = null;

        for(PatronInfo info1 : patrons)
        {
            if(info1.id.equals(player.getGameProfile().getId().toString().replaceAll("-", "")))
            {
                info = info1;
                break;
            }
        }
        return info;
    }
}