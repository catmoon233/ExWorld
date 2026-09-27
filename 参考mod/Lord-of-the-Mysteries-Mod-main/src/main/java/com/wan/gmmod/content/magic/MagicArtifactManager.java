package com.wan.gmmod.content.magic;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.MagicArtifactItem;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.characteristics.MagicArtifactData;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 神奇物品核心逻辑（服务端权威）。
 * <p>
 * 门途径序列 9 ~ 5 各自的能力：
 * <ul>
 *   <li><b>学徒（9）</b>：短距传送（5 格内，需视线），10 秒冷却；负面 10% 方向感混乱（左右颠倒 10 秒）。</li>
 *   <li><b>戏法大师（8）</b>：空间折叠存储（最多 3 格）；负面 5% 物品永久丢失（卷入灵界），取出延迟 1~3 秒。</li>
 *   <li><b>占星人（7）</b>：星象解读，预知未来 1 小时天气与吉凶；负面 20% 结果完全相反，解读期间无法感知周围。</li>
 *   <li><b>记录官（6）</b>：记录一个见过的超凡能力（不高于自身序列），24 小时内使用一次；负面 15% 失控。</li>
 *   <li><b>旅行家（5）</b>：开启传送门，传送至 1000 米内标记坐标；负面 8% 开错位置（±100 米），消耗 30% 最大灵性。</li>
 * </ul>
 * 变体（生成时掷定）会修改各能力的具体参数，见对应方法。持续负面（24h 自动弹出 /
 * 记录反噬）由 {@link com.wan.gmmod.common.event.MagicArtifactEventSubscriber} 驱动。
 */
public final class MagicArtifactManager {

    private MagicArtifactManager() {
    }

    // =====================================================================
    // 通用工具
    // =====================================================================

    /** 右键使用神奇物品的主入口（服务端）。返回是否成功触发（SUCCESS）。 */
    public static boolean onUse(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        MagicArtifactData data = MagicArtifactItem.getData(stack);
        if (data == null) {
            return false;
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        if (pathway == null) {
            return false;
        }
        return switch (data.level()) {
            case 9 -> apprenticeUse(sp, data);
            case 8 -> trickMasterUse(sp, data);
            case 7 -> astrologerUse(sp, data);
            case 6 -> recorderUse(sp, data);
            case 5 -> travelerUse(sp, data);
            default -> false;
        };
    }

    /** 检查并扣除灵性；不足返回 false。 */
    private static boolean spendSpirit(ServerPlayer sp, int amount) {
        int cur = SpiritualityManager.get(sp);
        if (cur < amount) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.skill.no_spirituality"), true);
            return false;
        }
        SpiritualityManager.set(sp, cur - amount);
        return true;
    }

    /** 玩家技能栏冷却键：{@code gmmod_magic_cd_<path>_<level>}。 */
    private static String cdKey(MagicArtifactData data) {
        return "gmmod_magic_cd_" + data.pathway() + "_" + data.level();
    }

    private static boolean onCooldown(ServerPlayer sp, MagicArtifactData data) {
        CompoundTag tag = sp.getPersistentData();
        long until = tag.getLong(cdKey(data));
        if (until > sp.serverLevel().getGameTime()) {
            long remain = (until - sp.serverLevel().getGameTime() + 19) / 20;
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.cooldown", remain), true);
            return true;
        }
        return false;
    }

    private static void setCooldown(ServerPlayer sp, MagicArtifactData data, int seconds) {
        sp.getPersistentData().putLong(cdKey(data),
                sp.serverLevel().getGameTime() + seconds * 20L);
    }

    private static long gameTime(ServerPlayer sp) {
        return sp.serverLevel().getGameTime();
    }

    /** 24 小时的游戏刻数。 */
    private static long hoursToTicks(int hours) {
        return hours * 60L * 20L;
    }

    // =====================================================================
    // 语言键（供 tooltip）
    // =====================================================================

    public static String positiveKey(Sequences.Pathway pathway, int level) {
        return "magic.guimi_mod." + pathway.getKey() + "." + level + ".positive";
    }

    public static String negativeKey(Sequences.Pathway pathway, int level) {
        return "magic.guimi_mod." + pathway.getKey() + "." + level + ".negative";
    }

    public static String variantKey(Sequences.Pathway pathway, int level, int variant) {
        return "magic.guimi_mod." + pathway.getKey() + "." + level + ".variant" + variant;
    }

    // =====================================================================
    // 序列9 学徒：短距传送
    // =====================================================================

    private static final int APPRENTICE_RANGE = 5;
    private static final int APPRENTICE_CD = 10;
    private static final int APPRENTICE_CONFUSE_CHANCE = 10;
    private static final int APPRENTICE_CONFUSE_SECONDS = 10;

    private static boolean apprenticeUse(ServerPlayer sp, MagicArtifactData data) {
        if (onCooldown(sp, data)) {
            return true;
        }
        // 变体1：距离翻倍，冷却翻倍
        int range = data.variant() == 1 ? APPRENTICE_RANGE * 2 : APPRENTICE_RANGE;
        int cd = data.variant() == 1 ? APPRENTICE_CD * 2 : APPRENTICE_CD;
        // 变体3：传送后可隐身 2 秒，消耗加倍
        int cost = data.variant() == 3 ? 10 : 5;
        if (!spendSpirit(sp, cost)) {
            return true;
        }

        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0F);
        Vec3 target = eye.add(look.scale(range));
        BlockHitResult hit = sp.level().clip(new ClipContext(
                eye, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp));
        Vec3 dest = hit.getType() == HitResult.Type.MISS ? target : hit.getLocation();

        BlockPos start = BlockPos.containing(dest.x, dest.y - 0.1, dest.z);
        BlockPos destPos = null;
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos check = start.above(dy);
            if (sp.level().getBlockState(check).isAir()
                    && sp.level().getBlockState(check.above()).isAir()) {
                destPos = check;
                break;
            }
        }
        if (destPos == null) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.no_space"), true);
            return true;
        }
        sp.teleportTo(sp.serverLevel(),
                destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5,
                sp.getYRot(), sp.getXRot());
        sp.serverLevel().sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

        // 变体3：传送后隐身 2 秒
        if (data.variant() == 3) {
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, true));
        }
        // 负面：10% 方向感混乱（左右颠倒 → 眩晕 + 移动反向体验）；变体2 为上下颠倒（失重）
        if (sp.serverLevel().random.nextInt(100) < APPRENTICE_CONFUSE_CHANCE) {
            int ticks = APPRENTICE_CONFUSE_SECONDS * 20;
            if (data.variant() == 2) {
                sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, true));
            } else {
                sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, true));
            }
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.apprentice_confuse"), true);
        }
        setCooldown(sp, data, cd);
        return true;
    }

    // =====================================================================
    // 序列8 戏法大师：空间折叠存储
    // =====================================================================

    /** 折叠空间存储（ListTag of 条目）。条目：物品 {k:"i", data:ItemStack}；活物 {k:"l", type, data}。 */
    private static final String FOLD_TAG = "gmmod_magic_fold";
    /** 取出延迟截止（游戏刻）。 */
    private static final String FOLD_DELAY_UNTIL = "gmmod_magic_fold_delay_until";
    /** 最近一次存储的时间（用于变体2 的 24h 自动弹出）。 */
    private static final String FOLD_STORE_TIME = "gmmod_magic_fold_store_time";

    private static final int FOLD_CAPACITY = 3;
    private static final int FOLD_LOSS_CHANCE = 5;

    /** 可被收进折叠空间的小动物类型（变体3）。 */
    private static final Set<EntityType<?>> FOLDABLE_PETS = Set.of(
            EntityType.CHICKEN, EntityType.COW, EntityType.SHEEP, EntityType.PIG,
            EntityType.RABBIT, EntityType.CAT, EntityType.WOLF, EntityType.PARROT,
            EntityType.TURTLE, EntityType.FROG, EntityType.AXOLOTL);

    private static boolean trickMasterUse(ServerPlayer sp, MagicArtifactData data) {
        // 变体1：容量翻倍，丢失概率翻倍
        int capacity = data.variant() == 1 ? FOLD_CAPACITY * 2 : FOLD_CAPACITY;
        int lossChance = data.variant() == 1 ? FOLD_LOSS_CHANCE * 2 : FOLD_LOSS_CHANCE;
        // 变体2：取出无延迟，但存储 24 小时后自动弹出
        boolean noDelay = data.variant() == 2;
        // 变体3：可存储小动物，消耗加倍
        boolean canFoldPets = data.variant() == 3;
        int cost = canFoldPets ? 10 : 5;
        if (onCooldown(sp, data)) {
            return true;
        }
        if (!spendSpirit(sp, cost)) {
            return true;
        }

        CompoundTag tag = sp.getPersistentData();
        ListTag entries = tag.getList(FOLD_TAG, Tag.TAG_COMPOUND);

        // 潜行右键：优先收起瞄准的小动物（变体3），否则取出最后一条
        if (sp.isShiftKeyDown()) {
            LivingEntity pet = canFoldPets ? lookAtPet(sp, 5) : null;
            if (pet != null) {
                if (entries.size() >= capacity) {
                    sp.displayClientMessage(Component.translatable(
                            "message.guimi_mod.magic.fold_full"), true);
                    return true;
                }
                CompoundTag entry = new CompoundTag();
                entry.putString("k", "l");
                entry.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
                entry.put("data", pet.saveWithoutId(new CompoundTag()));
                entries.add(entry);
                saveFold(sp, entries, data);
                pet.discard();
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.fold_pet_stored",
                        pet.getDisplayName()), true);
                setCooldown(sp, data, 3);
                return true;
            }
            // 取出
            if (entries.isEmpty()) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.fold_empty"), true);
                return true;
            }
            if (!noDelay) {
                long until = tag.getLong(FOLD_DELAY_UNTIL);
                if (until <= gameTime(sp)) {
                    int delay = 1 + sp.serverLevel().random.nextInt(3);
                    tag.putLong(FOLD_DELAY_UNTIL, gameTime(sp) + delay * 20L);
                    sp.displayClientMessage(Component.translatable(
                            "message.guimi_mod.magic.fold_delay", delay), true);
                    return true;
                }
            }
            CompoundTag entry = (CompoundTag) entries.remove(entries.size() - 1);
            saveFold(sp, entries, data);
            if (entry.getString("k").equals("l")) {
                spawnStoredEntity(sp, entry);
            } else {
                ItemStack out = ItemStack.parse(sp.registryAccess(), entry.getCompound("data"))
                        .orElse(ItemStack.EMPTY);
                if (!out.isEmpty() && !sp.getInventory().add(out)) {
                    sp.level().addFreshEntity(new ItemEntity(sp.level(),
                            sp.getX(), sp.getY() + 0.5, sp.getZ(), out));
                }
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.fold_taken"), true);
            }
            setCooldown(sp, data, 3);
            return true;
        }

        // 右键：存物品
        if (entries.size() >= capacity) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.fold_full"), true);
            return true;
        }
        ItemStack toStore = ItemStack.EMPTY;
        if (!sp.getMainHandItem().isEmpty() && MagicArtifactItem.getData(sp.getMainHandItem()) == null) {
            toStore = sp.getMainHandItem().copy();
            sp.getMainHandItem().shrink(sp.getMainHandItem().getCount());
        } else {
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (!s.isEmpty() && MagicArtifactItem.getData(s) == null) {
                    toStore = s.copy();
                    s.shrink(s.getCount());
                    break;
                }
            }
        }
        if (toStore.isEmpty()) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.fold_nothing"), true);
            return true;
        }
        // 负面：5% 永久丢失（变体1 概率翻倍）
        if (sp.serverLevel().random.nextInt(100) < lossChance) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.fold_lost", toStore.getHoverName()), true);
            return true;
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("k", "i");
        entry.put("data", toStore.save(sp.registryAccess()));
        entries.add(entry);
        saveFold(sp, entries, data);
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.fold_stored", toStore.getHoverName()), true);
        setCooldown(sp, data, 3);
        return true;
    }

    private static void saveFold(ServerPlayer sp, ListTag entries, MagicArtifactData data) {
        sp.getPersistentData().put(FOLD_TAG, entries);
        // 变体2：记录存储时间，用于 24h 自动弹出
        if (data.variant() == 2) {
            sp.getPersistentData().putLong(FOLD_STORE_TIME, gameTime(sp));
        }
    }

    /** 重生折叠空间中的活物条目（变体3）。 */
    private static void spawnStoredEntity(ServerPlayer sp, CompoundTag entry) {
        EntityType<?> type = EntityType.byString(entry.getString("type")).orElse(null);
        if (type == null) {
            return;
        }
        CompoundTag data = entry.getCompound("data");
        Entity entity = type.create(sp.serverLevel());
        if (entity == null) {
            return;
        }
        entity.load(data);
        entity.moveTo(sp.getX(), sp.getY() + 0.5, sp.getZ(),
                entity.getYRot(), entity.getXRot());
        sp.serverLevel().addFreshEntity(entity);
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.fold_pet_released", entity.getDisplayName()), true);
    }

    /** 变体2 的 24h 自动弹出（由事件订阅器逐 tick 驱动）。 */
    public static void tickFoldAutoEject(ServerPlayer sp) {
        CompoundTag tag = sp.getPersistentData();
        if (!tag.contains(FOLD_STORE_TIME)) {
            return;
        }
        long storedAt = tag.getLong(FOLD_STORE_TIME);
        if (gameTime(sp) - storedAt < hoursToTicks(24)) {
            return;
        }
        ListTag entries = tag.getList(FOLD_TAG, Tag.TAG_COMPOUND);
        if (entries.isEmpty()) {
            tag.remove(FOLD_STORE_TIME);
            return;
        }
        // 弹出全部物品与活物
        for (int i = entries.size() - 1; i >= 0; i--) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.getString("k").equals("l")) {
                spawnStoredEntity(sp, entry);
            } else {
                ItemStack out = ItemStack.parse(sp.registryAccess(), entry.getCompound("data"))
                        .orElse(ItemStack.EMPTY);
                if (!out.isEmpty() && !sp.getInventory().add(out)) {
                    sp.level().addFreshEntity(new ItemEntity(sp.level(),
                            sp.getX(), sp.getY() + 0.5, sp.getZ(), out));
                }
            }
        }
        tag.remove(FOLD_TAG);
        tag.remove(FOLD_STORE_TIME);
        tag.remove(FOLD_DELAY_UNTIL);
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.fold_auto_eject"), true);
    }

    /** 从玩家视线拾取 ≤range 格内可被收容的小动物（变体3）。 */
    private static LivingEntity lookAtPet(ServerPlayer sp, double range) {
        LivingEntity entity = lookAtEntity(sp, range);
        if (entity != null && FOLDABLE_PETS.contains(entity.getType())) {
            return entity;
        }
        return null;
    }

    // =====================================================================
    // 序列7 占星人：星象解读
    // =====================================================================

    private static final int ASTRO_REVERSE_CHANCE = 20;

    private static boolean astrologerUse(ServerPlayer sp, MagicArtifactData data) {
        if (onCooldown(sp, data)) {
            return true;
        }
        // 变体1：准确率提升至 90%（反向概率 10%），解读时间翻倍
        int reverseChance = data.variant() == 1 ? 10 : ASTRO_REVERSE_CHANCE;
        // 变体2：预知具体事件，消耗灵性加倍
        int cost = data.variant() == 2 ? 20 : 10;
        if (!spendSpirit(sp, cost)) {
            return true;
        }

        ServerLevel level = sp.serverLevel();
        boolean rainNow = level.isRaining();
        boolean thunderNow = level.isThundering();
        boolean willRain = level.random.nextFloat() < 0.5 ? rainNow : !rainNow;
        boolean willThunder = willRain && level.random.nextFloat() < 0.3;

        boolean auspicious = level.random.nextBoolean();
        if (level.random.nextInt(100) < reverseChance) {
            auspicious = !auspicious;
        }

        String weatherKey = willThunder ? "magic.guimi_mod.weather.thunder"
                : willRain ? "magic.guimi_mod.weather.rain" : "magic.guimi_mod.weather.clear";
        String fortuneKey = auspicious ? "magic.guimi_mod.fortune.auspicious"
                : "magic.guimi_mod.fortune.inauspicious";

        // 变体3：结果以「谜语」形式呈现
        if (data.variant() == 3) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.astro_riddle"), true);
        } else {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.astro_result",
                    Component.translatable(weatherKey),
                    Component.translatable(fortuneKey)), true);
        }
        // 变体2：额外预知一条具体事件
        if (data.variant() == 2) {
            int event = level.random.nextInt(4);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.astro_event." + event), true);
        }
        // 解读期间无法感知周围（失明，易被偷袭）；变体1 时间翻倍
        int blindTicks = (data.variant() == 1 ? 12 : 6) * 20;
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0, false, true));

        setCooldown(sp, data, 8);
        return true;
    }

    // =====================================================================
    // 序列6 记录官：记录并使用超凡能力
    // =====================================================================

    /** 记录槽 1 / 2（变体2 同时记录 2 个）：{id, time, used}。 */
    private static final String REC_ABILITY = "gmmod_magic_record_ability";
    private static final String REC_TIME = "gmmod_magic_record_time";
    private static final String REC_USED = "gmmod_magic_record_used";
    private static final String REC2_ABILITY = "gmmod_magic_record2_ability";
    private static final String REC2_TIME = "gmmod_magic_record2_time";
    private static final String REC2_USED = "gmmod_magic_record2_used";

    private static final int RECORD_LOSE_CHANCE = 15;

    private static boolean recorderUse(ServerPlayer sp, MagicArtifactData data) {
        if (onCooldown(sp, data)) {
            return true;
        }
        CompoundTag tag = sp.getPersistentData();

        // 潜行右键：记录对准玩家身上已解锁的一个超凡能力（不高于自身序列）
        if (sp.isShiftKeyDown()) {
            LivingEntity target = lookAtEntity(sp, 10);
            if (!(target instanceof Player targetPlayer)) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.record_target"), true);
                return true;
            }
            int targetLevel = targetPlayer.getData(ModAttachments.SEQUENCE_LEVEL);
            if (targetLevel > 0 && targetLevel < data.level()) {
                // 对方序列高于自身（数字更小），不可记录
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.record_too_strong"), true);
                return true;
            }
            var abilities = SkillManager.getUnlockedAbilities(targetPlayer);
            if (abilities.isEmpty()) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.record_target"), true);
                return true;
            }
            String abilityId = abilities.get(0).getId().toString();
            String slotAbility = data.variant() == 2 ? REC2_ABILITY : REC_ABILITY;
            if (!tag.getString(slotAbility).isEmpty()) {
                if (data.variant() == 2 && tag.getString(REC_ABILITY).isEmpty()) {
                    slotAbility = REC_ABILITY;
                } else {
                    sp.displayClientMessage(Component.translatable(
                            "message.guimi_mod.magic.record_full"), true);
                    return true;
                }
            }
            String slotTime = slotAbility.equals(REC_ABILITY) ? REC_TIME : REC2_TIME;
            String slotUsed = slotAbility.equals(REC_ABILITY) ? REC_USED : REC2_USED;
            tag.putString(slotAbility, abilityId);
            tag.putLong(slotTime, gameTime(sp));
            tag.putInt(slotUsed, 0);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.recorded",
                    Component.translatable(abilityNameKey(abilityId))), true);
            setCooldown(sp, data, 3);
            return true;
        }

        // 右键：释放已记录的能力
        String recorded = tag.getString(REC_ABILITY);
        String slot = REC_ABILITY;
        long recordedTime = tag.getLong(REC_TIME);
        if (recorded.isEmpty() && data.variant() == 2) {
            recorded = tag.getString(REC2_ABILITY);
            slot = REC2_ABILITY;
            recordedTime = tag.getLong(REC2_TIME);
        }
        if (recorded.isEmpty()) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.record_none"), true);
            return true;
        }
        long now = gameTime(sp);
        if (now - recordedTime > hoursToTicks(24)) {
            // 记录超过 24 小时：自动失效（变体3 由事件订阅器触发反噬）
            clearRecordSlot(tag, slot);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.record_expired"), true);
            return true;
        }
        if (data.variant() == 1 && tag.getInt(slot.equals(REC_ABILITY) ? REC_USED : REC2_USED) >= 1) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.record_used"), true);
            return true;
        }
        Ability ability = AbilityRegistry.getById(ResourceLocation.tryParse(recorded));
        if (ability == null) {
            clearRecordSlot(tag, slot);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.record_invalid"), true);
            return true;
        }

        // 失控：15%（变体1 仅 5%）效果乱放，可能伤及自身
        int loseChance = data.variant() == 1 ? 5 : RECORD_LOSE_CHANCE;
        if (sp.serverLevel().random.nextInt(100) < loseChance) {
            loseControl(sp);
            markRecordUsed(tag, slot);
            setCooldown(sp, data, 5);
            return true;
        }

        // 正常释放：消耗灵性（变体2 双记录各半效 → 消耗翻倍）
        int cost = data.variant() == 2 ? 20 : 10;
        if (!spendSpirit(sp, cost)) {
            return true;
        }
        ability.onActivate(sp);
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.record_cast",
                Component.translatable(abilityNameKey(recorded))), true);
        markRecordUsed(tag, slot);
        setCooldown(sp, data, 5);
        return true;
    }

    private static void markRecordUsed(CompoundTag tag, String slot) {
        tag.putInt(slot.equals(REC_ABILITY) ? REC_USED : REC2_USED,
                tag.getInt(slot.equals(REC_ABILITY) ? REC_USED : REC2_USED) + 1);
    }

    private static void clearRecordSlot(CompoundTag tag, String slot) {
        if (slot.equals(REC_ABILITY)) {
            tag.remove(REC_ABILITY);
            tag.remove(REC_TIME);
            tag.remove(REC_USED);
        } else {
            tag.remove(REC2_ABILITY);
            tag.remove(REC2_TIME);
            tag.remove(REC2_USED);
        }
    }

    private static String abilityNameKey(String abilityId) {
        return "ability." + abilityId.replace(':', '.');
    }

    /** 失控：随机瞬移、眩晕并受轻微伤（模拟效果乱放，可能伤及自身）。 */
    private static void loseControl(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        Vec3 pos = sp.position();
        sp.teleportTo(sp.serverLevel(),
                pos.x + level.random.nextInt(17) - 8,
                pos.y + 1, pos.z + level.random.nextInt(17) - 8,
                sp.getYRot(), sp.getXRot());
        sp.hurt(sp.damageSources().magic(), 2.0F);
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1, false, true));
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.record_lose_control"), true);
    }

    /** 变体3 的 24h 反噬（由事件订阅器逐 tick 驱动）：超过时限未使用则遭受反噬。 */
    public static void tickRecordRebound(ServerPlayer sp) {
        CompoundTag tag = sp.getPersistentData();
        if (!tag.contains(REC_TIME)) {
            return;
        }
        long recordedAt = tag.getLong(REC_TIME);
        if (gameTime(sp) - recordedAt < hoursToTicks(24)) {
            return;
        }
        clearRecordSlot(tag, REC_ABILITY);
        sp.hurt(sp.damageSources().magic(), 3.0F);
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, true));
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.magic.record_rebound"), true);
    }

    // =====================================================================
    // 序列5 旅行家：传送门
    // =====================================================================

    private static final String MARKS_TAG = "gmmod_magic_marks";
    private static final int TRAVELER_MARKS = 3;
    private static final int TRAVELER_RANGE = 1000;
    private static final int TRAVELER_WRONG_CHANCE = 8;
    private static final int TRAVELER_DEVIATION = 100;
    private static final int TRAVELER_COST_PERCENT = 30;

    private static boolean travelerUse(ServerPlayer sp, MagicArtifactData data) {
        if (onCooldown(sp, data)) {
            return true;
        }
        // 变体1：距离翻倍但偏差翻倍
        int deviation = data.variant() == 1 ? TRAVELER_DEVIATION * 2 : TRAVELER_DEVIATION;
        // 变体2：灵性减半但冷却翻倍
        int costPercent = data.variant() == 2 ? TRAVELER_COST_PERCENT / 2 : TRAVELER_COST_PERCENT;
        int cd = data.variant() == 2 ? 40 : 20;
        // 变体3：最多 5 个标记，但每次传送后随机丢失 1 个
        int marksMax = data.variant() == 3 ? 5 : TRAVELER_MARKS;

        // 潜行右键：标记当前坐标
        if (sp.isShiftKeyDown()) {
            ListTag marks = sp.getPersistentData().getList(MARKS_TAG, Tag.TAG_LONG);
            if (marks.size() >= marksMax) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.magic.marks_full"), true);
                return true;
            }
            marks.add(LongTag.valueOf(sp.blockPosition().asLong()));
            sp.getPersistentData().put(MARKS_TAG, marks);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.marked", marks.size(), marksMax), true);
            return true;
        }

        ListTag marks = sp.getPersistentData().getList(MARKS_TAG, Tag.TAG_LONG);
        if (marks.isEmpty()) {
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.no_marks"), true);
            return true;
        }
        // 消耗最大灵性百分比
        int cost = ModAttachments.DEFAULT_SPIRITUALITY * costPercent / 100;
        if (!spendSpirit(sp, cost)) {
            return true;
        }

        // 8% 开错位置（±deviation）
        boolean wrong = sp.serverLevel().random.nextInt(100) < TRAVELER_WRONG_CHANCE;
        long chosen = ((LongTag) marks.get(sp.serverLevel().random.nextInt(marks.size()))).getAsLong();
        BlockPos dest = BlockPos.of(chosen);
        if (wrong) {
            dest = dest.offset(
                    sp.serverLevel().random.nextInt(deviation * 2 + 1) - deviation,
                    0,
                    sp.serverLevel().random.nextInt(deviation * 2 + 1) - deviation);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.travel_wrong"), true);
        }
        BlockPos destPos = null;
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos check = dest.above(dy);
            if (sp.level().getBlockState(check).isAir()
                    && sp.level().getBlockState(check.above()).isAir()) {
                destPos = check;
                break;
            }
        }
        if (destPos == null) {
            destPos = dest;
        }
        sp.teleportTo(sp.serverLevel(),
                destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5,
                sp.getYRot(), sp.getXRot());
        sp.serverLevel().sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.5, 0.5, 0.5, 0.1);

        // 变体3：每次传送后随机丢失 1 个标记
        if (data.variant() == 3 && marks.size() > 1) {
            marks.remove(sp.serverLevel().random.nextInt(marks.size()));
            sp.getPersistentData().put(MARKS_TAG, marks);
        }
        setCooldown(sp, data, cd);
        return true;
    }

    // =====================================================================
    // 辅助：视线内实体拾取
    // =====================================================================

    /** 从玩家视线拾取范围内第一个 LivingEntity（不含玩家自己）。 */
    public static LivingEntity lookAtEntity(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        var box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, start, end, box,
                e -> e != player && e instanceof LivingEntity && e.isPickable(),
                (float) (range * range));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }
}
