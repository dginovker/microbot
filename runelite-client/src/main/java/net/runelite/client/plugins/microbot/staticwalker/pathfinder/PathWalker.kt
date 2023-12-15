package net.runelite.client.plugins.microbot.staticwalker.pathfinder

import net.runelite.api.*
import net.runelite.api.coords.LocalPoint
import net.runelite.api.coords.WorldPoint
import net.runelite.client.plugins.microbot.Microbot
import net.runelite.client.plugins.microbot.util.Global
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject
import net.runelite.client.plugins.microbot.util.menu.Rs2Menu
import net.runelite.client.plugins.microbot.util.player.Rs2Player
import java.awt.Shape
import kotlin.random.Random

class PathWalker(private val nodes: List<PathNode>) {

    companion object {
        var enabled: Boolean = false
        private var isInterrupted: Boolean = false
        fun interrupt() {
            isInterrupted = true
        }
        fun getIsInterrupted(): Boolean {
            return isInterrupted
        }
    }

    fun walkPath() {
        enabled = true
        isInterrupted = false

        val maxSkipDistance = 12
        val minSkipDistance = 4
        var currentSkipDistance = Random.nextInt(minSkipDistance, maxSkipDistance)


        val upperBound = nodes.size - 1
        var previousNode: PathNode? = null
        var nextNode: PathNode? = null

        val player = Microbot.getClientForKotlin().localPlayer

        for (currentNode in nodes) {

            if (isInterrupted) {
                enabled = false
                return
            }

            val index = nodes.indexOf(currentNode)
            val isLastNode = nodes.indexOf(currentNode) == nodes.lastIndex

            if (Microbot.getClientForKotlin().getEnergy() > 3000) {
                Rs2Player.toggleRunEnergy(true)
            }

            if (index + 1 <= upperBound) {
                nextNode = nodes.get(index + 1)
            }

            // if we are at the last node, just click it
            if (isLastNode) {
                val minimapPoint = getMinimapPoint(currentNode.worldLocation) ?: continue
                clickPoint(minimapPoint)
                previousNode = currentNode
                continue
            }

            // if we have a transport, use it
            if (currentNode.pathTransports.isNotEmpty()) {
                if (previousNode != null) {
                    val minimapPoint = getMinimapPoint(previousNode.worldLocation)
                    if (minimapPoint != null) {
                        clickPoint(minimapPoint)
                    }
                }

                if (player.worldLocation.distanceTo(currentNode.worldLocation) > 2) {
                    val minimapPoint = getMinimapPoint(currentNode.worldLocation) ?: continue
                    clickPoint(minimapPoint)
                }

                nextNode ?: continue
                handleTransport(currentNode, nextNode)
                previousNode = currentNode
                continue
            }

            // if we are at the first node, and the next node is within the max skip distance, just click it
            if (previousNode == null && nodes.count() < maxSkipDistance) {
                val minimapPoint = getMinimapPoint(currentNode.worldLocation) ?: continue
                clickPoint(minimapPoint)
                previousNode = currentNode
                continue

            } else if (previousNode == null) {
                // if we are at the first node, and the next node is not within the max skip distance, walk to it
                previousNode = currentNode
                continue
            }

            // if our current distance is greater than the max skip distance, walk to it
            val distance = previousNode.worldLocation.distanceTo(currentNode.worldLocation)
            if (distance >= currentSkipDistance) {
                val minimapPoint = getMinimapPoint(currentNode.worldLocation) ?: continue
                clickPointWhileRunning(minimapPoint, currentNode)
                previousNode = currentNode
                currentSkipDistance = Random.nextInt(minSkipDistance, maxSkipDistance)
            }
        }

        enabled = false
    }

    private fun getMinimapPoint(worldPoint: WorldPoint): Point? {
        return Microbot.getClientThreadForKotlin().runOnClientThread {
            val localPoint = LocalPoint.fromWorld(Microbot.getClientForKotlin(), worldPoint) ?: return@runOnClientThread null
            return@runOnClientThread Perspective.localToMinimap(Microbot.getClientForKotlin(), localPoint)
        }
    }

    private fun clickPoint(minimapPoint: Point) {
        Microbot.getMouseForKotlin().click(minimapPoint)
        Global.sleep(1000)
        Global.sleepUntilTrue({ !Rs2Player.isWalking() && !Rs2Player.isAnimating() }, 200, 1000 * 30)
    }

    private fun clickPointWhileRunning(minimapPoint: Point, node: PathNode) {
        val player = Microbot.getClientForKotlin().localPlayer
        Microbot.getMouseForKotlin().click(minimapPoint)
        Global.sleep(1000)
        Global.sleepUntilTrue({
            val distanceToTarget = player.worldLocation.distanceTo(node.worldLocation)
            val isStill = !Rs2Player.isWalking() && !Rs2Player.isAnimating()
            val nearTile = distanceToTarget <= 2
            isStill || nearTile
        }, 200, 1000 * 30)
    }

    private fun handleTransport(pathNode: PathNode, nextNode: PathNode): Boolean {
        Global.sleepUntilTrue({ !Rs2Player.isWalking() && !Rs2Player.isAnimating() }, 200, 1000 * 10)
        Global.sleep(100, 250)

        val transport: PathTransport? = findTransport(pathNode, nextNode)
        if (transport == null) {
            println("cant find transport")
            return false
        }

        val success = operateTransport(transport)
        if (!success) {
            return false
        }

        Global.sleep(1000)
        Global.sleepUntilTrue({ !Rs2Player.isWalking() && !Rs2Player.isAnimating() }, 200, 1000 * 10)
        Global.sleep(250, 500)
        return true
    }

    private fun findTransport(pathNode: PathNode, nextNode: PathNode): PathTransport? {
        return if (pathNode.pathTransports.count() == 1) {
            pathNode.pathTransports.first()
        } else {
            pathNode.pathTransports.firstOrNull { pathTransport: PathTransport -> pathTransport.endPathNode?.worldLocation == nextNode.worldLocation }
        }
    }

    private fun operateTransport(pathTransport: PathTransport): Boolean {
        val player = Microbot.getClientForKotlin().localPlayer
        val clickableShape: Shape? = findTransportObjectShape(pathTransport.objectId)

        if (clickableShape == null) {
            println("No transport ID: ${pathTransport.objectId}, at location: ${pathTransport.startPathNode.worldLocation}")

        } else {
            println("Operating on Transport: ${pathTransport.objectId}, with action: ${pathTransport.action}")
            interactUsingAction(clickableShape, pathTransport.action)
            Global.sleepUntilTrue({ findTransportObjectShape(pathTransport.objectId) == null }, 200, 3000)
        }

        if (pathTransport.addtionalObjectId != null) {
            Global.sleep(300)
            val additionalTransportObjectShape = findTransportObjectShape(pathTransport.addtionalObjectId)

            if (additionalTransportObjectShape == null || pathTransport.additionalAction == "" || pathTransport.additionalAction == null) {
                println("No transport ID: ${pathTransport.addtionalObjectId}, at location: ${pathTransport.startPathNode.worldLocation}")

            } else {
                println("Operating on Transport: ${pathTransport.objectId}, with action: ${pathTransport.additionalAction}")
                interactUsingAction(additionalTransportObjectShape, pathTransport.additionalAction)
            }
        }

        if (pathTransport.endPathNode != null) {
            Global.sleepUntilTrue({ player.worldLocation == pathTransport.endPathNode.worldLocation }, 200, 5000)
        }

        return true
    }

    private fun findTransportObjectShape(objectId: Int): Shape? {
        val player = Microbot.getClientForKotlin().localPlayer

        val allTransportGameObjects = Rs2GameObject.getGameObjects()
            .filter { gameObject: GameObject -> gameObject.id == objectId }
            .filter { gameObject: GameObject -> gameObject.worldLocation != null }

        val allTransportWallObjects = Rs2GameObject.getWallObjects()
            .filter { wallObject: WallObject -> wallObject.id == objectId }
            .filter { wallObject: WallObject -> wallObject.worldLocation != null }

        val allTransportGroundObjects = Rs2GameObject.getGroundObjects()
            .filter { groundObject: GroundObject -> groundObject.id == objectId }
            .filter { groundObject: GroundObject -> groundObject.worldLocation != null }

        var transportGameObject: GameObject? = null
        var transportWallObject: WallObject? = null
        var transportGroundObject: GroundObject? = null
        var clickableShape: Shape? = null


        for (i in 1..6) {
            if (transportGameObject != null) {
                clickableShape = transportGameObject.canvasTilePoly
                break
            }

            if (transportWallObject != null) {
                clickableShape = transportWallObject.convexHull?.bounds
                break
            }

            if (transportGroundObject != null) {
                clickableShape = transportGroundObject.convexHull?.bounds
                break
            }

            transportGameObject = allTransportGameObjects
                .filter { gameObject: GameObject -> gameObject.worldLocation.distanceTo(player.worldLocation) <= i }
                .minByOrNull { gameObject: GameObject -> gameObject.worldLocation.distanceTo(player.worldLocation) }

            transportWallObject = allTransportWallObjects
                .filter { wallObject: WallObject -> wallObject.worldLocation.distanceTo(player.worldLocation) <= i }
                .minByOrNull { wallObject: WallObject -> wallObject.worldLocation.distanceTo(player.worldLocation) }

            transportGroundObject = allTransportGroundObjects
                .filter { groundObject: GroundObject -> groundObject.worldLocation.distanceTo(player.worldLocation) <= i }
                .minByOrNull { groundObject: GroundObject -> groundObject.worldLocation.distanceTo(player.worldLocation) }
        }

        return clickableShape
    }

    private fun interactUsingAction(transportObjectShape: Shape, action: String): Boolean {
        Rs2Menu.doAction(action, transportObjectShape)
        return true
    }

}