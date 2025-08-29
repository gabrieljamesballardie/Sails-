package com.example.sailsplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
    
    public MastBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }
    
    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> HORIZONTAL_Z_SHAPE;  // X-axis uses base model (Z-oriented), so use Z-shape
            case Y -> VERTICAL_SHAPE;
            case Z -> HORIZONTAL_X_SHAPE;  // Z-axis uses rotated model (X-oriented), so use X-shape
        };
    }
    
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        
        // If placing on top or bottom of a block, make it vertical
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            return this.defaultBlockState().setValue(AXIS, Direction.Axis.Y);
        }
        // If placing on the side of a block, align perpendicular to that face
        else {
            // If clicking on north/south faces, mast should run east-west (X-axis)
            // If clicking on east/west faces, mast should run north-south (Z-axis)
            Direction.Axis axis = (clickedFace == Direction.NORTH || clickedFace == Direction.SOUTH) 
                ? Direction.Axis.X 
                : Direction.Axis.Z;
            return this.defaultBlockState().setValue(AXIS, axis);
        }
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
