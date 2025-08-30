package com.example.sailsplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MastBlock extends Block {
    
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    
    // Define shapes for different orientations
    // Vertical (Y-axis): from [6, 0, 6] to [10, 16, 10]
    private static final VoxelShape VERTICAL_SHAPE = Shapes.box(
            6.0/16.0, 0.0, 6.0/16.0,      // min x, y, z
            10.0/16.0, 1.0, 10.0/16.0     // max x, y, z
    );
    
    // Horizontal X-axis: from [0, 6, 6] to [16, 10, 10]
    private static final VoxelShape HORIZONTAL_X_SHAPE = Shapes.box(
            0.0, 6.0/16.0, 6.0/16.0,      // min x, y, z
            1.0, 10.0/16.0, 10.0/16.0     // max x, y, z
    );
    
    // Horizontal Z-axis: from [6, 6, 0] to [10, 10, 16]
    private static final VoxelShape HORIZONTAL_Z_SHAPE = Shapes.box(
            6.0/16.0, 6.0/16.0, 0.0,      // min x, y, z
            10.0/16.0, 10.0/16.0, 1.0     // max x, y, z
    );

    // Flush shifted shapes (6 unit / 6px shift) for horizontal X axis (shift along Z)
    private static final VoxelShape HORIZONTAL_X_FLUSH_NORTH = Shapes.box(
        0.0, 6.0/16.0, 12.0/16.0,          // z 12..16 now for north (swapped)
        1.0, 10.0/16.0, 1.0
    );
    private static final VoxelShape HORIZONTAL_X_FLUSH_SOUTH = Shapes.box(
        0.0, 6.0/16.0, 0.0,    // z 0..4 now for south (swapped)
        1.0, 10.0/16.0, 4.0/16.0
    );

    // Flush shifted shapes for horizontal Z axis (shift along X)
    private static final VoxelShape HORIZONTAL_Z_FLUSH_EAST = Shapes.box(
        0.0, 6.0/16.0, 0.0,    // x 0..4 now for east (swapped naming)
        4.0/16.0, 10.0/16.0, 1.0
    );
    private static final VoxelShape HORIZONTAL_Z_FLUSH_WEST = Shapes.box(
        12.0/16.0, 6.0/16.0, 0.0,          // x 12..16 now for west (swapped)
        1.0, 10.0/16.0, 1.0
    );
    
    // Flush side enum indicates which adjacent face this mast is flush against (shifted toward)
    public enum FlushSide implements StringRepresentable {
        NONE("none"),
        NORTH("north"),
        SOUTH("south"),
        EAST("east"),
        WEST("west");

        private final String name;
        FlushSide(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }

        public static FlushSide fromDirection(Direction dir) {
            return switch (dir) {
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> NONE; // UP / DOWN unsupported
            };
        }
    }

    public static final EnumProperty<FlushSide> FLUSH_SIDE = EnumProperty.create("flush_side", FlushSide.class);

    public MastBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(FLUSH_SIDE, FlushSide.NONE));
    }
    
    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        // Currently flush does not alter collision; could later shift / shrink depending on decision.
        Direction.Axis axis = state.getValue(AXIS);
        FlushSide flush = state.getValue(FLUSH_SIDE);
        if (axis == Direction.Axis.Y) {
            return VERTICAL_SHAPE; // ignoring flush on vertical for now
        }
        if (axis == Direction.Axis.X) {
            return switch (flush) {
                case NORTH -> HORIZONTAL_X_FLUSH_NORTH;
                case SOUTH -> HORIZONTAL_X_FLUSH_SOUTH;
                default -> HORIZONTAL_Z_SHAPE; // base shape (note naming historical)
            };
        }
        // axis == Z
        return switch (flush) {
            case EAST -> HORIZONTAL_Z_FLUSH_EAST;
            case WEST -> HORIZONTAL_Z_FLUSH_WEST;
            default -> HORIZONTAL_X_SHAPE; // base shape
        };
    }
    
    @Override
    @Nullable
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockState base = this.defaultBlockState();

        // Preliminary axis selection (may be overridden for flush placement)
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            base = base.setValue(AXIS, Direction.Axis.Y);
        } else {
            // default (non-flush) behavior still uses player facing to choose axis
            Direction playerFacing = context.getHorizontalDirection();
            Direction.Axis axis = (playerFacing.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
            base = base.setValue(AXIS, axis);
        }

        // Flush logic: if player is crouching & clicked face is horizontal, mark flush toward that face (shift into block)
        boolean crouching = false;
        var player = context.getPlayer();
        if (player != null) {
            crouching = player.isShiftKeyDown();
        }
    if (crouching && clickedFace.getAxis().isHorizontal()) {
        base = base.setValue(FLUSH_SIDE, FlushSide.fromDirection(clickedFace));
        // For flush placement, force axis to run along the face (perpendicular to its normal)
        Direction.Axis flushAxis = (clickedFace == Direction.NORTH || clickedFace == Direction.SOUTH)
            ? Direction.Axis.X : Direction.Axis.Z;
        base = base.setValue(AXIS, flushAxis);
    }

        // Inherit flush from any adjacent existing flush mast (horizontal or vertical) if not already explicitly set this placement
        if (base.getValue(FLUSH_SIDE) == FlushSide.NONE) {
            BlockPos placePos = context.getClickedPos();
            // Check all 6 directions to propagate chain flush behavior
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = placePos.relative(dir);
                BlockState neighbor = context.getLevel().getBlockState(neighborPos);
                if (neighbor.getBlock() instanceof MastBlock) {
                    FlushSide neighborFlush = neighbor.getValue(FLUSH_SIDE);
                    if (neighborFlush != FlushSide.NONE) {
                        base = base.setValue(FLUSH_SIDE, neighborFlush);
                        // Ensure axis matches flush side orientation
                        if (neighborFlush == FlushSide.NORTH || neighborFlush == FlushSide.SOUTH) {
                            base = base.setValue(AXIS, Direction.Axis.X);
                        } else if (neighborFlush == FlushSide.EAST || neighborFlush == FlushSide.WEST) {
                            base = base.setValue(AXIS, Direction.Axis.Z);
                        }
                        break;
                    }
                }
            }
        }

        // Final consistency pass: if flush_side set but axis incompatible, correct it
        FlushSide fs = base.getValue(FLUSH_SIDE);
        if (fs != FlushSide.NONE) {
            if ((fs == FlushSide.NORTH || fs == FlushSide.SOUTH) && base.getValue(AXIS) != Direction.Axis.X) {
                base = base.setValue(AXIS, Direction.Axis.X);
            } else if ((fs == FlushSide.EAST || fs == FlushSide.WEST) && base.getValue(AXIS) != Direction.Axis.Z) {
                base = base.setValue(AXIS, Direction.Axis.Z);
            }
        }

        return base;
    }
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, FLUSH_SIDE);
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            player.sendSystemMessage(Component.literal("Mast placed axis=" + state.getValue(AXIS) + " flush_side=" + state.getValue(FLUSH_SIDE)));
        }
    }
}
