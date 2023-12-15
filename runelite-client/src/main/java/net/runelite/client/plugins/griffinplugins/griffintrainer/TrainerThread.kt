package net.runelite.client.plugins.griffinplugins.griffintrainer

import net.runelite.api.Skill
import net.runelite.client.plugins.griffinplugins.griffintrainer.models.PlayTimer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.combat.CombatTrainer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.fishing.FishingTrainer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.mining.MiningTrainer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.woodcutting.WoodcuttingTrainer
import net.runelite.client.plugins.microbot.Microbot
import net.runelite.client.plugins.microbot.util.Global

class TrainerThread(private val config: GriffinTrainerConfig) : Thread() {

    companion object {
        val overallTimer = PlayTimer()
        val taskTimer = PlayTimer()
        var currentTask = ""
        var countLabel = ""
        var count = 0
        var randomEventDismissedCount = 0
        var currentWorldNumber = 0
    }

    private enum class TrainerScripts {
        MINING, COMBAT, FISHING, WOODCUTTING
    }

    private lateinit var miningTrainer: MiningTrainer
    private lateinit var combatTrainer: CombatTrainer
    private lateinit var fishingTrainer: FishingTrainer
    private lateinit var woodcuttingTrainer: WoodcuttingTrainer

    override fun run() {
        miningTrainer = MiningTrainer(config)
        combatTrainer = CombatTrainer(config)
        fishingTrainer = FishingTrainer(config)
        woodcuttingTrainer = WoodcuttingTrainer(config)

        overallTimer.setRandomTimeout(config.minTotalTime(), config.maxTotalTime())
        overallTimer.start()

        currentWorldNumber = Microbot.getClientForKotlin().world

        var trainerSchedule = getTrainerSchedule()

        while (!overallTimer.isTimerComplete) {
            if (isInterrupted) return

            if (trainerSchedule.isEmpty()) {
                trainerSchedule = getTrainerSchedule()
            }

            if (trainerSchedule.isEmpty()) {
                break
            }

            val trainerToRun = trainerSchedule.removeAt(0)
            currentTask = trainerToRun.toString()

            taskTimer.reset()
            taskTimer.setRandomTimeout(config.minTaskTime(), config.maxTaskTime())
            taskTimer.start()

            while (!taskTimer.isTimerComplete) {
                if (isInterrupted) return

                if (runTrainer(trainerToRun)) {
                    break
                }

                Global.sleep(200, 250)
            }
        }
    }

    private fun runTrainer(trainerToRun: TrainerScripts): Boolean {
        return when (trainerToRun) {
            TrainerScripts.MINING -> miningTrainer.run()
            TrainerScripts.COMBAT -> combatTrainer.run()
            TrainerScripts.FISHING -> fishingTrainer.run()
            TrainerScripts.WOODCUTTING -> woodcuttingTrainer.run()
            else -> false
        }
    }

    private fun getTrainerSchedule(): MutableList<TrainerScripts> {
        val schedule = mutableListOf<TrainerScripts>()

        val attackLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.ATTACK)
        val strengthLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.STRENGTH)
        val defenceLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.DEFENCE)
        val miningLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.MINING)
        val fishingLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.FISHING)
        val woodcuttingLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.WOODCUTTING)

        if (config.trainCombat() && (attackLevel < config.attackLevel() || strengthLevel < config.strengthLevel() || defenceLevel < config.defenceLevel())) {
            schedule.add(TrainerScripts.COMBAT)
        }

        if (config.trainMining() && miningLevel < config.miningLevel()) {
            schedule.add(TrainerScripts.MINING)
        }

        if (config.trainFishing() && fishingLevel < config.fishingLevel()) {
            schedule.add(TrainerScripts.FISHING)
        }

        if (config.trainWoodcutting() && woodcuttingLevel < config.woodcuttingLevel()) {
            schedule.add(TrainerScripts.WOODCUTTING)
        }

        schedule.shuffle()
        return schedule
    }
}