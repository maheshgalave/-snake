package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.BillingCycle
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskPriority

class Converters {
    @TypeConverter
    fun fromSubscriptionTier(tier: SubscriptionTier): String = tier.name

    @TypeConverter
    fun toSubscriptionTier(name: String): SubscriptionTier = runCatching {
        SubscriptionTier.valueOf(name)
    }.getOrDefault(SubscriptionTier.STARTER)

    @TypeConverter
    fun fromBillingCycle(cycle: BillingCycle): String = cycle.name

    @TypeConverter
    fun toBillingCycle(name: String): BillingCycle = runCatching {
        BillingCycle.valueOf(name)
    }.getOrDefault(BillingCycle.MONTHLY)

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(name: String): TaskPriority = runCatching {
        TaskPriority.valueOf(name)
    }.getOrDefault(TaskPriority.MEDIUM)

    @TypeConverter
    fun fromGameMode(mode: com.example.data.model.GameMode): String = mode.name

    @TypeConverter
    fun toGameMode(name: String): com.example.data.model.GameMode = runCatching {
        com.example.data.model.GameMode.valueOf(name)
    }.getOrDefault(com.example.data.model.GameMode.CLASSIC)

    @TypeConverter
    fun fromSnakeSkin(skin: com.example.data.model.SnakeSkin): String = skin.name

    @TypeConverter
    fun toSnakeSkin(name: String): com.example.data.model.SnakeSkin = runCatching {
        com.example.data.model.SnakeSkin.valueOf(name)
    }.getOrDefault(com.example.data.model.SnakeSkin.NEON_CYAN)
}
