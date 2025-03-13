package moe.mukjep.fbsr

import com.demod.dcba.CommandReporting
import com.demod.factorio.*
import com.demod.factorio.prototype.DataPrototype
import com.demod.factorio.prototype.ItemPrototype
import com.demod.fbsr.*
import com.demod.fbsr.WorldMap.RailEdge
import com.demod.fbsr.bs.*
import com.demod.fbsr.entity.ErrorRendering
import moe.mukjep.fbsr.gui.GUIStyle
import com.google.common.collect.Table
import moe.mukjep.fbsr.bs.base.Direction
import moe.mukjep.fbsr.bs.types.BSBlueprint
import moe.mukjep.fbsr.render.*
import org.json.JSONException
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.concurrent.Volatile
import kotlin.math.*

object FBSR {
    private const val TARGET_FILE_SIZE = (10 shl 20).toLong() // 10MB
    private const val ESTIMATED_JPG_PIXELS_PER_BYTE = 3.5f // Based on measuring large JPG renders
    private const val MAX_WORLD_RENDER_PIXELS = (TARGET_FILE_SIZE * ESTIMATED_JPG_PIXELS_PER_BYTE).toLong()

    const val TILE_SIZE: Double = 64.0
    private val GRID_STROKE = BasicStroke((3 / TILE_SIZE).toFloat())

    private val itemColorCache: MutableMap<String, Color> = HashMap()

    @Volatile
    @get:JvmStatic
    var version: String? = null
        private set
        get() = field ?: try {
            val baseInfo = ModInfo(
                File(FactorioData.factorio, "data/base/info.json").inputStream().use(Utils::readJsonFromStream)
            )
            baseInfo.version.also { field = it }
        } catch (e: Exception) {
            throw RuntimeException("Exception during read version", e)
        }

    @Volatile
    private var initialized = false

    private fun addToItemAmount(items: MutableMap<String, Double>, itemName: String, add: Double) {
        var amount = items.getOrDefault(itemName, 0.0)
        amount += add
        items[itemName] = amount
    }

    private fun computeBounds(renderers: List<Renderer>, includeIgnoredBounds: Boolean): Rectangle2D.Double {
        if (renderers.isEmpty()) {
            return Rectangle2D.Double()
        }
        var first = true
        var minX = 0.0
        var minY = 0.0
        var maxX = 0.0
        var maxY = 0.0
        for (renderer in renderers) {
            if (!includeIgnoredBounds && renderer.ignoreBoundsCalculation()) {
                continue
            }
            val bounds = renderer.bounds
            if (first) {
                first = false
                minX = bounds.minX
                minY = bounds.minY
                maxX = bounds.maxX
                maxY = bounds.maxY
            } else {
                minX = min(minX, bounds.minX)
                minY = min(minY, bounds.minY)
                maxX = max(maxX, bounds.maxX)
                maxY = max(maxY, bounds.maxY)
            }
        }
        return Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY)
    }

    @JvmStatic
    fun generateSummedTotalItems(blueprint: BSBlueprint): Map<String, Double> {
        val ret = LinkedHashMap<String, Double>()
        if (blueprint.entities.isNotEmpty()) {
            ret["Entities"] = blueprint.entities.size.toDouble()
        }

        val table = FactorioData.getTable()

        for (entity in blueprint.entities) {
            val modules = RenderUtils.getModules(entity, table)
            for (entry in modules.entrySet()) {
                addToItemAmount(ret, "Modules", entry.count.toDouble())
            }
        }
        for (tile in blueprint.tiles) {
            var itemName = tile.name
            // TODO hard-coded
            if (itemName.startsWith("hazard-concrete")) {
                itemName = "hazard-concrete"
            }
            if (itemName.startsWith("refined-hazard-concrete")) {
                itemName = "refined-hazard-concrete"
            }
            if (itemName == "stone-path") {
                itemName = "stone-brick"
            }
            if (itemName == "grass-1") {
                itemName = "landfill"
            }
            if (!table.getItem(itemName).isPresent) {
                System.err.println("MISSING TILE ITEM: $itemName")
                continue
            }
            addToItemAmount(ret, "Tiles", 1.0)
        }

        return ret
    }

    @JvmStatic
    fun generateTotalItems(blueprint: BSBlueprint): Map<String, Double> {
        val table = FactorioData.getTable()

        val ret = LinkedHashMap<String, Double>()
        for (entity in blueprint.entities) {
            val entityName = entity.name
            val items = table.getItemsForEntity(entityName)
            if (items.isEmpty()) {
                continue
            }
            items.forEach { i: ItemPrototype ->
                addToItemAmount(ret, i.name, 1.0)
            }

            val modules = RenderUtils.getModules(entity, table)
            for (entry in modules.entrySet()) {
                addToItemAmount(ret, entry.element, entry.count.toDouble())
            }
        }
        for (tile in blueprint.tiles) {
            var itemName = tile.name
            // TODO hard-coded
            if (itemName.startsWith("hazard-concrete")) {
                itemName = "hazard-concrete"
            }
            if (itemName.startsWith("refined-hazard-concrete")) {
                itemName = "refined-hazard-concrete"
            }
            if (itemName == "stone-path") {
                itemName = "stone-brick"
            }
            if (itemName == "grass-1") {
                itemName = "landfill"
            }
            if (!table.getItem(itemName).isPresent) {
                System.err.println("MISSING TILE ITEM: $itemName")
                continue
            }
            addToItemAmount(ret, itemName, 1.0)
        }
        return ret
    }

    @JvmStatic
    fun generateTotalRawItems(totalItems: Map<String, Double>): Map<String, Double> {
        val table = FactorioData.getTable()
        val recipes = table.recipes
        val ret = LinkedHashMap<String, Double>()
        val calculator = TotalRawCalculator(recipes)
        for ((recipeName, recipeAmount) in totalItems) {
            table.getRecipe(recipeName).ifPresent { r ->
                val multiplier = recipeAmount / r.outputs[recipeName]!!
                val totalRaw = calculator.compute(r)
                for ((itemName, itemAmount) in totalRaw) {
                    addToItemAmount(ret, itemName, itemAmount * multiplier)
                }
            }
        }
        return ret
    }

    private fun getItemLogisticColor(table: DataTable, itemName: String): Color {
        return itemColorCache.computeIfAbsent(
            itemName
        ) { k: String ->
            val optProto = table.getItem(k)
            if (!optProto.isPresent) {
                System.err.println("ITEM MISSING FOR LOGISTICS: $k")
                return@computeIfAbsent Color.MAGENTA
            }
            val prototype: DataPrototype = optProto.get()
            val image = FactorioData.getIcon(prototype)
            val color = RenderUtils.getAverageColor(image)
            val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
            Color.getHSBColor(
                hsb[0],
                max(0.25, hsb[1].toDouble()).toFloat(),
                max(0.5, hsb[2].toDouble()).toFloat()
            )
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    @JvmStatic
    fun initialize() {
        if (initialized) {
            return
        }
        val table = FactorioData.getTable()
        EntityRendererFactory.initPrototypes(table)
        TileRendererFactory.initPrototypes(table)
        initialized = true
    }

    private fun populateRailBlocking(map: WorldMap) {
        map.railNodes.cellSet().filter { c ->
            c.value.hasSignals()
        }.forEach { c ->
            val blockingNode = c.value
            val signals = blockingNode.signals
            for (signalDir in signals) {
                val blockingDir = signalDir.back()
                if (signals.contains(blockingDir)) {
                    continue
                }

                run {
                    val work = ArrayDeque<RailEdge>()
                    work.addAll(blockingNode.getOutgoingEdges(blockingDir))
                    while (!work.isEmpty()) {
                        val edge = work.poll()
                        if (edge.isBlocked) {
                            continue
                        }
                        edge.isBlocked = true
                        val node = map.getRailNode(edge.endPos).get()
                        if (node.hasSignals()) {
                            continue
                        }
                        if (node.getIncomingEdges(edge.endDir).all { e: RailEdge -> e.isBlocked }
                        ) {
                            work.addAll(node.getOutgoingEdges(edge.endDir.back()))
                        }
                    }
                }

                run {
                    val work = ArrayDeque<RailEdge>()
                    work.addAll(blockingNode.getIncomingEdges(blockingDir.back()))
                    while (!work.isEmpty()) {
                        val edge = work.poll()
                        if (edge.isBlocked) {
                            continue
                        }
                        edge.isBlocked = true
                        val node = map.getRailNode(edge.startPos).get()
                        if (node.hasSignals()) {
                            continue
                        }
                        if (node.getOutgoingEdges(edge.startDir).all { e -> e.isBlocked }
                        ) {
                            work.addAll(node.getIncomingEdges(edge.startDir.back()))
                        }
                    }
                }
            }
        }
    }

    private fun populateRailStationLogistics(map: WorldMap) {
        map.railNodes.cellSet().filter { c ->
            c.value.station.isPresent
        }.forEach { c ->
            val stationNode = c.value
            val stationDir = stationNode.station.get()

            run {
                val work = ArrayDeque<RailEdge>()
                work.addAll(stationNode.getOutgoingEdges(stationDir))
                work.addAll(stationNode.getOutgoingEdges(stationDir.back()))
                while (!work.isEmpty()) {
                    val edge = work.poll()
                    if (edge.isBlocked || edge.isOutput) {
                        continue
                    }
                    edge.isOutput = true
                    val node = map.getRailNode(edge.endPos).get()
                    if (node.getIncomingEdges(edge.endDir).all { e -> e.isOutput }
                    ) {
                        work.addAll(node.getOutgoingEdges(edge.endDir.back()))
                    }
                }
            }
            run {
                val work = ArrayDeque<RailEdge>()
                work.addAll(stationNode.getIncomingEdges(stationDir.back()))
                while (!work.isEmpty()) {
                    val edge = work.poll()
                    if (edge.isBlocked || edge.isInput) {
                        continue
                    }
                    edge.isInput = true
                    val node = map.getRailNode(edge.startPos).get()
                    if (node.getOutgoingEdges(edge.startDir).all { e: RailEdge -> e.isInput }
                    ) {
                        work.addAll(node.getIncomingEdges(edge.startDir.back()))
                    }
                }
            }
        }
    }

    private fun populateReverseLogistics(map: WorldMap) {
        val logisticGrid = map.logisticGrid
        logisticGrid.cellSet().forEach { c ->
            val pos = Point2D.Double(c.rowKey / 2.0 + 0.25, c.columnKey / 2.0 + 0.25)
            val cell = c.value
            cell.move.ifPresent { d: Direction ->
                map.getLogisticGridCell(d.offset(pos, 0.5)).filter { mc: LogisticGridCell -> mc.acceptMoveFrom(d) }
                    .ifPresent { mc: LogisticGridCell -> mc.addMovedFrom(d.back()) }
            }
            cell.warps
                .ifPresent { l: List<Point2D.Double?> ->
                    for (p in l) {
                        map.getLogisticGridCell(p)
                            .ifPresent { mc: LogisticGridCell ->
                                mc.addWarpedFrom(
                                    pos
                                )
                            }
                    }
                }
        }
    }

    private fun populateTransitLogistics(map: WorldMap, populateInputs: Boolean, populateOutputs: Boolean) {
        val logisticGrid = map.logisticGrid
        val work = ArrayDeque<Map.Entry<Point2D.Double, LogisticGridCell>>()

        if (populateOutputs) {
            logisticGrid.cellSet().filter { c ->
                c.value.isTransitStart
            }.forEach { c ->
                val outputs = c.value.outputs.get()
                for (item in outputs) {
                    work.add(
                        AbstractMap.SimpleEntry(
                            map.getLogisticCellPosition(
                                c
                            ), c.value
                        )
                    )
                    while (!work.isEmpty()) {
                        val pair = work.pop()
                        val cellPos = pair.key
                        val cell = pair.value
                        if (cell.addTransit(item) && !cell.isBannedOutput(item)) {
                            cell.move.ifPresent { d: Direction ->
                                val nextCellPos = d.offset(cellPos, 0.5)
                                map.getLogisticGridCell(nextCellPos)
                                    .filter { nc: LogisticGridCell ->
                                        !nc.isBlockTransit && nc.acceptMoveFrom(
                                            d
                                        )
                                    }
                                    .ifPresent { next: LogisticGridCell ->
                                        work.add(
                                            AbstractMap.SimpleEntry(
                                                nextCellPos,
                                                next
                                            )
                                        )
                                    }
                            }
                            cell.warps
                                .ifPresent { l: List<Point2D.Double> ->
                                    for (p in l) {
                                        map.getLogisticGridCell(p)
                                            .filter { nc: LogisticGridCell ->
                                                (!nc.isBlockTransit
                                                        && !(nc.move
                                                    .isPresent && cell.isBlockWarpFromIfMove)
                                                        && !(cell.move
                                                    .isPresent && nc.isBlockWarpToIfMove))
                                            }
                                            .ifPresent { next: LogisticGridCell ->
                                                work.add(
                                                    AbstractMap.SimpleEntry(
                                                        p,
                                                        next
                                                    )
                                                )
                                            }
                                    }
                                }
                        }
                    }
                }
            }
        }

        if (populateInputs) {
            logisticGrid.cellSet().filter { c ->
                c.value.isTransitEnd
            }.forEach { c ->
                val inputs = c.value.inputs.get()
                for (item in inputs) {
                    work.add(
                        AbstractMap.SimpleEntry(
                            map.getLogisticCellPosition(
                                c
                            ), c.value
                        )
                    )
                    while (!work.isEmpty()) {
                        val pair = work.pop()
                        val cellPos = pair.key
                        val cell = pair.value
                        if (cell.addTransit(item)) {
                            cell.movedFrom
                                .ifPresent { l: List<Direction> ->
                                    for (d in l) {
                                        val nextCellPos = d.offset(cellPos, 0.5)
                                        map.getLogisticGridCell(nextCellPos)
                                            .filter { nc: LogisticGridCell -> !nc.isBlockTransit }
                                            .ifPresent { next: LogisticGridCell ->
                                                work.add(
                                                    AbstractMap.SimpleEntry(
                                                        nextCellPos,
                                                        next
                                                    )
                                                )
                                            }
                                    }
                                }
                            cell.warpedFrom
                                .ifPresent { l: List<Point2D.Double> ->
                                    for (p in l) {
                                        map.getLogisticGridCell(p)
                                            .filter { nc: LogisticGridCell -> !nc.isBlockTransit }
                                            .ifPresent { next: LogisticGridCell ->
                                                work.add(
                                                    AbstractMap.SimpleEntry(
                                                        p,
                                                        next
                                                    )
                                                )
                                            }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    // FIXME the generic type checking is all screwed up
    fun renderBlueprint(request: RenderRequest): RenderResult {
        val (blueprint, reporting) = request

        println("Rendering " + blueprint.label.orElse("Untitled Blueprint") + " " + blueprint.version)
        val startMillis = System.currentTimeMillis()

        val table = FactorioData.getTable()
        val map = WorldMap()

        map.isAltMode = request.show.altMode

        val entityRenderingTuples = ArrayList<EntityRenderingTuple<BSEntity>>()
        val tileRenderingTuples = ArrayList<TileRenderingTuple>()
        val entityByNumber = HashMap<Int, EntityRenderingTuple<BSEntity>>()

        for (metaEntity in blueprint.entities) {
            var factory = EntityRendererFactory.forName<BSEntity>(metaEntity.name)
            val entity = try {
                if (metaEntity.isLegacy) {
                    factory.parseEntity(metaEntity.legacy)
                } else {
                    factory.parseEntity(metaEntity.json)
                }
            } catch (e: Exception) {
                metaEntity.parseException = Optional.of(e)
                metaEntity
            }
            if (metaEntity.parseException.isPresent) {
                factory = ErrorRendering(factory)
                reporting.addException(metaEntity.parseException.get(), entity!!.name + " " + entity.entityNumber)
            }
            val tuple = EntityRenderingTuple<BSEntity>(entity, factory)
            entityRenderingTuples.add(tuple)
            entityByNumber[entity!!.entityNumber] = tuple
        }
        for (tile in blueprint.tiles) {
            val factory = TileRendererFactory.forName(tile.name)
            val tuple = TileRenderingTuple(tile, factory)
            tileRenderingTuples.add(tuple)
        }

        map.isSpacePlatform = tileRenderingTuples.any { t -> t.factory.isSpacePlatform }

        entityRenderingTuples.forEach { t ->
            try {
                t.factory.populateWorldMap(map, table, t.entity)
            } catch (e: Exception) {
                reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.entity!!.name)
            }
        }
        tileRenderingTuples.forEach { t ->
            try {
                t.factory.populateWorldMap(map, table, t.tile)
            } catch (e: Exception) {
                reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.tile.name)
            }
        }

        entityRenderingTuples.forEach { t ->
            try {
                t.factory.populateLogistics(map, table, t.entity)
            } catch (e: Exception) {
                reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.entity!!.name)
            }
        }

        populateReverseLogistics(map)
        populateTransitLogistics(map, request.show.pathInputs, request.show.pathOutputs)

        populateRailBlocking(map)
        populateRailStationLogistics(map)

        val renderers = ArrayList<Renderer>()
        val register = Consumer { r: Renderer -> renderers.add(r) }

        TileRendererFactory.createAllRenderers({ e ->
            renderers.add(
                e
            )
        }, tileRenderingTuples)

        tileRenderingTuples.forEach { t ->
            try {
                t.factory.createRenderers(register, map, table, t.tile)
            } catch (e: Exception) {
                reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.tile.name)
            }
        }

        entityRenderingTuples.forEach { t ->
            try {
                t.factory.createRenderers(register, map, table, t.entity)
            } catch (e: Exception) {
                reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.entity.name)
            }
        }

        if (map.isAltMode) {
            entityRenderingTuples.forEach { t ->
                try {
                    t.factory.createModuleIcons(register, map, table, t.entity)
                } catch (e: Exception) {
                    reporting.addException(e, t.factory.javaClass.simpleName + ", " + t.entity.name)
                }
            }
        }

        val connectorOrientations: MutableMap<Int, Double> = HashMap()
        val wireEntityNumbers = blueprint.wires.stream()
            .flatMapToInt { w: BSWire ->
                IntStream.of(
                    w.firstEntityNumber,
                    w.secondEntityNumber
                )
            }.distinct().toArray()

        for (entityNumber in wireEntityNumbers) {
            val tuple = entityByNumber[entityNumber]!!
            val wired = blueprint.wires.stream().flatMapToInt { w: BSWire ->
                if (w.firstEntityNumber == entityNumber) {
                    return@flatMapToInt IntStream.of(w.secondEntityNumber)
                } else if (w.secondEntityNumber == entityNumber) {
                    return@flatMapToInt IntStream.of(w.firstEntityNumber)
                } else {
                    return@flatMapToInt IntStream.of()
                }
            }.mapToObj { key: Int ->
                entityByNumber[key]
            }.collect(Collectors.toList())

            val orientation = tuple.factory.initWireConnector(register, tuple.entity, wired)
            connectorOrientations[entityNumber] = orientation
        }

        for (wire in blueprint.wires) {
            try {
                val first = entityByNumber[wire.firstEntityNumber]!!
                val second = entityByNumber[wire.secondEntityNumber]!!

                val orientation1 = connectorOrientations[wire.firstEntityNumber]!!
                val orientation2 = connectorOrientations[wire.secondEntityNumber]!!

                val firstPoint = first.factory.createWirePoint(
                    register,
                    first.entity.position.createPoint(), orientation1, wire.firstWireConnectorId
                )
                val secondPoint = second.factory.createWirePoint(
                    register,
                    second.entity.position.createPoint(), orientation2, wire.secondWireConnectorId
                )

                if (!firstPoint.isPresent || !secondPoint.isPresent) {
                    continue  // Probably something modded
                }

                renderers.add(
                    RenderUtils.createWireRenderer(
                        firstPoint.get().position,
                        secondPoint.get().position, firstPoint.get().color.color,
                        firstPoint.get().shadow, secondPoint.get().shadow
                    )
                )
            } catch (e: Exception) {
                reporting.addException(
                    e, "Wire " + wire.firstEntityNumber + ", " + wire.firstWireConnectorId + ", "
                            + wire.secondEntityNumber + ", " + wire.secondWireConnectorId
                )
            }
        }

        showLogisticGrid(register, table, map, request.debug.pathItems)
        showRailLogistics(register, table, map, request.debug.pathRails)

        if (request.debug.entityPlacement) {
            entityRenderingTuples.forEach { t: EntityRenderingTuple<*> ->
                val pos = t.entity.position.createPoint()
                renderers.add(object : Renderer(Layer.DEBUG_P, pos, true) {
                    override fun render(g: Graphics2D) {
                        g.color = Color.cyan
                        g.fill(Ellipse2D.Double(pos.x - 0.1, pos.y - 0.1, 0.2, 0.2))
                        val ps = g.stroke
                        g.stroke = BasicStroke(3f / TILE_SIZE.toFloat())
                        g.color = Color.green
                        g.draw(Line2D.Double(pos, t.entity.direction.offset(pos, 0.3)))
                        g.stroke = ps
                    }
                })
            }
            tileRenderingTuples.forEach { t: TileRenderingTuple ->
                val pos = t.tile.position.createPoint()
                renderers.add(object : Renderer(Layer.DEBUG_P, pos, true) {
                    override fun render(g: Graphics2D) {
                        g.color = Color.cyan
                        g.fill(Ellipse2D.Double(pos.x - 0.1, pos.y - 0.1, 0.2, 0.2))
                    }
                })
            }
        }

        val gridPadding = if ((request.gridLines != null && request.show.gridNumbers)) 1.0 else 0.0
        val gridRound = if ((request.gridLines != null && request.show.gridNumbers)) 0.6 else 0.2
        val worldPadding = 0.5

        val visualBounds = computeBounds(renderers, true)
        visualBounds.setFrameFromDiagonal(
            floor(visualBounds.minX + 0.4),
            floor(visualBounds.minY + 0.4), ceil(visualBounds.maxX - 0.4),
            ceil(visualBounds.maxY - 0.4)
        )
        val gridBounds = computeBounds(renderers, false)
        gridBounds.setFrameFromDiagonal(
            floor(gridBounds.minX + 0.4) - gridPadding,
            floor(gridBounds.minY + 0.4) - gridPadding,
            ceil(gridBounds.maxX - 0.4) + gridPadding,
            ceil(gridBounds.maxY - 0.4) + gridPadding
        )

        val worldBounds = Rectangle2D.Double()
        worldBounds.setFrameFromDiagonal(
            min(visualBounds.minX, gridBounds.minX) - worldPadding,
            min(visualBounds.minY, gridBounds.minY) - worldPadding,
            max(visualBounds.maxX, gridBounds.maxX) + worldPadding,
            max(visualBounds.maxY, gridBounds.maxY) + worldPadding
        )

        // Max scale limit
        var worldRenderScale = request.maxScale

        val gridPlatformMode = map.isSpacePlatform && !request.show.gridNumbers

        // Shrink down the scale to fit the max requirements
        val maxWidthPixels = request.maxWidth
        val maxHeightPixels = request.maxHeight
        val maxPixels =
            min(MAX_WORLD_RENDER_PIXELS.toDouble(), (maxWidthPixels.toLong() * maxHeightPixels.toLong()).toDouble())
                .toLong()

        if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE) > maxWidthPixels) {
            worldRenderScale *= (maxWidthPixels / (worldBounds.getWidth() * worldRenderScale * TILE_SIZE))
        }
        if ((worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxHeightPixels) {
            worldRenderScale *= (maxHeightPixels / (worldBounds.getHeight() * worldRenderScale * TILE_SIZE))
        }
        if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE)
            * (worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxPixels
        ) {
            worldRenderScale *= sqrt(
                maxPixels / ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE)
                        * (worldBounds.getHeight() * worldRenderScale * TILE_SIZE))
            )
        }

        // Expand the world to fit the min requirements
        val minWidthPixels = request.minWidth
        val minHeightPixels = request.minHeight

        if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE) < minWidthPixels) {
            val padding = ((minWidthPixels - (worldBounds.getWidth() * worldRenderScale * TILE_SIZE))
                    / (worldRenderScale * TILE_SIZE))
            worldBounds.x -= padding / 2.0
            worldBounds.width += padding
        }
        if ((worldBounds.getHeight() * worldRenderScale * TILE_SIZE) < minHeightPixels) {
            val padding = ((minHeightPixels - (worldBounds.getHeight() * worldRenderScale * TILE_SIZE))
                    / (worldRenderScale * TILE_SIZE))
            worldBounds.y -= padding / 2.0
            worldBounds.height += padding
        }

        val imageWidth = max(
            minWidthPixels.toDouble(),
            min(
                maxWidthPixels.toDouble(), Math.round(worldBounds.getWidth() * worldRenderScale * TILE_SIZE).toInt()
                    .toDouble()
            )
        )
            .toInt()
        val imageHeight = max(
            minHeightPixels.toDouble(),
            min(
                maxHeightPixels.toDouble(), Math.round(worldBounds.getHeight() * worldRenderScale * TILE_SIZE).toInt()
                    .toDouble()
            )
        )
            .toInt()
        println("\t" + imageWidth + "x" + imageHeight + " (" + worldRenderScale + ")")

        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        val shadowImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val shadowG = shadowImage.createGraphics()
        val noXform = g.transform

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        g.scale(image.width / worldBounds.getWidth(), image.height / worldBounds.getHeight())
        g.translate(-worldBounds.getX(), -worldBounds.getY())
        val worldXform = g.transform

        shadowG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        shadowG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        shadowG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        shadowG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        shadowG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        shadowG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        shadowG.transform = worldXform

        // Background
        request.background?.let {
            g.color = it
            g.fill(worldBounds)
        }

        val gridTooSmall = (1 / worldRenderScale) > 5
        val gridLayer = if (request.show.gridAboveBelts) {
            Layer.GRID_ABOVE_BELTS
        } else {
            Layer.GRID
        }

        // Grid Lines
        if (request.gridLines != null && !gridTooSmall) {
            if (gridPlatformMode) {
                renderers.add(object : Renderer(gridLayer, gridBounds, true) {
                    @Throws(Exception::class)
                    override fun render(g: Graphics2D) {
                        g.stroke = GRID_STROKE
                        g.color = request.gridLines!!
                        val rect = Rectangle2D.Double(0.0, 0.0, 1.0, 1.0)
                        for (tuple in tileRenderingTuples) {
                            val pos = tuple.tile.position
                            rect.x = pos.x
                            rect.y = pos.y
                            g.draw(rect)
                        }
                    }
                })
            } else {
                renderers.add(object : Renderer(gridLayer, gridBounds, true) {
                    @Throws(Exception::class)
                    override fun render(g: Graphics2D) {
                        g.stroke = GRID_STROKE
                        g.color = request.gridLines!!
                        var x = (Math.round(gridBounds.minX) + 1).toDouble()
                        while (x <= gridBounds.maxX - 1) {
                            g.draw(Line2D.Double(x, gridBounds.minY, x, gridBounds.maxY))
                            x++
                        }
                        var y = (Math.round(gridBounds.minY) + 1).toDouble()
                        while (y <= gridBounds.maxY - 1) {
                            g.draw(Line2D.Double(gridBounds.minX, y, gridBounds.maxX, y))
                            y++
                        }
                        g.draw(
                            RoundRectangle2D.Double(
                                gridBounds.x, gridBounds.y, gridBounds.width,
                                gridBounds.height, gridRound, gridRound
                            )
                        )
                    }
                })
            }
        }

        renderers.filterIsInstance<EntityRenderer>()
            .forEach { r2: EntityRenderer ->
                try {
                    r2.renderShadows(shadowG)
                } catch (e: Exception) {
                    reporting.addException(e)
                }
            }
        shadowG.dispose()
        RenderUtils.halveAlpha(shadowImage)

        renderers.add(object : Renderer(Layer.SHADOW_BUFFER, worldBounds, true) {
            override fun render(g: Graphics2D) {
                val tempXform = g.transform
                g.transform = noXform
                g.drawImage(shadowImage, 0, 0, null)

                g.transform = tempXform
            }
        })

        val debugBounds = request.debug.entityPlacement
        renderers.stream().sorted { r11: Renderer, r21: Renderer ->

            var ret = r11.layer.compareTo(r21.layer)
            if (ret != 0) {
                return@sorted ret
            }

            val b1 = r11.bounds
            val b2 = r21.bounds

            ret = b1.minY.compareTo(b2.minY)
            if (ret != 0) {
                return@sorted ret
            }

            ret = b1.minX.compareTo(b2.minX)
            if (ret != 0) {
                return@sorted ret
            }

            ret = r11.layer.compareTo(r21.layer)
            ret
        }.forEach { r4: Renderer ->
            try {
                r4.render(g)

                if (debugBounds) {
                    g.stroke = BasicStroke(1f / TILE_SIZE.toFloat())
                    g.color = if (r4.ignoreBoundsCalculation()) Color.gray else Color.magenta
                    g.draw(r4.bounds)
                }
            } catch (e: Exception) {
                reporting.addException(e)
            }
        }
        g.transform = worldXform

        // Grid Numbers
        if (request.gridLines != null && request.show.gridNumbers && !gridTooSmall) {
            g.color = request.gridLines!!
            g.font = GUIStyle.FONT_BP_REGULAR.deriveFont(0.6f)
            val tx = 0.18f
            val ty = 0.68f
            val gridColor = request.gridLines!!
            g.color = gridColor
            run {
                var x = (Math.round(gridBounds.minX) + 1).toDouble()
                var i = 1.0
                while (x <= gridBounds.maxX - 2) {
                    val strNum = String.format("%02d", Math.round(i).toInt() % 100)
                    val x1 = x.toFloat() + tx
                    val y1 = (gridBounds.maxY - 1 + ty).toFloat()
                    val y2 = (gridBounds.minY + ty).toFloat()
                    g.drawString(strNum, x1, y1)
                    g.drawString(strNum, x1, y2)
                    x++
                    i++
                }
            }
            var y = (Math.round(gridBounds.minY) + 1).toDouble()
            var i = 1.0
            while (y <= gridBounds.maxY - 2) {
                val strNum = String.format("%02d", Math.round(i).toInt() % 100)
                val x1 = (gridBounds.maxX - 1 + tx).toFloat()
                val y1 = y.toFloat() + ty
                val x2 = (gridBounds.minX + tx).toFloat()
                g.drawString(strNum, x1, y1)
                g.drawString(strNum, x2, y1)
                y++
                i++
            }
        }

        val level = reporting.level
        if (level != CommandReporting.Level.INFO) {
            g.transform = worldXform
            g.stroke = GRID_STROKE
            g.color = level.color.darker()
            g.draw(worldBounds)
        }

        g.dispose()

        val endMillis = System.currentTimeMillis()
        println("\tRender Time " + (endMillis - startMillis) + " ms")

        val result =
            RenderResult(image, endMillis - startMillis, worldRenderScale)
        return result
    }

    private fun showLogisticGrid(register: Consumer<Renderer>, table: DataTable, map: WorldMap, debug: Boolean) {
        val logisticGrid = map.logisticGrid
        logisticGrid.cellSet().forEach { c: Table.Cell<Int, Int, LogisticGridCell> ->
            val pos = Point2D.Double(c.rowKey / 2.0 + 0.25, c.columnKey / 2.0 + 0.25)
            val cell = c.value
            cell.transits
                .ifPresent { s: SortedSet<String> ->
                    if (s.isEmpty()) {
                        return@ifPresent
                    }
                    val width = 0.3f / s.size
                    for ((i, itemName) in s.withIndex()) {
                        val shift =
                            ((i + 1) / (s.size + 1).toDouble() - 0.5) / 3.0 // -0.25..0.25
                        cell.move.filter { d: Direction ->
                            map.getLogisticGridCell(d.offset(pos, 0.5))
                                .map<Boolean> { obj: LogisticGridCell -> obj.isAccepting }
                                .orElse(false)
                        }.ifPresent { d: Direction ->
                            register.accept(object :
                                Renderer(Layer.LOGISTICS_MOVE, pos, true) {
                                override fun render(g: Graphics2D) {
                                    val ps = g.stroke
                                    g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                                    g.color = RenderUtils.withAlpha(
                                        getItemLogisticColor(table, itemName),
                                        255 - 127 / s.size
                                    )
                                    g.draw(
                                        Line2D.Double(
                                            d.right().offset(pos, shift),
                                            d.right().offset(d.offset(pos, 0.5), shift)
                                        )
                                    )
                                    g.stroke = ps
                                }
                            })
                        }
                    }
                }
            if (debug) {
                cell.movedFrom
                    .ifPresent { l: List<Direction> ->
                        for (d in l) {
                            val p = d.offset(pos, 0.5)
                            register.accept(object : Renderer(Layer.DEBUG_LA1, p, true) {
                                override fun render(g: Graphics2D) {
                                    val ps = g.stroke
                                    g.stroke = BasicStroke(
                                        2 / TILE_SIZE.toFloat(), BasicStroke.CAP_ROUND,
                                        BasicStroke.JOIN_ROUND
                                    )
                                    g.color = Color.cyan
                                    g.draw(Line2D.Double(pos, p))
                                    g.stroke = ps
                                }
                            })
                        }
                    }
            }
        }
    }

    private fun showRailLogistics(register: Consumer<Renderer>, table: DataTable, map: WorldMap, debug: Boolean) {
        for ((edge1, value) in map.railEdges) {
            val input = edge1.isInput || value.isInput
            val output = edge1.isOutput || value.isOutput

            if (input || output) {
                val p1 = edge1.startPos
                val d1 = edge1.startDir
                val p2 = edge1.endPos
                val d2 = edge1.endDir

                register.accept(object : Renderer(Layer.LOGISTICS_RAIL_IO, edge1.startPos, true) {
                    override fun render(g: Graphics2D) {
                        val path = if (edge1.isCurved) {
                            val control = 1.7
                            val cc1 = d1.offset(p1, control)
                            val cc2 = d2.offset(p2, control)
                            CubicCurve2D.Double(p1.x, p1.y, cc1.x, cc1.y, cc2.x, cc2.y, p2.x, p2.y)
                        } else {
                            Line2D.Double(p1, p2)
                        }

                        val color = if ((input && output)) Color.yellow else if (input) Color.green else Color.red

                        val ps = g.stroke
                        g.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)
                        g.color = RenderUtils.withAlpha(color, 32)
                        g.draw(path)
                        g.stroke = ps
                    }
                })
            }

            if (debug) {
                for (edge in arrayOf(edge1, value)) {
                    if (edge.isBlocked) {
                        continue
                    }

                    val p1 = edge.startPos
                    val d1 = edge.startDir
                    val p2 = edge.endPos
                    val d2 = edge.endDir

                    register.accept(object : Renderer(Layer.LOGISTICS_RAIL_IO, edge.startPos, true) {
                        override fun render(g: Graphics2D) {
                            val ps = g.stroke
                            g.stroke = BasicStroke(
                                2 / TILE_SIZE.toFloat(), BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_ROUND
                            )
                            g.color = RenderUtils.withAlpha(Color.green, 92)
                            g.draw(Line2D.Double(d1.right().offset(p1), d2.left().offset(p2)))
                            g.stroke = ps
                        }
                    })
                }
            }
        }

        if (debug) {
            map.railNodes.cellSet().forEach { c ->
                val pos = Point2D.Double(c.rowKey / 2.0, c.columnKey / 2.0)
                val node = c.value

                register.accept(object : Renderer(Layer.DEBUG_RAIL1, pos, true) {
                    override fun render(g: Graphics2D) {
                        val ps = g.stroke
                        g.stroke = BasicStroke(
                            1 / TILE_SIZE.toFloat(),
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND
                        )

                        g.color = Color.cyan
                        g.font = Font("Courier New", Font.PLAIN, 1)
                        for (dir in Direction.entries) {
                            val edges = node.getIncomingEdges(dir)
                            if (!edges.isEmpty()) {
                                val p1 = dir.right().offset(pos, 0.25)
                                val p2 = dir.offset(p1, 0.5)
                                g.draw(Line2D.Double(p1, p2))
                                g.drawString("" + edges.size, p2.x.toFloat() - 0.1f, p2.y.toFloat() - 0.2f)
                            }
                        }

                        g.stroke = ps
                    }
                })
                register.accept(object : Renderer(Layer.DEBUG_RAIL2, pos, true) {
                    override fun render(g: Graphics2D) {
                        val ps = g.stroke
                        g.stroke = BasicStroke(
                            1 / TILE_SIZE.toFloat(),
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND
                        )

                        g.color = Color.magenta
                        g.font = Font("Courier New", Font.PLAIN, 1)
                        for (dir in Direction.entries) {
                            val edges = node.getOutgoingEdges(dir)
                            if (!edges.isEmpty()) {
                                val p1 = dir.left().offset(pos, 0.25)
                                val p2 = dir.offset(p1, 0.5)
                                g.draw(Line2D.Double(p1, p2))
                                g.drawString("" + edges.size, p2.x.toFloat() - 0.1f, p2.y.toFloat() - 0.2f)
                            }
                        }

                        g.stroke = ps
                    }
                })
            }
        }
    }

}