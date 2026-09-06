# Data formats, ValueIO, and what a 1.21.1 world needs to load

This is a findings document, not a design. Nothing in it has been acted on. It exists so that whoever
picks up "make old worlds load" or "get rid of the NBT bridge" does not have to rediscover any of
this — every claim here was checked against the code on disk, and the file paths and shapes are
quoted rather than remembered.

Two questions prompted it: *does the port's read-from-raw-NBT shortcut want to be a datafixer?*, and
*does Create have an upgrade path for old worlds already?* The short answers are **no, mostly** and
**yes, two of them**. The rest of this file is why.

---

## 1. There are two problems here, not one

They are constantly mistaken for each other because both look like "Create reads tags in a ValueIO
world". Only the second one loses data.

### Problem A — the envelope bridge. Cosmetic; no data at stake.

26.2 replaced `CompoundTag` block-entity serialization with `ValueInput`/`ValueOutput`. Create has
~126 hand-written `read(CompoundTag…)`/`write(CompoundTag…)` bodies, and the tag helpers they use are
shared with packets and contraption storage, which still speak tags. Rather than rewrite all of them
during the port, one bridge was added at the boundary.

`foundation/blockEntity/SyncedBlockEntity.java`:

```java
@Override protected final void saveAdditional(ValueOutput output) {
    CompoundTag tag = new CompoundTag();
    saveAdditional(tag, registries);           // Create's own hook, re-declared in this class
    NbtValueIO.store(output, tag);
}
@Override protected final void loadAdditional(ValueInput input) {
    loadAdditional(NbtValueIO.read(input), input.lookup());
}
```

The two `protected void …Additional(CompoundTag, HolderLookup.Provider)` methods are re-declarations
of the *1.21.1 vanilla* signatures as Create-private methods. Because they exist, every subclass's
`@Override` still compiles and still overrides something.

Facts worth knowing:

- **This is port-era, not upstream.** `foundation/utility/NbtValueIO.java` does not exist on
  `mc1.21.1/dev`. It came in with commit `6863f410d` *"Move entity save data onto ValueIO"*, whose
  message states the reason plainly: *"rather than rewriting every body"*. On 1.21.1 there were no
  such overrides at all — subclasses overrode vanilla's `CompoundTag` hooks directly.
- **126 files** still declare `read(CompoundTag…)`/`write(CompoundTag…)`. **Zero** block entities
  override the real 26.2 `loadAdditional(ValueInput)`.
- The same per-class bridge is applied to seven entities: `AbstractContraptionEntity`,
  `SuperGlueEntity`, `PackageEntity`, `BlueprintEntity`, `PotatoProjectileEntity`,
  `MinecartController`, `SeatEntity`.
- **The stored bytes are identical either way.** `NbtValueIO.store` goes through NeoForge's
  `ValueOutput#store(CompoundTag)`, which splats the tag's entries into the root — the same keys at
  the same level ValueIO would have written. Removing the bridge is a readability change, not a
  compatibility one.

Two things in here are simply wrong and worth fixing whatever else is decided:

- **`SeatEntity` is vestigial.** It bridges a tag it never reads from or writes to:
  ```java
  @Override protected void readAdditionalSaveData(ValueInput input)  { CompoundTag tag = NbtValueIO.read(input); }
  @Override protected void addAdditionalSaveData(ValueOutput output) { CompoundTag tag = new CompoundTag(); NbtValueIO.store(output, tag); }
  ```
- **The registry lookup is asymmetric.** Saving falls back to `RegistryAccess.EMPTY` when
  `level == null`, while loading uses `input.lookup()`; `NbtValueIO.toTag` is hard-wired to `EMPTY`.
  Anything whose serialization needs registries will round-trip differently depending on which path
  it took.

### Problem B — the nested handler formats. Real, silent data loss.

NeoForge's transfer-API rewrite changed how *storage handlers* serialize. This is nothing to do with
Create's bridge, and NeoForge ships no migration for it — which means it affects every mod that used
`ItemStackHandler`, not only Create.

| | 1.21.1 | 26.2 |
| --- | --- | --- |
| inventories | `ItemStackHandler` | `ItemStacksResourceHandler` (extends `StacksResourceHandler`) |
| fluid tanks | `FluidTank` | `FluidStacksResourceHandler` |

The inventory shapes, verbatim from both versions:

```java
// 1.21.1 — net.neoforged.neoforge.items.ItemStackHandler#serializeNBT
//   sparse: empty slots omitted, each entry carries its own index
{ Items: [ { Slot: 0, id: "...", count: n, ... }, { Slot: 3, ... } ], Size: 9 }

// 26.2 — net.neoforged.neoforge.transfer.StacksResourceHandler#serialize
//   dense: every slot present in order, empty ones as empty stacks, no indices, no size
public static final String VALUE_IO_KEY = "stacks";
output.store(VALUE_IO_KEY, codec, stacks);      // codec is a list of ItemStack.OPTIONAL_CODEC
{ stacks: [ <stack>, <stack>, ... ] }
```

So a migration is not a rename — it is **sparse-with-indices to dense-positional**, and it needs the
handler's size to place the entries. `Size` is present in the old shape, which is what makes it
possible.

Three more of the same kind:

- **fluid tanks** — `{ Fluid: {…}, Amount: n }` became the ValueIO form.
- **enchantments** — an item written before 26.2 keeps its levels one step further in, under
  `levels`. Read now, the item comes back *unenchanted rather than unread*: a deployer holding a
  pickaxe of Efficiency V swings it like a plain one.
- **`OpenEndedPipe`** — the end's direction was `Location { Pos, Face }`; the codec spells those
  `pos` and `direction`. Reading an old one does not fail loudly, it leaves the pipe aimed upward, so
  a pipe that was drawing from the water beside it comes back sucking at the air.

Every one of these is silent. Nothing throws; things simply come back empty, unenchanted, or pointing
the wrong way.

---

## 2. Why the read-time fallbacks exist at all

Four stopgaps currently paper over Problem B. They are tagged `GAMETEST FIX`:

| file | line | covers |
| --- | --- | --- |
| `foundation/utility/NbtValueIO.java` | 100 | inventories — old `Items`/`Slot` list |
| `foundation/utility/NbtValueIO.java` | 186 | enchantments — `unwrapLegacyEnchantments` |
| `foundation/fluid/SmartFluidTank.java` | 98 | tanks — old `Fluid` tag |
| `content/fluids/OpenEndedPipe.java` | 110 | the open end's direction |

They exist because of the **gametests**, and this is the part that is easy to miss: Create's 67
gametest structure files under `src/main/resources/data/create/structure/gametest/` are 1.21.1-era
binary NBT and were **never regenerated** during the port — `git diff mc1.21.1/dev...HEAD` on that
directory is empty. Decompressed, they still carry the old keys:

```
arrow_dispenser.nbt      [Items, Slot]
mounted_fluid_drain.nbt  [Fluid]
roller_filling.nbt       [Items, Slot]
```

A gametest that places one of those got an empty inventory and an empty tank, silently, and failed.

**This means the gametest suite is already exercising a 1.21.1 → 26.2 data migration on every run.**
That is the cheapest regression harness available for any of this work, and it is already green.

---

## 3. What Create already does about upgrades

Create has done this before, twice, with two different mechanisms — and it picks between them by
**what is carrying the data**, not by preference.

### Mechanism 1 — datafixers, where DFU already walks the data

Only for chunks and items, and never by registering a mod fixer. Create *mixes into vanilla's own fix
classes* and appends its entries:

- `foundation/mixin/datafixer/ItemStackComponentizationFixMixin` —
  `@Inject(method = "fixItemStack", at = @At("TAIL"))`, moving 0.5.1-era item tags
  (`SequencedAssembly`, backtank `Air`, `FirstPulley`, worldshaper settings, clipboard, filter mode)
  into data components
- `foundation/mixin/datafixer/BlockPosFormatAndRenamesFixMixin` and `V1460Mixin`
- `foundation/utility/DataFixerHelper` — a table of `BlockPosFixer` records against
  `References.BLOCK_ENTITY` and `References.ENTITY`, covering `Source`, `Controller`, `LastKnownPos`,
  `TargetOffset`, `Breaking`, `MirrorChildren`, `EnginePos`, and others

All of it targets **1.20.1 → 1.21.1**. All of it is byte-identical on both branches — nothing was
added for 26.2 — and all three mixin targets still exist in 26.2, so it still works for what it covers.

### Mechanism 2 — read-time, shape-gated handling, for Create's own format changes

Everywhere DFU cannot reach. None of these is version-gated: they are unconditional, they look for the
old shape, and they no-op when it is absent.

- `content/contraptions/MountedStorageManager.java:445` — `readLegacy` reads an old `"Storage"` list
  beside the new one and dispatches on marker keys (`Toolbox`, `NoFuel`, `Bottomless`, `Synced`) into
  `fromLegacy` constructors
- `content/contraptions/Contraption.java` — picks `legacyReadStructureBlockInfo` over the palletted
  reader, and keeps an `isLegacy` map per block position from a `"Legacy"` key
- `AllDisplaySources.LEGACY_NAMES`, `AllDisplayTargets.LEGACY_NAMES`,
  `AllContraptionTypes.BY_LEGACY_NAME` — old registry-name aliases

### What that implies for the four stopgaps

**They are already the house style.** They are Mechanism 2, applied to a format change that happened
to come from NeoForge rather than from Create. Their own comments — *"a stopgap: what is written this
way wants a datafixer, not a read-time fallback"* — are harsher than the codebase's own practice
warrants.

What they actually lack is **coverage, not a different mechanism**. The enchantment unwrap runs only
where an inventory is read back, so a schematic, or a block entity holding an item of its own, still
loses what it was enchanted with. That is the honest bug, and it is a smaller one than "we need a
datafixer".

---

## 4. What is possible on NeoForge 26.2

Checked directly against `neoforge-26.2.0.66` (and `.69`) sources and userdev jars, so nobody has to
search for this again.

**There is no mod-facing datafixer API. At all.**

- No `RegisterDataFixersEvent` or equivalent. Enumerating every `Register*Event` in the NeoForge
  sources yields 50 events; none is datafixer-related.
- `"DataFixer"` appears in exactly one NeoForge source file (`FakePlayer`, a constructor
  pass-through). `"DataFixTypes"` appears in none.
- `DataFixTypes` is a plain `enum` with no `IExtensibleEnum` and no NeoForge patch — a mod cannot add
  a constant.
- `DataFixers.addFixers(DataFixerBuilder, FileFixerUpper.Builder)` is `private static`, and
  `DATA_FIXER` is built in a **static initialiser** — before any mod is constructed. There is no seam.
- NeoForge registers its *own* fixers by hard-patching that method
  (`patches/net/minecraft/util/datafix/DataFixers.java.patch`), claiming schema numbers in vanilla's
  gaps: 3801, 3804, 3815. All attribute renames, all pre-1.21.1. **NeoForge ships nothing for its own
  transfer-API storage change.**

**NeoForge's stated position is that mods should version their own data.**
`SavedDataType.dataFixType` was made `@Nullable` specifically for this, with the comment:

> `// Neo: We do not have update logic compatible with DFU, several downstream patches from this record are made to support a nullable dataFixType.`

Create's two `SavedData` subclasses — `RailwaySavedData` (`create:create_tracks`) and
`LogisticsNetworkSavedData` (`create:create_logistics`) — both use the 3-argument constructor, i.e.
no `DataFixTypes`. `SavedDataStorage.readTagFromDisk` skips DFU entirely when it is null.

**Three further reasons DFU fits this badly**, even by mixin:

1. **`DataFixTypes` has no `BLOCK_ENTITY` constant.** Block-entity NBT is fixed *only* as part of
   `DataFixTypes.CHUNK`, inside `SimpleRegionStorage.upgradeChunkTag`. `References.BLOCK_ENTITY`
   exists, but only as a sub-type within the `CHUNK` / `ITEM_STACK` / `DATA_COMPONENTS` type graph.
   Individual block-entity tags carry no `DataVersion` of their own — nothing in vanilla writes one.
2. **Chunks are not the only carrier.** The same old-shaped data lives in gametest structures
   (`DataFixTypes.STRUCTURE`), in schematics, and inside contraption storage — which is a tag nested
   *within* a block entity, not a chunk. One chunk-level fixer reaches none of those.
3. **The ordering constraint.** DFU requires schemas in ascending version order. Vanilla's last in
   26.2 is **4892**; 1.21.1's data version was **3955**. A `@Inject` at `TAIL` of `addFixers` can only
   append *above* 4892, which means the fix runs on every load of every world including 26.2-native
   ones — so it must be shape-conditional and idempotent anyway. Inserting between existing schemas
   needs a mid-method injection point and is brittle across NeoForge updates.

Point 3 is worth dwelling on: once a fix has to be shape-conditional and idempotent, it is doing
exactly what a read-time fallback does, with a mixin and a schema number wrapped around it.

---

## 5. If someone implements this

### The likely shape

Widening Mechanism 2 is the option most consistent with the codebase and the only one that reaches
every carrier. Concretely that means, for each of the four formats, a single shared "read the old
shape if it is there" helper called from **every** read site rather than one — the enchantment case
is the one currently applied in a single place and needed in several.

A datafixer by mixin remains defensible for the *chunk* path specifically, and matches what upstream
already does for `BlockPos` renames. It is not a substitute for the fallbacks; it would be an
optimisation on top of them, so that a world is fixed once on load rather than on every read.

### Checklist of what must be covered

- [ ] inventories — sparse `{Items:[{Slot}], Size}` → dense `{stacks:[…]}`; needs `Size` to place them
- [ ] fluid tanks — `{Fluid, Amount}` → ValueIO form
- [ ] enchantments — nested `levels` → flattened; **currently only handled where an inventory is read
      back**, so schematics and block entities holding their own items still lose it
- [ ] `OpenEndedPipe` — `Location{Pos, Face}` → `{pos, direction}`; fails to *upward*, not to unset
- [ ] carriers: world chunks, gametest structures, schematics, contraption storage
- [ ] `SeatEntity`'s vestigial bridge (unrelated, but delete it while you are here)
- [ ] the `RegistryAccess.EMPTY` / `input.lookup()` asymmetry in `SyncedBlockEntity` / `NbtValueIO`

### How to verify it

Three harnesses already exist, in increasing order of realism.

1. **The gametests.** They already place 1.21.1-era structures, so they already test this path. If a
   fallback is removed or a fixer is wrong, they fail. Cheapest signal available.
2. **A 1.21.1 world, produced rather than forged.** There is a full, buildable Create 1.21.1 checkout
   at `../Create-1.21.1` — `mod_version 6.0.11`, the same version as this port — with a `run/world`
   of 283 region files. Build it, place the machines worth testing, save, then load that world here.
   (The existing world has no `create_tracks.dat`, so it contains no railways yet.)
3. **The e2e harness.** `Create-e2e`'s `WorldReloadTest` already proves 26.2 → 26.2: it builds a
   railway, a train with a schedule and a driven millstone, saves, stops the server, starts it again
   and asserts everything came back. The driver's `ClusterScope.startServer(name)` gives a test its
   own server on its own port and game directory; it wipes `world` on first start, so an optional
   "copy this world in instead" argument would turn that test into a 1.21.1 → 26.2 upgrade test with
   the assertions already written.

### Decisions left open

- **How much of the envelope bridge to remove** — all 126 bodies, the framework and seven entities
  first, or none. It costs no compatibility either way, so this is purely a question of appetite.
- **Whether loading a 1.21.1 world is a shipping requirement.** This is the one that matters: it
  decides whether the fallbacks get widened, get a datafixer on top, or are deleted along with the
  bridge.
