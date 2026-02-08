// com/antechrist/adherentsapp/data/model/dto/KihonEnums.kt
package com.antechrist.adherentsapp.data.model

import androidx.annotation.Keep

@Keep
enum class TechniqueKind { DEFENSE, PUNCH, KICK, POSITION }

@Keep
enum class Movement {
    FORWARD, BACKWARD, LATERAL, CHASSE_FORWARD, CHASSE_BACK, TIRES_FORWARD, TIRES_BACK, PIVOT_90_IN, PIVOT_90_OUT, PIVOT_180
}

@Keep
enum class Side { LEFT, RIGHT }

@Keep
enum class Hand { FRONT, REAR }

@Keep
enum class Foot { FRONT, REAR }

@Keep
enum class Height { JODAN, CHUDAN, GEDAN }

@Keep
enum class Direction { FORWARD, BACKWARD, LATERAL }

@Keep
enum class Coordination { SAME_ARM, SAME_LEG, OPPOSITE_ARM, OPPOSITE_LEG, NONE }

@Keep
enum class Tempo { ONE_COUNT, TWO_COUNT, BURST, SLOW_TO_FAST }

@Keep
enum class SequenceStatus { DRAFT, READY, PUBLISHED }
