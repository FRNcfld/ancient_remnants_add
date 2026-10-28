package com.frnc.ancient_remnants_add.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;

/**
 * 巨石祭坛。
 *
 * <p>玩家在获得「远古回响」挑战进度后，手持一本已解锁全部条目的巨石典籍右键祭坛，
 * 即可把典籍放上去并激活该维度的丰收扩展效果。
 */
public class MonolithAltarBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MonolithAltarBlock() {
        super(Properties.of()
                .strength(3.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.DEEPSLATE)
                .lightLevel(state -> state.getValue(ACTIVE) ? 10 : 0));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonolithAltarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            // 客户端只按看得见的条件判断，并尽量与服务端走同一分支：
            // 已激活 + 空手 = 取回（服务端消费）；已激活 + 手持物品 = 交给别的交互（服务端 PASS）；
            // 未激活时服务端一定会消费这次交互（尝试放典籍或给出提示），客户端别去放方块。
            if (state.getValue(ACTIVE)) {
                return player.getItemInHand(hand).isEmpty()
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(ACTIVE)) {
            return takeCodexBack(state, level, pos, serverPlayer, hand);
        }

        // 先确认祭坛能收下典籍，再动进度：否则祭坛放不进去，进度却已经发放了。
        if (!(level.getBlockEntity(pos) instanceof MonolithAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (!altar.getCodex().isEmpty()) {
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!AncientEchoAdvancement.isCompleteCodex(serverPlayer, held)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.ancient_remnants_add.altar.incomplete"), true);
            return InteractionResult.CONSUME;
        }
        if (!AncientEchoAdvancement.tryAward(serverPlayer)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.ancient_remnants_add.altar.no_advancement"), true);
            return InteractionResult.CONSUME;
        }

        altar.setCodex(held);
        BlockState activeState = state.setValue(ACTIVE, true);
        level.setBlock(pos, activeState, 3);
        level.sendBlockUpdated(pos, state, activeState, 3);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        if (level instanceof ServerLevel serverLevel) {
            AltarSavedData.get(serverLevel).add(pos);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverPlayer.displayClientMessage(
                Component.translatable("message.ancient_remnants_add.altar.activated"), false);
        AncientRemnantsAdd.debug("巨石祭坛已激活: {}", pos);
        return InteractionResult.CONSUME;
    }

    /** 空手右击已激活的祭坛：取回典籍并停用祭坛。 */
    private InteractionResult takeCodexBack(BlockState state, Level level, BlockPos pos,
                                            ServerPlayer player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof MonolithAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }

        ItemStack codex = altar.getCodex();
        altar.setCodex(ItemStack.EMPTY);

        BlockState inactiveState = state.setValue(ACTIVE, false);
        level.setBlock(pos, inactiveState, 3);
        level.sendBlockUpdated(pos, state, inactiveState, 3);

        if (!codex.isEmpty() && !player.getInventory().add(codex)) {
            player.drop(codex, false);
        }
        if (level instanceof ServerLevel serverLevel) {
            AltarSavedData.get(serverLevel).remove(pos);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.7F);
        player.displayClientMessage(
                Component.translatable("message.ancient_remnants_add.altar.deactivated"), false);
        AncientRemnantsAdd.debug("巨石祭坛已停用: {}", pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof MonolithAltarBlockEntity altar) {
                ItemStack codex = altar.getCodex();
                if (!codex.isEmpty()) {
                    Block.popResource(level, pos, codex);
                }
            }
            if (level instanceof ServerLevel serverLevel) {
                AltarSavedData.get(serverLevel).remove(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
