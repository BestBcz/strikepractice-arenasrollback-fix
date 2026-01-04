package net.minecraft.server.v1_7_R4;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.spigotmc.AsyncCatcher;

/* loaded from: Mirez.jar:net/minecraft/server/v1_7_R4/EntityTrackerEntry.class */
public class EntityTrackerEntry {
    private static final Logger p = LogManager.getLogger();
    public Entity tracker;
    public int b;
    public int c;
    public int xLoc;
    public int yLoc;
    public int zLoc;
    public int yRot;
    public int xRot;
    public int i;
    public double j;
    public double k;
    public double l;
    public int m;
    private double q;
    private double r;
    private double s;
    private boolean isMoving;
    private boolean u;
    private int v;
    private Entity w;
    private boolean x;
    public boolean n;

    // [修复1] 添加泛型 <EntityPlayer>，解决大部分类型转换报错
    public Set<EntityPlayer> trackedPlayers = new HashSet<EntityPlayer>();

    public EntityTrackerEntry(Entity entity, int i, int j, boolean flag) {
        this.tracker = entity;
        this.b = i;
        this.c = j;
        this.u = flag;
        this.xLoc = MathHelper.floor(entity.locX * 32.0d);
        this.yLoc = MathHelper.floor(entity.locY * 32.0d);
        this.zLoc = MathHelper.floor(entity.locZ * 32.0d);
        this.yRot = MathHelper.d((entity.yaw * 256.0f) / 360.0f);
        this.xRot = MathHelper.d((entity.pitch * 256.0f) / 360.0f);
        this.i = MathHelper.d((entity.getHeadRotation() * 256.0f) / 360.0f);
    }

    public boolean equals(Object object) {
        return (object instanceof EntityTrackerEntry) && ((EntityTrackerEntry) object).tracker.getId() == this.tracker.getId();
    }

    public int hashCode() {
        return this.tracker.getId();
    }

    public void track(List list) {
        this.n = false;

        // ==========================================
        // [修复核心] 1.7 -> 1.8 鱼钩强制销毁逻辑
        // 如果实体已死亡且是鱼钩，强制发送销毁包给所有追踪的玩家
        // ==========================================
        if (this.tracker.dead && this.tracker instanceof EntityFishingHook) {
            Iterator<EntityPlayer> it = this.trackedPlayers.iterator();
            while (it.hasNext()) {
                EntityPlayer player = it.next();
                if (player != null) {
                    player.d(this.tracker); // d() 方法通常是发送 PacketPlayOutEntityDestroy
                }
                it.remove();
            }
            return;
        }
        // ==========================================

        if (!this.isMoving || this.tracker.e(this.q, this.r, this.s) > 16.0d) {
            this.q = this.tracker.locX;
            this.r = this.tracker.locY;
            this.s = this.tracker.locZ;
            this.isMoving = true;
            this.n = true;
            scanPlayers(list);
        }
        if (this.w != this.tracker.vehicle || (this.tracker.vehicle != null && this.m % 60 == 0)) {
            this.w = this.tracker.vehicle;
            broadcast(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
        }
        if (this.tracker instanceof EntityItemFrame) {
            EntityItemFrame i3 = (EntityItemFrame) this.tracker;
            ItemStack i4 = i3.getItem();
            if (this.m % 10 == 0 && i4 != null && (i4.getItem() instanceof ItemWorldMap)) {
                WorldMap i6 = Items.MAP.getSavedMap(i4, this.tracker.world);
                // [修复2] 因为 trackedPlayers 加了泛型，这里的增强 for 循环不会报错了
                for (EntityHuman i8 : this.trackedPlayers) {
                    EntityPlayer i9 = (EntityPlayer) i8;
                    i6.a(i9, i4);
                    Packet j0 = Items.MAP.c(i4, this.tracker.world, i9);
                    if (j0 != null) {
                        i9.playerConnection.sendPacket(j0);
                    }
                }
            }
            b();
        } else if (this.m % this.c == 0 || this.tracker.al || this.tracker.getDataWatcher().a()) {
            if (this.tracker.vehicle == null) {
                this.v++;
                int i = this.tracker.as.a(this.tracker.locX);
                int j = MathHelper.floor(this.tracker.locY * 32.0d);
                int k = this.tracker.as.a(this.tracker.locZ);
                int l = MathHelper.d((this.tracker.yaw * 256.0f) / 360.0f);
                int i1 = MathHelper.d((this.tracker.pitch * 256.0f) / 360.0f);
                int j1 = i - this.xLoc;
                int k1 = j - this.yLoc;
                int l1 = k - this.zLoc;
                Object object = null;
                boolean flag = Math.abs(j1) >= 4 || Math.abs(k1) >= 4 || Math.abs(l1) >= 4 || this.m % 60 == 0;
                boolean flag1 = Math.abs(l - this.yRot) >= 4 || Math.abs(i1 - this.xRot) >= 4;
                if (flag) {
                    this.xLoc = i;
                    this.yLoc = j;
                    this.zLoc = k;
                }
                if (flag1) {
                    this.yRot = l;
                    this.xRot = i1;
                }
                if (this.m > 0 || (this.tracker instanceof EntityArrow)) {
                    if (j1 >= -128 && j1 < 128 && k1 >= -128 && k1 < 128 && l1 >= -128 && l1 < 128 && this.v <= 400 && !this.x) {
                        if (flag && flag1) {
                            object = new PacketPlayOutRelEntityMoveLook(this.tracker.getId(), (byte) j1, (byte) k1, (byte) l1, (byte) l, (byte) i1, this.tracker.onGround);
                        } else if (flag) {
                            object = new PacketPlayOutRelEntityMove(this.tracker.getId(), (byte) j1, (byte) k1, (byte) l1, this.tracker.onGround);
                        } else if (flag1) {
                            object = new PacketPlayOutEntityLook(this.tracker.getId(), (byte) l, (byte) i1, this.tracker.onGround);
                        }
                    } else {
                        this.v = 0;
                        if (this.tracker instanceof EntityPlayer) {
                            scanPlayers(new ArrayList(this.trackedPlayers));
                        }
                        object = new PacketPlayOutEntityTeleport(this.tracker.getId(), i, j, k, (byte) l, (byte) i1, this.tracker.onGround, (this.tracker instanceof EntityFallingBlock) || (this.tracker instanceof EntityTNTPrimed));
                    }
                }
                if (this.u) {
                    double d0 = this.tracker.motX - this.j;
                    double d1 = this.tracker.motY - this.k;
                    double d2 = this.tracker.motZ - this.l;
                    double d4 = (d0 * d0) + (d1 * d1) + (d2 * d2);
                    if (d4 > 0.02d * 0.02d || (d4 > 0.0d && this.tracker.motX == 0.0d && this.tracker.motY == 0.0d && this.tracker.motZ == 0.0d)) {
                        this.j = this.tracker.motX;
                        this.k = this.tracker.motY;
                        this.l = this.tracker.motZ;
                        broadcast(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.j, this.k, this.l));
                    }
                }
                if (object != null) {
                    broadcast((Packet) object);
                }
                b();
                this.x = false;
            } else {
                int i2 = MathHelper.d((this.tracker.yaw * 256.0f) / 360.0f);
                int j2 = MathHelper.d((this.tracker.pitch * 256.0f) / 360.0f);
                boolean flag2 = Math.abs(i2 - this.yRot) >= 4 || Math.abs(j2 - this.xRot) >= 4;
                if (flag2) {
                    broadcast(new PacketPlayOutEntityLook(this.tracker.getId(), (byte) i2, (byte) j2, this.tracker.onGround));
                    this.yRot = i2;
                    this.xRot = j2;
                }
                this.xLoc = this.tracker.as.a(this.tracker.locX);
                this.yLoc = MathHelper.floor(this.tracker.locY * 32.0d);
                this.zLoc = this.tracker.as.a(this.tracker.locZ);
                b();
                this.x = true;
            }
            int i5 = MathHelper.d((this.tracker.getHeadRotation() * 256.0f) / 360.0f);
            if (Math.abs(i5 - this.i) >= 4) {
                broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) i5));
                this.i = i5;
            }
            this.tracker.al = false;
        }
        this.m++;
        if (this.tracker.velocityChanged) {
            boolean cancelled = false;
            if (this.tracker instanceof EntityPlayer) {
                Player player = (Player) this.tracker.getBukkitEntity();
                Vector velocity = player.getVelocity();
                PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity);
                this.tracker.world.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    cancelled = true;
                } else if (!velocity.equals(event.getVelocity())) {
                    player.setVelocity(velocity);
                }
            }
            if (!cancelled) {
                broadcastIncludingSelf(new PacketPlayOutEntityVelocity(this.tracker));
            }
            this.tracker.velocityChanged = false;
        }
    }

    private void b() {
        DataWatcher datawatcher = this.tracker.getDataWatcher();
        if (datawatcher.a()) {
            broadcastIncludingSelf(new PacketPlayOutEntityMetadata(this.tracker.getId(), datawatcher, false));
        }
        if (this.tracker instanceof EntityLiving) {
            AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
            Set set = attributemapserver.getAttributes();
            if (!set.isEmpty()) {
                if (this.tracker instanceof EntityPlayer) {
                    ((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(set, false);
                }
                broadcastIncludingSelf(new PacketPlayOutUpdateAttributes(this.tracker.getId(), set));
            }
            set.clear();
        }
    }

    public void broadcast(Packet packet) {
        for (EntityPlayer entityplayer : this.trackedPlayers) {
            entityplayer.playerConnection.sendPacket(packet);
        }
    }

    public void broadcastIncludingSelf(Packet packet) {
        broadcast(packet);
        if (this.tracker instanceof EntityPlayer) {
            ((EntityPlayer) this.tracker).playerConnection.sendPacket(packet);
        }
    }

    public void a() {
        for (EntityPlayer entityplayer : this.trackedPlayers) {
            entityplayer.d(this.tracker);
        }
    }

    public void a(EntityPlayer entityplayer) {
        if (this.trackedPlayers.contains(entityplayer)) {
            entityplayer.d(this.tracker);
            this.trackedPlayers.remove(entityplayer);
        }
    }

    public void updatePlayer(EntityPlayer entityplayer) {
        AsyncCatcher.catchOp("player tracker update");
        if (entityplayer != this.tracker) {
            double d0 = entityplayer.locX - (this.xLoc / 32);
            double d1 = entityplayer.locZ - (this.zLoc / 32);
            if (d0 >= (-this.b) && d0 <= this.b && d1 >= (-this.b) && d1 <= this.b) {
                if (this.trackedPlayers.contains(entityplayer)) {
                    return;
                }
                if (d(entityplayer) || this.tracker.attachedToPlayer) {
                    if (this.tracker instanceof EntityPlayer) {
                        Player player = ((EntityPlayer) this.tracker).getBukkitEntity();
                        if (!entityplayer.getBukkitEntity().canSee(player)) {
                            return;
                        }
                    }
                    entityplayer.removeQueue.remove(Integer.valueOf(this.tracker.getId()));
                    this.trackedPlayers.add(entityplayer);
                    Packet packet = c();
                    if (this.tracker instanceof EntityPlayer) {
                        entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.addPlayer((EntityPlayer) this.tracker));
                        if (!entityplayer.getName().equals(entityplayer.listName) && entityplayer.playerConnection.networkManager.getVersion() > 28) {
                            entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.updateDisplayName((EntityPlayer) this.tracker));
                        }
                    }
                    entityplayer.playerConnection.sendPacket(packet);
                    if (!this.tracker.getDataWatcher().d()) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(this.tracker.getId(), this.tracker.getDataWatcher(), true));
                    }
                    if (this.tracker instanceof EntityLiving) {
                        AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
                        Collection collection = attributemapserver.c();
                        if (this.tracker.getId() == entityplayer.getId()) {
                            ((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(collection, false);
                        }
                        if (!collection.isEmpty()) {
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(this.tracker.getId(), collection));
                        }
                    }
                    this.j = this.tracker.motX;
                    this.k = this.tracker.motY;
                    this.l = this.tracker.motZ;
                    if (this.u && !(packet instanceof PacketPlayOutSpawnEntityLiving)) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.tracker.motX, this.tracker.motY, this.tracker.motZ));
                    }
                    if (this.tracker.vehicle != null) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
                    }
                    if (this.tracker.passenger != null) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, this.tracker.passenger, this.tracker));
                    }
                    if ((this.tracker instanceof EntityInsentient) && ((EntityInsentient) this.tracker).getLeashHolder() != null) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, this.tracker, ((EntityInsentient) this.tracker).getLeashHolder()));
                    }
                    if (this.tracker instanceof EntityLiving) {
                        for (int i = 0; i < 5; i++) {
                            ItemStack itemstack = ((EntityLiving) this.tracker).getEquipment(i);
                            if (itemstack != null) {
                                entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack));
                            }
                        }
                    }
                    if (this.tracker instanceof EntityHuman) {
                        EntityHuman entityhuman = (EntityHuman) this.tracker;
                        if (entityhuman.isSleeping()) {
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutBed(entityhuman, MathHelper.floor(this.tracker.locX), MathHelper.floor(this.tracker.locY), MathHelper.floor(this.tracker.locZ)));
                        }
                    }
                    this.i = MathHelper.d((this.tracker.getHeadRotation() * 256.0f) / 360.0f);
                    broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) this.i));
                    if (this.tracker instanceof EntityLiving) {
                        EntityLiving entityliving = (EntityLiving) this.tracker;
                        // [修复3] 这里加了强转，因为 getEffects() 返回的是 raw collection
                        for (Object obj : entityliving.getEffects()) {
                            MobEffect mobeffect = (MobEffect) obj;
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(this.tracker.getId(), mobeffect));
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            if (this.trackedPlayers.contains(entityplayer)) {
                this.trackedPlayers.remove(entityplayer);
                entityplayer.d(this.tracker);
            }
        }
    }

    private boolean d(EntityPlayer entityplayer) {
        return entityplayer.r().getPlayerChunkMap().a(entityplayer, this.tracker.ah, this.tracker.aj);
    }

    public void scanPlayers(List list) {
        for (int i = 0; i < list.size(); i++) {
            updatePlayer((EntityPlayer) list.get(i));
        }
    }

    private Packet c() {
        PacketPlayOutSpawnEntity packetplayoutspawnentity;
        if (this.tracker.dead) {
            return null;
        }
        if (this.tracker instanceof EntityItem) {
            return new PacketPlayOutSpawnEntity(this.tracker, 2, 1);
        }
        if (this.tracker instanceof EntityPlayer) {
            return new PacketPlayOutNamedEntitySpawn((EntityHuman) this.tracker);
        }
        if (this.tracker instanceof EntityMinecartAbstract) {
            EntityMinecartAbstract entityminecartabstract = (EntityMinecartAbstract) this.tracker;
            return new PacketPlayOutSpawnEntity(this.tracker, 10, entityminecartabstract.m());
        }
        if (this.tracker instanceof EntityBoat) {
            return new PacketPlayOutSpawnEntity(this.tracker, 1);
        }
        if (!(this.tracker instanceof IAnimal) && !(this.tracker instanceof EntityEnderDragon)) {
            if (this.tracker instanceof EntityFishingHook) {
                EntityHuman entityhuman = ((EntityFishingHook) this.tracker).owner;
                return new PacketPlayOutSpawnEntity(this.tracker, 90, entityhuman != null ? entityhuman.getId() : this.tracker.getId());
            }
            if (this.tracker instanceof EntityArrow) {
                Entity entity = ((EntityArrow) this.tracker).shooter;
                return new PacketPlayOutSpawnEntity(this.tracker, 60, entity != null ? entity.getId() : this.tracker.getId());
            }
            if (this.tracker instanceof EntitySnowball) {
                return new PacketPlayOutSpawnEntity(this.tracker, 61);
            }
            if (this.tracker instanceof EntityPotion) {
                return new PacketPlayOutSpawnEntity(this.tracker, 73, ((EntityPotion) this.tracker).getPotionValue());
            }
            if (this.tracker instanceof EntityThrownExpBottle) {
                return new PacketPlayOutSpawnEntity(this.tracker, 75);
            }
            if (this.tracker instanceof EntityEnderPearl) {
                return new PacketPlayOutSpawnEntity(this.tracker, 65);
            }
            if (this.tracker instanceof EntityEnderSignal) {
                return new PacketPlayOutSpawnEntity(this.tracker, 72);
            }
            if (this.tracker instanceof EntityFireworks) {
                return new PacketPlayOutSpawnEntity(this.tracker, 76);
            }
            if (this.tracker instanceof EntityFireball) {
                EntityFireball entityfireball = (EntityFireball) this.tracker;
                byte b0 = 63;
                if (this.tracker instanceof EntitySmallFireball) {
                    b0 = 64;
                } else if (this.tracker instanceof EntityWitherSkull) {
                    b0 = 66;
                }
                if (entityfireball.shooter != null) {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, ((EntityFireball) this.tracker).shooter.getId());
                } else {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, 0);
                }
                packetplayoutspawnentity.d((int) (entityfireball.dirX * 8000.0d));
                packetplayoutspawnentity.e((int) (entityfireball.dirY * 8000.0d));
                packetplayoutspawnentity.f((int) (entityfireball.dirZ * 8000.0d));
                return packetplayoutspawnentity;
            }
            if (this.tracker instanceof EntityEgg) {
                return new PacketPlayOutSpawnEntity(this.tracker, 62);
            }
            if (this.tracker instanceof EntityTNTPrimed) {
                return new PacketPlayOutSpawnEntity(this.tracker, 50);
            }
            if (this.tracker instanceof EntityEnderCrystal) {
                return new PacketPlayOutSpawnEntity(this.tracker, 51);
            }
            if (this.tracker instanceof EntityFallingBlock) {
                EntityFallingBlock entityfallingblock = (EntityFallingBlock) this.tracker;
                return new PacketPlayOutSpawnEntity(this.tracker, 70, Block.getId(entityfallingblock.f()) | (entityfallingblock.data << 16));
            }
            if (this.tracker instanceof EntityPainting) {
                return new PacketPlayOutSpawnEntityPainting((EntityPainting) this.tracker);
            }
            if (this.tracker instanceof EntityItemFrame) {
                EntityItemFrame entityitemframe = (EntityItemFrame) this.tracker;
                PacketPlayOutSpawnEntity packetplayoutspawnentity2 = new PacketPlayOutSpawnEntity(this.tracker, 71, entityitemframe.direction);
                packetplayoutspawnentity2.a(MathHelper.d(entityitemframe.x * 32));
                packetplayoutspawnentity2.b(MathHelper.d(entityitemframe.y * 32));
                packetplayoutspawnentity2.c(MathHelper.d(entityitemframe.z * 32));
                return packetplayoutspawnentity2;
            }
            if (this.tracker instanceof EntityLeash) {
                EntityLeash entityleash = (EntityLeash) this.tracker;
                PacketPlayOutSpawnEntity packetplayoutspawnentity3 = new PacketPlayOutSpawnEntity(this.tracker, 77);
                packetplayoutspawnentity3.a(MathHelper.d(entityleash.x * 32));
                packetplayoutspawnentity3.b(MathHelper.d(entityleash.y * 32));
                packetplayoutspawnentity3.c(MathHelper.d(entityleash.z * 32));
                return packetplayoutspawnentity3;
            }
            if (this.tracker instanceof EntityExperienceOrb) {
                return new PacketPlayOutSpawnEntityExperienceOrb((EntityExperienceOrb) this.tracker);
            }
            throw new IllegalArgumentException("Don't know how to add " + this.tracker.getClass() + "!");
        }
        this.i = MathHelper.d((this.tracker.getHeadRotation() * 256.0f) / 360.0f);
        return new PacketPlayOutSpawnEntityLiving((EntityLiving) this.tracker);
    }

    public void clear(EntityPlayer entityplayer) {
        AsyncCatcher.catchOp("player tracker clear");
        if (this.trackedPlayers.contains(entityplayer)) {
            this.trackedPlayers.remove(entityplayer);
            entityplayer.d(this.tracker);
        }
    }
}