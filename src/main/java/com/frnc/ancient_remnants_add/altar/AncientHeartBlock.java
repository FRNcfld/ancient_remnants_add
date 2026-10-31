package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.waypoint.WaypointMarkerSync;
import com.frnc.ancient_remnants_add.worldgen.AncientHeartRuinStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 远古之心：2×2×1 多方块，也是残缺祭坛的「心脏」。
 *
 * <p>四格使用同一个方块，通过 {@link Part} 区分 ROOT 与三个附属格；只有 ROOT 持有
 * BlockEntity（仅作为休眠核心渲染锚点）。玩家把 2×2 放进残缺祭坛中央的 4 格空气后，
 * ROOT 会被标记为「已修复的祭坛站点」；之后由 Shift + 长按右键的祭坛仪式激活
 * （见 {@link AltarActivationHandler}）。
 *
 * <p>破坏任意一格会移除整组；站点记录不会被删除，玩家可以重新放回远古之心。
 */
public class AncientHeartBlock extends BaseEntityBlock {

    /** 2×2 方块的四个位置，dx/dz 是相对 ROOT 的偏移。 */
    public enum Part implements StringRepresentable {
        ROOT("root", 0, 0),
        EAST("east", 1, 0),
        SOUTH("south", 0, 1),
        SOUTH_EAST("south_east", 1, 1);

        private final String name;
        private final int dx;
        private final int dz;

        Part(String name, int dx, int dz) {
            this.name = name;
            this.dx = dx;
            this.dz = dz;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public int getDx() {
            return dx;
        }

        public int getDz() {
            return dz;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** 清理四格时防止 setBlock 递归触发 onRemove。 */
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public AncientHeartBlock() {
        super(Properties.of()
                .strength(3.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.DEEPSLATE)
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> state.getValue(ACTIVE) ? 10 : 0));
        registerDefaultState(stateDefinition.any()
                .setValue(PART, Part.ROOT)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.ROOT ? new AncientHeartBlockEntity(pos, state) : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        // 任意一格中键复制都得到同一个物品。
        return new ItemStack(AltarRegistry.ANCIENT_HEART_ITEM.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos root = context.getClickedPos();
        Level level = context.getLevel();
        for (Part part : Part.values()) {
            BlockPos cell = root.offset(part.getDx(), 0, part.getDz());
            if (!level.getWorldBorder().isWithinBounds(cell)) {
                return null;
            }
            BlockPlaceContext cellContext = BlockPlaceContext.at(context, cell, Direction.UP);
            if (!level.getBlockState(cell).canBeReplaced(cellContext)) {
                return null;
            }
        }
        return defaultBlockState().setValue(PART, Part.ROOT).setValue(ACTIVE, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        BlockPos root = getRootPos(pos, state);
        for (Part part : Part.values()) {
            if (part == Part.ROOT) {
                continue;
            }
            BlockPos cell = root.offset(part.getDx(), 0, part.getDz());
            if (!level.setBlock(cell, state.setValue(PART, part), 3)) {
                rollback(level, root);
                return;
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            markSiteRepaired(serverLevel, root);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!(level instanceof ServerLevel serverLevel) || player.isCreative()) {
            return;
        }
        // 1.20.1 的破坏流程先移除方块（onRemove 已清理整组），playerDestroy 之后才调用；
        // 所以不能读世界里的 ROOT 状态，直接用被破坏格的状态还原 ROOT 来掷战利品。
        BlockState rootState = state.setValue(PART, Part.ROOT);
        List<ItemStack> drops = Block.getDrops(rootState, serverLevel, pos, null, player, tool);
        for (ItemStack drop : drops) {
            Block.popResource(serverLevel, pos, drop);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()
                && !Boolean.TRUE.equals(REMOVING.get())) {
            REMOVING.set(Boolean.TRUE);
            try {
                cleanupGroup(level, getRootPos(pos, state));
            } finally {
                REMOVING.remove();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 移除整组四格，并熄灭站点、把站点里保存的典籍掉出来。
     *
     * <p>献祭处理器会先 {@code extinguish} 取走典籍再移除方块；这里第二次 extinguish
     * 返回空，不会双掉。站点记录保留，玩家可以重新放远古之心。
     */
    private static void cleanupGroup(Level level, BlockPos rootPos) {
        if (level instanceof ServerLevel serverLevel) {
            ItemStack codex = AltarSavedData.get(serverLevel).extinguish(rootPos);
            if (!codex.isEmpty()) {
                Block.popResource(serverLevel, rootPos, codex);
            }
            // 小地图：祭坛不再激活，移除所有玩家数据里的祭坛标记（含离线玩家的持久数据）。
            WaypointMarkerSync.altarExtinguished(serverLevel, rootPos);
        }
        for (Part part : Part.values()) {
            BlockPos cell = rootPos.offset(part.getDx(), 0, part.getDz());
            if (level.getBlockState(cell).is(AltarRegistry.ANCIENT_HEART.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void rollback(Level level, BlockPos rootPos) {
        for (Part part : Part.values()) {
            BlockPos cell = rootPos.offset(part.getDx(), 0, part.getDz());
            if (level.getBlockState(cell).is(AltarRegistry.ANCIENT_HEART.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** 玩家放置好 2×2 后，把对应的祭坛站点标记为已修复。 */
    private static void markSiteRepaired(ServerLevel level, BlockPos root) {
        if (!isComplete(level, root)) {
            return;
        }
        AltarSavedData data = AltarSavedData.get(level);
        if (data.findSiteAt(root) == null) {
            registerSiteFallback(level, root, data);
        }
        data.markRepaired(root);
    }

    /**
     * 旧世界（结构生成时尚未有站点登记）或 afterPlace 调度窗口内的兜底：
     * 如果 ROOT 确实位于 ancient_heart_ruin 结构里，就补登记站点。
     */
    private static void registerSiteFallback(ServerLevel level, BlockPos root, AltarSavedData data) {
        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(AncientHeartRuinStructure.ID);
        if (structure == null) {
            return;
        }
        StructureStart start = level.structureManager().getStructureAt(root, structure);
        if (!start.isValid()) {
            return;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos origin = new BlockPos(box.minX(), box.minY(), box.minZ());
        if (AltarGeometry.siteRootFromOrigin(origin).equals(root)) {
            data.registerSite(root);
        }
    }

    public static BlockPos getRootPos(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        return pos.offset(-part.getDx(), 0, -part.getDz());
    }

    /** 祭坛交互用：四格都必须是本方块且 part 与位置对应（相邻区块必须已加载）。 */
    public static boolean isComplete(LevelReader level, BlockPos rootPos) {
        for (Part part : Part.values()) {
            BlockPos cell = rootPos.offset(part.getDx(), 0, part.getDz());
            BlockState state = level.getBlockState(cell);
            if (!state.is(AltarRegistry.ANCIENT_HEART.get()) || state.getValue(PART) != part) {
                return false;
            }
        }
        return true;
    }

    /** 把四格的 ACTIVE 一起设置；缺失的格子跳过。 */
    public static void setActive(Level level, BlockPos rootPos, boolean active) {
        for (Part part : Part.values()) {
            BlockPos cell = rootPos.offset(part.getDx(), 0, part.getDz());
            BlockState state = level.getBlockState(cell);
            if (state.is(AltarRegistry.ANCIENT_HEART.get())
                    && state.getValue(PART) == part
                    && state.getValue(ACTIVE) != active) {
                level.setBlock(cell, state.setValue(ACTIVE, active), 3);
            }
        }
    }
}
