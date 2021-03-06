package com.github.leeonardoo.manhunt

import com.github.leeonardoo.manhunt.api.event.CompassUpdateCallback
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import org.apache.logging.log4j.Level
import java.util.*

object ManhuntUtils {

    var speedrunner: UUID? = null
    var hunters = mutableListOf<UUID>()
    var haveMod = mutableListOf<PlayerEntity>()

    @JvmField
    var SERVER_QUESTION_PACKET_ID: Identifier = Identifier(Manhunt.MOD_ID, "question")

    @JvmField
    val CLIENT_ANSWER_PACKET_ID = Identifier(Manhunt.MOD_ID, "answer")

    @Throws(CommandSyntaxException::class)
    fun playerHasMod(context: CommandContext<ServerCommandSource>) = context.source.entity != null &&
            context.source.entity is PlayerEntity &&
            this.haveMod.contains(context.source.player)

    fun fromCmdContext(context: CommandContext<ServerCommandSource>, uuid: UUID): ServerPlayerEntity? {
        return context.source.minecraftServer.playerManager.getPlayer(uuid)
    }

    @JvmStatic
    fun fromServer(server: MinecraftServer, uuid: UUID?): ServerPlayerEntity? {
        return server.playerManager.getPlayer(uuid)
    }

    @JvmStatic
    fun updateCompass(compass: ItemStack, target: ServerPlayerEntity?): ItemStack {
        if (target == null) {
            Manhunt.log(Level.WARN, "Compass target is null, can't update compass! Please fix!")
            return compass.copy()
        }

        // Is dimension disabled?
        if (Manhunt.CONFIG.disabledDimensions.contains(target.serverWorld.registryKey.value.toString()))
            return compass.copy()

        val oldCompass = compass.copy()
        var newCompass = compass.copy()
        val itemTag = newCompass.orCreateTag.copy()
        itemTag.putBoolean("LodestoneTracked", false)
        itemTag.putString("LodestoneDimension", target.serverWorld.registryKey.value.toString())
        val lodestonePos = CompoundTag()
        lodestonePos.putInt("X", target.x.toInt())
        lodestonePos.putInt("Y", target.y.toInt())
        lodestonePos.putInt("Z", target.z.toInt())
        itemTag.put("LodestonePos", lodestonePos)
        newCompass.tag = itemTag
        newCompass = CompassUpdateCallback.EVENT.invoker().onCompassUpdate(oldCompass, newCompass)

        return newCompass
    }

    fun applyStatusEffectToPlayer(player: PlayerEntity, effect: StatusEffect): Boolean {
        return player.addStatusEffect(StatusEffectInstance(effect, 2, 0, false, false))
    }
}