package com.nhowe.ember.core.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.nhowe.ember.domain.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Tactile feedback for the satisfying moments. Every call is a no-op when haptics are off. */
class Haptics(context: Context, settings: Flow<Settings>, scope: CoroutineScope) {
    private val vibrator: Vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    private val enabled = settings.map { it.hapticsEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    fun tick() = play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    fun click() = play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    fun heavy() = play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))

    /** Goal completed: a crisp double tap. */
    fun success() = play(VibrationEffect.createWaveform(longArrayOf(0, 18, 60, 28), intArrayOf(0, 140, 0, 255), -1))

    /** Perfect day or milestone: a rising rumble. */
    fun celebrate() {
        if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_THUD)) {
            play(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.8f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f, 60)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f, 120)
                    .compose()
            )
        } else {
            play(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 120), intArrayOf(0, 120, 0, 180, 0, 255), -1))
        }
    }

    private fun play(effect: VibrationEffect) {
        if (!enabled.value || !vibrator.hasVibrator()) return
        runCatching { vibrator.vibrate(effect) }
    }
}
