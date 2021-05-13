package twilightforest.structures.darktower;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.HorizontalFaceBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RedstoneDiodeBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.template.TemplateManager;
import twilightforest.TFFeature;
import twilightforest.block.TFBlocks;
import twilightforest.entity.TFEntities;
import twilightforest.loot.TFTreasure;
import twilightforest.structures.StructureTFComponentOld;
import twilightforest.structures.StructureTFDecorator;
import twilightforest.structures.lichtower.ComponentTFTowerRoof;
import twilightforest.structures.lichtower.ComponentTFTowerRoofAttachedSlab;
import twilightforest.structures.lichtower.ComponentTFTowerRoofFence;
import twilightforest.structures.lichtower.ComponentTFTowerRoofGableForwards;
import twilightforest.structures.lichtower.ComponentTFTowerRoofSlabForwards;
import twilightforest.structures.lichtower.ComponentTFTowerWing;
import twilightforest.util.RotationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ComponentTFDarkTowerWing extends ComponentTFTowerWing {
	protected boolean keyTower = false;
	protected ArrayList<EnumDarkTowerDoor> openingTypes = new ArrayList<>();

	public ComponentTFDarkTowerWing(TemplateManager manager, CompoundNBT nbt) {
		this(TFDarkTowerPieces.TFDTWin, nbt);
	}

	public ComponentTFDarkTowerWing(IStructurePieceType piece, CompoundNBT nbt) {
		super(piece, nbt);
		this.keyTower = nbt.getBoolean("keyTower");

		this.readDoorsTypesFromArray(nbt.getIntArray("doorTypeInts"));
	}

	protected ComponentTFDarkTowerWing(IStructurePieceType piece, TFFeature feature, int i, int x, int y, int z, int pSize, int pHeight, Direction direction) {
		super(piece, feature, i, x, y, z, pSize, pHeight, direction);
	}

	/**
	 * Turn the openings array into an array of ints.
	 */
	private int[] getDoorsTypesAsIntArray() {
		int[] ret = new int[this.openingTypes.size()];

		int idx = 0;

		for (EnumDarkTowerDoor doorType : openingTypes) {
			ret[idx++] = doorType.ordinal();
		}

		return ret;
	}

	@Override
	protected void readAdditional(CompoundNBT tagCompound) {
		super.readAdditional(tagCompound);

		tagCompound.putBoolean("keyTower", this.keyTower);

		tagCompound.putIntArray("doorTypeInts", this.getDoorsTypesAsIntArray());
	}

	/**
	 * Read in opening types from int array
	 */
	private void readDoorsTypesFromArray(int[] intArray) {
		for (int typeInt : intArray) {
			this.openingTypes.add(EnumDarkTowerDoor.values()[typeInt]);
		}
	}

	@Override
	public void buildComponent(StructurePiece parent, List<StructurePiece> list, Random rand) {
		if (parent != null && parent instanceof StructureTFComponentOld) {
			this.deco = ((StructureTFComponentOld) parent).deco;
		}

		// we should have a door where we started
		addOpening(0, 1, size / 2, Rotation.CLOCKWISE_180);

		// add a roof?
		makeARoof(parent, list, rand);

		// add a beard
		makeABeard(parent, list, rand);

		if (size > 10) {
			// sub towers
			for (Rotation direction : RotationUtil.ROTATIONS) {
				int[] dest = getValidOpening(rand, direction);
				int childHeight = validateChildHeight(height - 4 + rand.nextInt(10) - rand.nextInt(10));

				boolean madeWing = makeTowerWing(list, rand, this.getComponentType(), dest[0], dest[1], dest[2], size - 2, childHeight, direction);

				// occasional balcony
				if (!madeWing && (direction == Rotation.CLOCKWISE_180 || rand.nextBoolean())) {
					makeTowerBalcony(list, rand, this.getComponentType(), dest[0], dest[1], dest[2], direction);
				}
			}
		} else if (rand.nextInt(4) == 0) {
			// occasional balcony on small towers too
			Rotation direction = RotationUtil.ROTATIONS[rand.nextInt(4)];
			int[] dest = getValidOpening(rand, direction);
			makeTowerBalcony(list, rand, this.getComponentType(), dest[0], dest[1], dest[2], direction);
		}
	}

	protected int validateChildHeight(int childHeight) {
		return (childHeight / 4) * 4 + 1;
	}

	/**
	 * Attach a roof to this tower.
	 */
	@Override
	public void makeARoof(StructurePiece parent, List<StructurePiece> list, Random rand) {
		int index = this.getComponentType();

		ComponentTFTowerRoof roof;

		switch (rand.nextInt(5)) {
			case 0:
			case 1:
			default:
				roof = new ComponentTFDarkTowerRoofAntenna(getFeatureType(), index, this);
				break;
			case 2:
				roof = new ComponentTFDarkTowerRoofCactus(getFeatureType(), index, this);
				break;
			case 3:
				roof = new ComponentTFDarkTowerRoofRings(getFeatureType(), index, this);
				break;
			case 4:
				roof = new ComponentTFDarkTowerRoofFourPost(getFeatureType(), index, this);
				break;
		}

		list.add(roof);
		roof.buildComponent(this, list, rand);
		roofType = roof.getClass();
	}

	@Override
	protected void makeAttachedRoof(List<StructurePiece> list, Random rand) {
		int index = this.getComponentType();
		ComponentTFTowerRoof roof;

		// this is our preferred roof type:
		if (roofType == null && rand.nextInt(32) != 0) {
			tryToFitRoof(list, rand, new ComponentTFTowerRoofGableForwards(getFeatureType(), index + 1, this));
		}

		// this is for roofs that don't fit.
		if (roofType == null && rand.nextInt(8) != 0) {
			tryToFitRoof(list, rand, new ComponentTFTowerRoofSlabForwards(getFeatureType(), index + 1, this));
		}

		// finally, if we're cramped for space, try this
		if (roofType == null && rand.nextInt(32) != 0) {
			// fall through to this next roof
			roof = new ComponentTFTowerRoofAttachedSlab(getFeatureType(), index + 1, this);
			tryToFitRoof(list, rand, roof);
		}

		// last resort
		if (roofType == null) {
			// fall through to this next roof
			roof = new ComponentTFTowerRoofFence(getFeatureType(), index + 1, this);
			tryToFitRoof(list, rand, roof);
		}
	}

	/**
	 * Add a beard to this structure.  There is only one type of beard.
	 */
	@Override
	public void makeABeard(StructurePiece parent, List<StructurePiece> list, Random rand) {
		ComponentTFDarkTowerBeard beard = new ComponentTFDarkTowerBeard(getFeatureType(), this.getComponentType() + 1, this);
		list.add(beard);
		beard.buildComponent(this, list, rand);
	}

	/**
	 * Make another wing just like this one
	 */
	@Override
	public boolean makeTowerWing(List<StructurePiece> list, Random rand, int index, int x, int y, int z, int wingSize, int wingHeight, Rotation rotation) {
		// kill too-small towers
		if (wingHeight < 8) {
			return false;
		}

		Direction direction = getStructureRelativeRotation(rotation);
		int[] dx = offsetTowerCoords(x, y, z, 5, direction);

		if (dx[1] + wingHeight > 250) {
			// end of the world!
			return false;
		}

		ComponentTFDarkTowerBridge bridge = new ComponentTFDarkTowerBridge(TFDarkTowerPieces.TFDTBri, getFeatureType(), index, dx[0], dx[1], dx[2], wingSize, wingHeight, direction);
		// check to see if it intersects something already there
		StructurePiece intersect = StructurePiece.findIntersecting(list, bridge.getBoundingBox());
		if (intersect == null || intersect == this) {
			intersect = StructurePiece.findIntersecting(list, bridge.getWingBB());
		} else {
			return false;
		}
		if (intersect == null || intersect == this) {
			list.add(bridge);
			bridge.buildComponent(this, list, rand);
			addOpening(x, y, z, rotation);
			return true;
		} else {
			return false;
		}
	}

	protected boolean makeTowerBalcony(List<StructurePiece> list, Random rand, int index, int x, int y, int z, Rotation rotation) {
		Direction direction = getStructureRelativeRotation(rotation);
		int[] dx = offsetTowerCoords(x, y, z, 5, direction);

		ComponentTFDarkTowerBalcony balcony = new ComponentTFDarkTowerBalcony(getFeatureType(), index, dx[0], dx[1], dx[2], direction);
		// check to see if it intersects something already there
		StructurePiece intersect = StructurePiece.findIntersecting(list, balcony.getBoundingBox());
		if (intersect == null || intersect == this) {
			list.add(balcony);
			balcony.buildComponent(this, list, rand);
			addOpening(x, y, z, rotation, EnumDarkTowerDoor.REAPPEARING);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean func_230383_a_(ISeedReader world, StructureManager manager, ChunkGenerator generator, Random rand, MutableBoundingBox sbb, ChunkPos chunkPosIn, BlockPos blockPos) {
		Random decoRNG = new Random(world.getSeed() + (this.boundingBox.minX * 321534781) ^ (this.boundingBox.minZ * 756839));

		// make walls
		makeEncasedWalls(world, rand, sbb, 0, 0, 0, size - 1, height - 1, size - 1);

		// clear inside
		fillWithAir(world, sbb, 1, 1, 1, size - 2, height - 2, size - 2);

		// sky light
//		nullifySkyLightForBoundingBox(world);

		if (this.size > 9) {
			// half floors, always starting at y = 4
			addHalfFloors(world, decoRNG, sbb, 4, height - 1);
		} else {
			if (decoRNG.nextInt(3) == 0) {
				addSmallTimberBeams(world, decoRNG, sbb, 4, height - 1);
			} else {
				addHalfFloors(world, decoRNG, sbb, 4, height - 1);
			}
		}

		// openings
		makeOpenings(world, sbb);

		// destroy some towers
		if (decoRNG.nextBoolean() && !this.isKeyTower() && this.height > 8) {
			int blobs = 1;

			if (this.size > 9 && decoRNG.nextBoolean()) {
				blobs++;
			}

			for (int i = 0; i < blobs; i++) {

				// find a random spot in the tower
				int x = decoRNG.nextInt(size);
				int y = decoRNG.nextInt(height - 7) + 2;
				int z = decoRNG.nextInt(size);

				destroyTower(world, decoRNG, x, y, z, 3, sbb);
			}
		}

		return true;
	}

	/**
	 * Add a destruction burst
	 */
	protected void destroyTower(ISeedReader world, Random decoRNG, int x, int y, int z, int amount, MutableBoundingBox sbb) {
		//makeNetherburst(world, decoRNG, 16, 100, 40, x, y, z, 0, sbb);

		int initialRadius = decoRNG.nextInt(amount) + amount;

		drawBlob(world, x, y, z, initialRadius, AIR, sbb);

		for (int i = 0; i < 3; i++) {
			int dx = x + (initialRadius - 1) * (decoRNG.nextBoolean() ? 1 : -1);
			int dy = y + (initialRadius - 1) * (decoRNG.nextBoolean() ? 1 : -1);
			int dz = z + (initialRadius - 1) * (decoRNG.nextBoolean() ? 1 : -1);

			netherTransformBlob(world, decoRNG, dx, dy, dz, initialRadius - 1, sbb);
			drawBlob(world, dx, dy, dz, initialRadius - 2, AIR, sbb);
		}
	}

	private void netherTransformBlob(ISeedReader world, Random inRand, int sx, int sy, int sz, int rad, MutableBoundingBox sbb) {

		Random rand = new Random(inRand.nextLong());

		// then trace out a quadrant
		for (byte dx = 0; dx <= rad; dx++) {
			for (byte dy = 0; dy <= rad; dy++) {
				for (byte dz = 0; dz <= rad; dz++) {
					// determine how far we are from the center.
					byte dist;
					if (dx >= dy && dx >= dz) {
						dist = (byte) (dx + (byte) ((Math.max(dy, dz) * 0.5) + (Math.min(dy, dz) * 0.25)));
					} else if (dy >= dx && dy >= dz) {
						dist = (byte) (dy + (byte) ((Math.max(dx, dz) * 0.5) + (Math.min(dx, dz) * 0.25)));
					} else {
						dist = (byte) (dz + (byte) ((Math.max(dx, dy) * 0.5) + (Math.min(dx, dy) * 0.25)));
					}

					// if we're inside the blob, fill it
					if (dist <= rad) {
						// do eight at a time for easiness!
						testAndChangeToNetherrack(world, rand, sx + dx, sy + dy, sz + dz, sbb);
						testAndChangeToNetherrack(world, rand, sx + dx, sy + dy, sz + dz, sbb);
						testAndChangeToNetherrack(world, rand, sx + dx, sy + dy, sz - dz, sbb);
						testAndChangeToNetherrack(world, rand, sx - dx, sy + dy, sz + dz, sbb);
						testAndChangeToNetherrack(world, rand, sx - dx, sy + dy, sz - dz, sbb);
						testAndChangeToNetherrack(world, rand, sx + dx, sy - dy, sz + dz, sbb);
						testAndChangeToNetherrack(world, rand, sx + dx, sy - dy, sz - dz, sbb);
						testAndChangeToNetherrack(world, rand, sx - dx, sy - dy, sz + dz, sbb);
						testAndChangeToNetherrack(world, rand, sx - dx, sy - dy, sz - dz, sbb);
					}
				}
			}
		}
	}

	private void testAndChangeToNetherrack(ISeedReader world, Random rand, int x, int y, int z, MutableBoundingBox sbb) {
		if (this.getBlockStateFromPos(world, x, y, z, sbb).getBlock() != Blocks.AIR) {
			this.setBlockState(world, Blocks.NETHERRACK.getDefaultState(), x, y, z, sbb);

			if (this.getBlockStateFromPos(world, x, y + 1, z, sbb).getBlock() == Blocks.AIR && rand.nextBoolean()) {
				this.setBlockState(world, Blocks.FIRE.getDefaultState(), x, y + 1, z, sbb);
			}
		}
	}

	/**
	 * Draw a giant blob of whatevs (okay, it's going to be leaves).
	 */
	private void drawBlob(ISeedReader world, int sx, int sy, int sz, int rad, BlockState state, MutableBoundingBox sbb) {
		// then trace out a quadrant
		for (byte dx = 0; dx <= rad; dx++) {
			for (byte dy = 0; dy <= rad; dy++) {
				for (byte dz = 0; dz <= rad; dz++) {
					// determine how far we are from the center.
					byte dist;
					if (dx >= dy && dx >= dz) {
						dist = (byte) (dx + (byte) ((Math.max(dy, dz) * 0.5) + (Math.min(dy, dz) * 0.25)));
					} else if (dy >= dx && dy >= dz) {
						dist = (byte) (dy + (byte) ((Math.max(dx, dz) * 0.5) + (Math.min(dx, dz) * 0.25)));
					} else {
						dist = (byte) (dz + (byte) ((Math.max(dx, dy) * 0.5) + (Math.min(dx, dy) * 0.25)));
					}

					// if we're inside the blob, fill it
					if (dist <= rad) {
						// do eight at a time for easiness!
						this.setBlockState(world, state, sx + dx, sy + dy, sz + dz, sbb);
						this.setBlockState(world, state, sx + dx, sy + dy, sz - dz, sbb);
						this.setBlockState(world, state, sx - dx, sy + dy, sz + dz, sbb);
						this.setBlockState(world, state, sx - dx, sy + dy, sz - dz, sbb);
						this.setBlockState(world, state, sx + dx, sy - dy, sz + dz, sbb);
						this.setBlockState(world, state, sx + dx, sy - dy, sz - dz, sbb);
						this.setBlockState(world, state, sx - dx, sy - dy, sz + dz, sbb);
						this.setBlockState(world, state, sx - dx, sy - dy, sz - dz, sbb);
					}
				}
			}
		}
	}

	/**
	 * Add a bunch of random half floors
	 */
	@SuppressWarnings("fallthrough")
	private void addHalfFloors(ISeedReader world, Random rand, MutableBoundingBox sbb, int bottom, int top) {

		int spacing = 4;//this.size > 9 ? 4 : 3;
		Rotation rotation = RotationUtil.ROTATIONS[(this.boundingBox.minY + bottom) % 3];

		if (bottom == 0) {
			bottom += spacing;
		}

		// fill with half floors
		for (int y = bottom; y < top; y += spacing) {
			rotation = rotation.add(Rotation.CLOCKWISE_180);

			if (y >= top - spacing) {
				makeFullFloor(world, sbb, y);
				if (isDeadEnd()) {
					decorateTreasureRoom(world, sbb, rotation, y, 4, this.deco);
				}
			} else {
				makeHalfFloor(world, sbb, rotation, y);

				// decorate
				// FIXME: Case 1 gets double weight when size >= 11
				switch (rand.nextInt(8)) {
					case 0:
						if (this.size < 11) {
							decorateReappearingFloor(world, sbb, rotation, y);
							break;
						}
					case 1:
						decorateSpawner(world, rand, sbb, rotation, y);
						break;
					case 2:
						decorateLounge(world, sbb, rotation, y);
						break;
					case 3:
						decorateLibrary(world, sbb, rotation, y);
						break;
					case 4:
						decorateExperimentPulser(world, sbb, rotation, y);
						break;
					case 5:
						decorateExperimentLamp(world, sbb, rotation, y);
						break;
					case 6:
						decoratePuzzleChest(world, sbb, rotation, y);
						break;
				}
			}

			addStairsDown(world, sbb, rotation, y, size - 2, spacing);
			if (this.size > 9) {
				// double wide staircase
				addStairsDown(world, sbb, rotation, y, size - 3, spacing);
			}
		}

		rotation = rotation.add(Rotation.CLOCKWISE_180);

		// stairs to roof
		addStairsDown(world, sbb, rotation, this.height - 1, size - 2, spacing);
	}

	/**
	 * Dark tower half floors
	 */
	protected void makeHalfFloor(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		this.fillBlocksRotated(world, sbb, size / 2, y, 1, size - 2, y, size - 2, deco.blockState, rotation);
		this.fillBlocksRotated(world, sbb, size / 2 - 1, y, 1, size / 2 - 1, y, size - 2, deco.accentState, rotation);
	}

	/**
	 * Dark tower full floors
	 */
	protected void makeFullFloor(ISeedReader world, MutableBoundingBox sbb, int y) {
		// half floor
		this.fillWithBlocks(world, sbb, 1, y, 1, size - 2, y, size - 2, deco.blockState, Blocks.AIR.getDefaultState(), false);
		this.fillWithBlocks(world, sbb, size / 2, y, 1, size / 2, y, size - 2, deco.accentState, Blocks.AIR.getDefaultState(), true);
	}

	/**
	 * Dark tower treasure rooms!
	 *
	 * @param myDeco
	 */
	protected void decorateTreasureRoom(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y, int spacing, StructureTFDecorator myDeco) {
		//treasure chest!
		int x = this.size / 2;
		int z = this.size / 2;

		this.makePillarFrame(world, sbb, this.deco, rotation, x - 1, y, z - 1, true);

		setBlockStateRotated(world, myDeco.platformState, x, y + 1, z, rotation, sbb);

		placeTreasureAtCurrentPosition(world, x, y + 2, z, this.isKeyTower() ? TFTreasure.darktower_key : TFTreasure.darktower_cache, sbb);
	}

	private void decorateSpawner(ISeedReader world, Random rand, MutableBoundingBox sbb, Rotation rotation, int y) {
		int x = this.size > 9 ? 4 : 3;
		int z = this.size > 9 ? 5 : 4;

		EntityType<?> mobID;

		if (this.size > 9) {
			mobID = rand.nextBoolean() ? TFEntities.tower_golem : TFEntities.tower_broodling;
		} else {
			mobID = TFEntities.tower_broodling;
		}

		// pillar frame
		this.makePillarFrame(world, sbb, this.deco, rotation, x, y, z, true);
		this.setSpawnerRotated(world, x + 1, y + 2, z + 1, rotation, mobID, sbb);
	}

	/**
	 * A lounge with a couch and table
	 */
	private void decorateLounge(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		int cx = this.size > 9 ? 9 : 7;
		int cz = this.size > 9 ? 4 : 3;

		setBlockStateRotated(world, getStairState(deco.stairState, Direction.SOUTH, false), cx, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.WEST, false), cx, y + 1, cz + 1, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.NORTH, false), cx, y + 1, cz + 2, rotation, sbb);

		cx = this.size > 9 ? 5 : 3;

		setBlockStateRotated(world, getStairState(deco.stairState, Direction.SOUTH, true), cx, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, getSlabState(Blocks.SPRUCE_SLAB.getDefaultState(), SlabType.TOP), cx, y + 1, cz + 1, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.NORTH, true), cx, y + 1, cz + 2, rotation, sbb);
	}

	/**
	 * Decorate with a pressure plate triggered reappearing floor.  Only suitable for small towers
	 */
	private void decorateReappearingFloor(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		final BlockState inactiveReappearing = TFBlocks.reappearing_block.get().getDefaultState();
		final BlockState woodenPressurePlate = Blocks.OAK_PRESSURE_PLATE.getDefaultState();
		// floor
		this.fillBlocksRotated(world, sbb, 4, y, 3, 7, y, 5, inactiveReappearing, rotation);
		// plates
		this.fillBlocksRotated(world, sbb, 4, y + 1, 2, 7, y + 1, 2, woodenPressurePlate, rotation);
		this.fillBlocksRotated(world, sbb, 4, y + 1, 6, 7, y + 1, 6, woodenPressurePlate, rotation);
	}

	/**
	 * Decorate with a redstone device that turns a lamp on or off
	 */
	private void decorateExperimentLamp(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {

		int cx = this.size > 9 ? 5 : 3;
		int cz = this.size > 9 ? 5 : 4;

		final BlockState redstoneLamp = Blocks.REDSTONE_LAMP.getDefaultState();

		setBlockStateRotated(world, Blocks.STICKY_PISTON.getDefaultState().with(DirectionalBlock.FACING, Direction.UP), cx, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, redstoneLamp, cx, y + 2, cz, rotation, sbb);
		setBlockStateRotated(world, deco.accentState, cx, y + 1, cz + 1, rotation, sbb);
		setBlockStateRotated(world, getLeverState(Blocks.LEVER.getDefaultState(), AttachFace.WALL, Direction.NORTH, false), cx, y + 1, cz + 2, rotation, sbb);
		setBlockStateRotated(world, deco.accentState, cx, y + 3, cz - 1, rotation, sbb);
		setBlockStateRotated(world, getLeverState(Blocks.LEVER.getDefaultState(), AttachFace.WALL, Direction.SOUTH, true), cx, y + 3, cz - 2, rotation, sbb);
	}

	protected static BlockState getLeverState(BlockState initialState, AttachFace face, Direction direction, boolean isPowered) {
		switch (direction) {
			case NORTH:
			case SOUTH:
			case EAST:
			case WEST:
				//All Horizontal facings are as they should
				break;
			case UP:
			case DOWN:
			default:
				//Levers cannot face Up or Down, as it is a Horizontal Face
				direction = Direction.NORTH;
		}
		return initialState.with(HorizontalBlock.HORIZONTAL_FACING, direction)
				.with(HorizontalFaceBlock.FACE, face)
				.with(LeverBlock.POWERED, isPowered);
	}

	/**
	 * Decorate with a redstone device that pulses a block back and forth
	 */
	private void decorateExperimentPulser(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {

		int cx = this.size > 9 ? 6 : 5;
		int cz = this.size > 9 ? 4 : 3;

		BlockState redstoneWire = Blocks.REDSTONE_WIRE.getDefaultState();
		BlockState woodenPressurePlate = Blocks.OAK_PRESSURE_PLATE.getDefaultState();
		BlockState stickyPiston = Blocks.STICKY_PISTON.getDefaultState().with(DirectionalBlock.FACING, Direction.SOUTH);
		BlockState unpoweredRepeater = Blocks.REPEATER.getDefaultState().with(RedstoneDiodeBlock.POWERED, false).with(HorizontalBlock.HORIZONTAL_FACING, Direction.WEST).with(RepeaterBlock.DELAY, 2);

		setBlockStateRotated(world, stickyPiston, cx, y + 1, cz + 1, rotation, sbb);
		setBlockStateRotated(world, deco.accentState, cx, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, redstoneWire, cx + 1, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, woodenPressurePlate, cx + 2, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, unpoweredRepeater, cx - 1, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, redstoneWire, cx - 2, y + 1, cz, rotation, sbb);
		setBlockStateRotated(world, redstoneWire, cx - 2, y + 1, cz + 1, rotation, sbb);
		setBlockStateRotated(world, redstoneWire, cx - 1, y + 1, cz + 1, rotation, sbb);
	}

	/**
	 * Decorate with some bookshelves
	 */
	private void decorateLibrary(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		int bx = this.size > 9 ? 4 : 3;
		int bz = this.size > 9 ? 3 : 2;

		makeSmallBookshelf(world, sbb, rotation, y, bx, bz);

		bx = this.size > 9 ? 9 : 7;
		bz = this.size > 9 ? 3 : 2;
		makeSmallBookshelf(world, sbb, rotation, y, bx, bz);
	}

	protected void makeSmallBookshelf(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y, int bx, int bz) {
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.NORTH, false), bx, y + 1, bz, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.NORTH, true), bx, y + 2, bz, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.SOUTH, false), bx, y + 1, bz + 3, rotation, sbb);
		setBlockStateRotated(world, getStairState(deco.stairState, Direction.SOUTH, true), bx, y + 2, bz + 3, rotation, sbb);
		final BlockState bookshelf = Blocks.BOOKSHELF.getDefaultState();
		setBlockStateRotated(world, bookshelf, bx, y + 1, bz + 1, rotation, sbb);
		setBlockStateRotated(world, bookshelf, bx, y + 2, bz + 1, rotation, sbb);
		setBlockStateRotated(world, bookshelf, bx, y + 1, bz + 2, rotation, sbb);
		setBlockStateRotated(world, bookshelf, bx, y + 2, bz + 2, rotation, sbb);
	}

	/**
	 * A chest with an extremely simple puzzle
	 */
	private void decoratePuzzleChest(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		int x = this.size > 9 ? 4 : 3;
		int z = this.size > 9 ? 5 : 4;
		// pillar frameframe
		this.makePillarFrame(world, sbb, this.deco, rotation, x, y, z, true);

		// reinforce with towerwood
		setBlockStateRotated(world, deco.platformState, x + 1, y + 1, z + 1, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x + 2, y + 1, z + 1, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x, y + 1, z + 1, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x + 1, y + 1, z + 2, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x + 1, y + 1, z, rotation, sbb);

		setBlockStateRotated(world, deco.blockState, x + 2, y + 3, z + 1, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x, y + 3, z + 1, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x + 1, y + 3, z + 2, rotation, sbb);
		setBlockStateRotated(world, AIR, x + 1, y + 3, z, rotation, sbb);
		setBlockStateRotated(world, deco.blockState, x + 1, y + 3, z + 1, rotation, sbb);
		setBlockStateRotated(world, Blocks.STICKY_PISTON.getDefaultState().with(DirectionalBlock.FACING, Direction.NORTH), x + 1, y + 3, z - 1, rotation, sbb);
		setBlockStateRotated(world, deco.accentState, x + 1, y + 3, z - 2, rotation, sbb);
		setBlockStateRotated(world, getLeverState(Blocks.LEVER.getDefaultState(), AttachFace.WALL, Direction.WEST, false), x + 2, y + 3, z - 2, rotation, sbb);

		placeTreasureRotated(world, x + 1, y + 2, z + 1, getCoordBaseMode(), rotation, TFTreasure.darktower_cache, sbb);
	}

	/**
	 * Make a 3x3x3 pillar frame
	 */
	protected void makePillarFrame(ISeedReader world, MutableBoundingBox sbb, StructureTFDecorator myDeco, Rotation rotation, int x, int y, int z, boolean fenced) {
		makePillarFrame(world, sbb, myDeco, rotation, x, y, z, 3, 3, 3, fenced);
	}

	/**
	 * Place one of the architectural features that I frequently overuse in my structures
	 */
	protected void makePillarFrame(ISeedReader world, MutableBoundingBox sbb, StructureTFDecorator myDeco, Rotation rotation, int x, int y, int z, int width, int height, int length, boolean fenced) {
		// fill in posts
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < length; dz++) {
				if ((dx % 3 == 0 || dx == width - 1) && (dz % 3 == 0 || dz == length - 1)) {
					for (int py = 1; py <= height; py++) {
						setBlockStateRotated(world, myDeco.pillarState, x + dx, y + py, z + dz, rotation, sbb);
					}
				} else {
					if (dx == 0) {
						final BlockState southStairs = getStairState(deco.stairState, Direction.WEST, false);
						setBlockStateRotated(world, southStairs, x + dx, y + 1, z + dz, rotation, sbb);
						setBlockStateRotated(world, southStairs.with(StairsBlock.HALF, Half.TOP), x + dx, y + height, z + dz, rotation, sbb);
					} else if (dx == width - 1) {
						final BlockState northStairs = getStairState(deco.stairState, Direction.EAST, false);
						setBlockStateRotated(world, northStairs, x + dx, y + 1, z + dz, rotation, sbb);
						setBlockStateRotated(world, northStairs.with(StairsBlock.HALF, Half.TOP), x + dx, y + height, z + dz, rotation, sbb);
					} else if (dz == 0) {
						final BlockState westStairs = getStairState(deco.stairState, Direction.NORTH, false);
						setBlockStateRotated(world, westStairs, x + dx, y + 1, z + dz, rotation, sbb);
						setBlockStateRotated(world, westStairs.with(StairsBlock.HALF, Half.TOP), x + dx, y + height, z + dz, rotation, sbb);
					} else if (dz == length - 1) {
						final BlockState eastStairs = getStairState(deco.stairState, Direction.SOUTH, false);
						setBlockStateRotated(world, eastStairs, x + dx, y + 1, z + dz, rotation, sbb);
						setBlockStateRotated(world, eastStairs.with(StairsBlock.HALF, Half.TOP), x + dx, y + height, z + dz, rotation, sbb);
					}

					if (fenced && (dx == 0 || dx == width - 1 || dz == 0 || dz == length - 1)) {
						for (int fy = 2; fy <= height - 1; fy++) {
							setBlockStateRotated(world, myDeco.fenceState, x + dx, y + fy, z + dz, rotation, sbb);
						}
					}
				}
			}
		}
	}

	/**
	 * Dark tower half floors
	 */
	protected void addStairsDown(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y, int sz, int spacing) {
		// stairs
		for (int i = 0; i < spacing; i++) {
			int sx = size - 3 - i;

			this.setBlockStateRotated(world, getStairState(deco.stairState, Direction.WEST, false), sx, y - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, deco.accentState, sx, y - 1 - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, AIR, sx, y + 1 - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, AIR, sx, y + 2 - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, AIR, sx - 1, y + 2 - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, AIR, sx, y + 3 - i, sz, rotation, sbb);
			this.setBlockStateRotated(world, AIR, sx - 1, y + 3 - i, sz, rotation, sbb);
		}
	}

	/**
	 * Add a bunch of timber beams
	 */
	protected void addSmallTimberBeams(ISeedReader world, Random rand, MutableBoundingBox sbb, int bottom, int top) {

		int spacing = 4;
		Rotation rotation = Rotation.NONE;
		if (bottom == 0) {
			bottom += spacing;
		}

		// fill with 3/4 floors
		for (int y = bottom; y < top; y += spacing) {
			rotation = rotation.add(Rotation.CLOCKWISE_90);

			if (y >= top - spacing && isDeadEnd()) {
				makeTimberFloor(world, sbb, rotation, y);

				StructureTFDecorator logDeco = new StructureDecoratorDarkTower();

				logDeco.pillarState = TFBlocks.dark_log.get().getDefaultState();
				logDeco.platformState = TFBlocks.dark_log.get().getDefaultState();

				decorateTreasureRoom(world, sbb, rotation, y, 4, logDeco);
			} else {
				makeSmallTimberBeams(world, rand, sbb, rotation, y, y == bottom && bottom != spacing);
			}
		}
	}

	/**
	 * Make a mostly soid timber floor
	 */
	protected void makeTimberFloor(ISeedReader world, MutableBoundingBox sbb, Rotation rotation, int y) {
		BlockState beamID = TFBlocks.dark_log.get().getDefaultState();
		BlockState beamStateNS = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		BlockState beamStateUD = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState beamStateEW = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.X);

		for (int z = 1; z < size - 1; z++) {
			for (int x = 1; x < size - 1; x++) {
				if (x < z) {
					setBlockStateRotated(world, beamStateNS, x, y, z, rotation, sbb);
				} else {
					setBlockStateRotated(world, beamStateEW, x, y, z, rotation, sbb);
				}
			}
		}

		// beams going down
		for (int by = 1; by < 4; by++) {
			BlockState ladder = Blocks.LADDER.getDefaultState();
			setBlockStateRotated(world, beamStateUD, 2, y - by, 2, rotation, sbb);
			setBlockStateRotated(world, ladder.with(LadderBlock.FACING, Direction.WEST), 2 + 1, y - by, 2, rotation, sbb);
			setBlockStateRotated(world, beamStateUD, 6, y - by, 6, rotation, sbb);
			setBlockStateRotated(world, ladder.with(LadderBlock.FACING, Direction.EAST), 6 - 1, y - by, 6, rotation, sbb);
		}

		// holes for entrance
		setBlockStateRotated(world, AIR, 3, y, 2, rotation, sbb);
		setBlockStateRotated(world, AIR, 5, y, 6, rotation, sbb);
	}

	/**
	 * Make a lattice of log blocks
	 */
	protected void makeSmallTimberBeams(ISeedReader world, Random rand, MutableBoundingBox sbb, Rotation rotation, int y, boolean bottom) {
		BlockState beamID = TFBlocks.dark_log.get().getDefaultState();
		BlockState beamStateNS = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.X);
		BlockState beamStateUD = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState beamStateEW = beamID.with(RotatedPillarBlock.AXIS, Direction.Axis.Z);

		// two beams going e/w
		for (int z = 1; z < size - 1; z++) {
			setBlockStateRotated(world, beamStateEW, 2, y, z, rotation, sbb);
			setBlockStateRotated(world, beamStateEW, 6, y, z, rotation, sbb);
		}

		// a few random cross beams
		int z = pickBetweenExcluding(3, size - 3, rand, 2, 2, 6);
		for (int x = 3; x < 6; x++) {
			setBlockStateRotated(world, beamStateNS, x, y, z, rotation, sbb);
		}

		// beams going down
		int x1 = 2;
		int z1 = rand.nextBoolean() ? 2 : 6;
		int x3 = 6;
		int z3 = rand.nextBoolean() ? 2 : 6;

		for (int by = 1; by < 4; by++) {
			final BlockState ladder = Blocks.LADDER.getDefaultState();
			if (!bottom || checkPost(world, x1, y - 4, z1, rotation, sbb)) {
				setBlockStateRotated(world, beamStateUD, x1, y - by, z1, rotation, sbb);
				setBlockStateRotated(world, ladder.with(LadderBlock.FACING, Direction.WEST), x1 + 1, y - by, z1, rotation, sbb);
			}
			if (!bottom || checkPost(world, x3, y - 4, z3, rotation, sbb)) {
				setBlockStateRotated(world, beamStateUD, x3, y - by, z3, rotation, sbb);
				setBlockStateRotated(world, ladder.with(LadderBlock.FACING, Direction.EAST), x3 - 1, y - by, z3, rotation, sbb);
			}
		}
	}

	/**
	 * Utility function to pick a random number between two values, excluding three specified values
	 */
	protected int pickBetweenExcluding(int low, int high, Random rand, int k, int l, int m) {
		int result;

		do {
			result = rand.nextInt(high - low) + low;
		}
		while (result == k || result == l || result == m);

		return result;
	}

	/**
	 * Pick one of the three specified values at random
	 */
	protected int pickFrom(Random rand, int i, int j, int k) {
		switch (rand.nextInt(3)) {
			case 0:
			default:
				return i;
			case 1:
				return j;
			case 2:
				return k;
		}
	}

	/**
	 * Utility function for beam maze that checks if we should build a beam all the way down -- is there a valid spot to end it?
	 */
	protected boolean checkPost(ISeedReader world, int x, int y, int z, Rotation rotation, MutableBoundingBox sbb) {
		int worldX = this.getXWithOffsetRotated(x, z, rotation);
		int worldY = this.getYWithOffset(y);
		int worldZ = this.getZWithOffsetRotated(x, z, rotation);
		final BlockPos vec = new BlockPos(worldX, worldY, worldZ);
		if (!sbb.isVecInside(vec)) return false;
		BlockState blockState = world.getBlockState(vec);
		return blockState.getBlock() != Blocks.AIR && blockState != deco.accentState;
	}

	/**
	 * Generate walls for the tower with the distinct pattern of blocks and accent blocks
	 *
	 * @param rand
	 */
	protected void makeEncasedWalls(ISeedReader world, Random rand, MutableBoundingBox sbb, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ) {
						;
					} else {
						// wall
						if (((y == minY || y == maxY) && ((x == minY || x == maxX) || (z == minZ || z == maxZ)))
								|| ((z == minZ || z == maxZ) && ((x == minY || x == maxX) || (y == minY || y == maxY)))) {
							this.setBlockState(world, deco.accentState, x, y, z, sbb);
						} else {
							StructurePiece.BlockSelector blocker = deco.randomBlocks;

							blocker.selectBlocks(rand, x, y, z, true);
							this.setBlockState(world, blocker.getBlockState(), x, y, z, sbb);
						}
					}
				}
			}
		}

		// corners
		this.setBlockState(world, deco.accentState, minX + 1, minY + 1, minZ, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, minY + 1, maxZ, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, minY + 1, minZ, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, minY + 1, maxZ, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, maxY - 1, minZ, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, maxY - 1, maxZ, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, maxY - 1, minZ, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, maxY - 1, maxZ, sbb);

		this.setBlockState(world, deco.accentState, minX, minY + 1, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, minX, minY + 1, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, maxX, minY + 1, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, maxX, minY + 1, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, minX, maxY - 1, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, minX, maxY - 1, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, maxX, maxY - 1, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, maxX, maxY - 1, maxZ - 1, sbb);

		this.setBlockState(world, deco.accentState, minX + 1, minY, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, minY, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, minY, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, minY, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, maxY, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, minX + 1, maxY, maxZ - 1, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, maxY, minZ + 1, sbb);
		this.setBlockState(world, deco.accentState, maxX - 1, maxY, maxZ - 1, sbb);
	}

	/**
	 * Gets a random position in the specified direction that connects to a floor currently in the tower.
	 */
	@Override
	public int[] getValidOpening(Random rand, Rotation direction) {
		int verticalOffset = this.size == 19 ? 5 : 4;

		// for directions 0 or 2, the wall lies along the z axis
		if (direction == Rotation.NONE || direction == Rotation.CLOCKWISE_180) {
			int rx = direction == Rotation.NONE ? size - 1 : 0;
			int rz = this.size / 2;
			int ry = this.height - verticalOffset;

			return new int[]{rx, ry, rz};
		}

		// for directions 1 or 3, the wall lies along the x axis
		if (direction == Rotation.CLOCKWISE_90 || direction == Rotation.COUNTERCLOCKWISE_90) {
			int rx = this.size / 2;
			int rz = direction == Rotation.CLOCKWISE_90 ? size - 1 : 0;
			int ry = this.height - verticalOffset;

			return new int[]{rx, ry, rz};
		}

		return new int[]{0, 0, 0};
	}

	/**
	 * Add an opening to the outside (or another tower) in the specified direction.
	 */
	@Override
	public void addOpening(int dx, int dy, int dz, Rotation direction) {
		this.addOpening(dx, dy, dz, direction, EnumDarkTowerDoor.VANISHING);
	}

	/**
	 * Add an opening where we keep track of the kind of opening
	 * TODO: we could make a type of object that stores all these values
	 * TODO: also use an Enum for the kinds of openings?
	 */
	protected void addOpening(int dx, int dy, int dz, Rotation direction, EnumDarkTowerDoor type) {
		super.addOpening(dx, dy, dz, direction);
		this.openingTypes.add(openings.indexOf(new BlockPos(dx, dy, dz)), type);
	}

	/**
	 * Iterate through the openings on our list and add them to the tower
	 */
	@Override
	protected void makeOpenings(ISeedReader world, MutableBoundingBox sbb) {
		for (int i = 0; i < openings.size(); i++) {
			BlockPos doorCoords = openings.get(i);

			EnumDarkTowerDoor doorType;
			if (openingTypes.size() > i) {
				doorType = openingTypes.get(i);
			} else {
				doorType = EnumDarkTowerDoor.VANISHING;
			}

			switch (doorType) {
				case VANISHING:
				default:
					makeDoorOpening(world, doorCoords.getX(), doorCoords.getY(), doorCoords.getZ(), sbb);
					break;
				case REAPPEARING:
					makeReappearingDoorOpening(world, doorCoords.getX(), doorCoords.getY(), doorCoords.getZ(), sbb);
					break;
				case LOCKED:
					makeLockedDoorOpening(world, doorCoords.getX(), doorCoords.getY(), doorCoords.getZ(), sbb);
					break;
			}
		}
	}

	/**
	 * Make an opening in this tower for a door.
	 */
	@Override
	protected void makeDoorOpening(ISeedReader world, int dx, int dy, int dz, MutableBoundingBox sbb) {
		// nullify sky light
//		nullifySkyLightAtCurrentPosition(world, dx - 3, dy - 1, dz - 3, dx + 3, dy + 3, dz + 3);

		final BlockState inactiveVanish = TFBlocks.vanishing_block.get().getDefaultState();

		// clear the door
		if (dx == 0 || dx == size - 1) {
			this.fillWithBlocks(world, sbb, dx, dy - 1, dz - 2, dx, dy + 3, dz + 2, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx, dy, dz - 1, dx, dy + 2, dz + 1, inactiveVanish, AIR, false);
		}
		if (dz == 0 || dz == size - 1) {
			this.fillWithBlocks(world, sbb, dx - 2, dy - 1, dz, dx + 2, dy + 3, dz, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx - 1, dy, dz, dx + 1, dy + 2, dz, inactiveVanish, AIR, false);
		}
	}

	/**
	 * Make a 3x3 tower door that reappears
	 */
	protected void makeReappearingDoorOpening(ISeedReader world, int dx, int dy, int dz, MutableBoundingBox sbb) {
		// nullify sky light
//		nullifySkyLightAtCurrentPosition(world, dx - 3, dy - 1, dz - 3, dx + 3, dy + 3, dz + 3);

		final BlockState inactiveReappearing = TFBlocks.reappearing_block.get().getDefaultState();

		// clear the door
		if (dx == 0 || dx == size - 1) {
			this.fillWithBlocks(world, sbb, dx, dy - 1, dz - 2, dx, dy + 3, dz + 2, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx, dy, dz - 1, dx, dy + 2, dz + 1, inactiveReappearing, AIR, false);
		}
		if (dz == 0 || dz == size - 1) {
			this.fillWithBlocks(world, sbb, dx - 2, dy - 1, dz, dx + 2, dy + 3, dz, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx - 1, dy, dz, dx + 1, dy + 2, dz, inactiveReappearing, AIR, false);
		}
	}

	/**
	 * Make a 3x3 tower door that is locked
	 */
	protected void makeLockedDoorOpening(ISeedReader world, int dx, int dy, int dz, MutableBoundingBox sbb) {
		// nullify sky light
//		nullifySkyLightAtCurrentPosition(world, dx - 3, dy - 1, dz - 3, dx + 3, dy + 3, dz + 3);

		// clear the door
		final BlockState lockedVanish = TFBlocks.locked_vanishing_block.get().getDefaultState();
		final BlockState inactiveVanish = TFBlocks.vanishing_block.get().getDefaultState();

		if (dx == 0 || dx == size - 1) {
			this.fillWithBlocks(world, sbb, dx, dy - 1, dz - 2, dx, dy + 3, dz + 2, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx, dy, dz - 1, dx, dy + 2, dz + 1, inactiveVanish, AIR, false);
			this.setBlockState(world, lockedVanish, dx, dy, dz + 1, sbb);
			this.setBlockState(world, lockedVanish, dx, dy, dz - 1, sbb);
			this.setBlockState(world, lockedVanish, dx, dy + 2, dz + 1, sbb);
			this.setBlockState(world, lockedVanish, dx, dy + 2, dz - 1, sbb);
		}
		if (dz == 0 || dz == size - 1) {
			this.fillWithBlocks(world, sbb, dx - 2, dy - 1, dz, dx + 2, dy + 3, dz, deco.accentState, AIR, false);
			this.fillWithBlocks(world, sbb, dx - 1, dy, dz, dx + 1, dy + 2, dz, inactiveVanish, AIR, false);
			this.setBlockState(world, lockedVanish, dx + 1, dy, dz, sbb);
			this.setBlockState(world, lockedVanish, dx - 1, dy, dz, sbb);
			this.setBlockState(world, lockedVanish, dx + 1, dy + 2, dz, sbb);
			this.setBlockState(world, lockedVanish, dx - 1, dy + 2, dz, sbb);
		}
	}

	/**
	 * Returns true if this tower has only one exit.
	 * <p>
	 * TODO: is this really the best way?
	 */
	@Override
	public boolean isDeadEnd() {
		// we have to modify this to ignore door type 2 since that leads to balconies
		int nonBalconies = 0;

		for (EnumDarkTowerDoor type : openingTypes) {
			if (type != EnumDarkTowerDoor.REAPPEARING) {
				nonBalconies++;
			}
		}

		return nonBalconies <= 1;
	}

	public boolean isKeyTower() {
		return keyTower;
	}

	public void setKeyTower(boolean keyTower) {
		this.keyTower = keyTower;
	}
}
