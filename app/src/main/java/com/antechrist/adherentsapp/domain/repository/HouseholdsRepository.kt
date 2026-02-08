// domain/repository/HouseholdsRepository.kt
interface HouseholdsRepository {
    suspend fun listHouseholds(seasonKey: String?): List<HouseholdLite>
}

data class HouseholdLite(
    val id: String,
    val label: String,   // ex: "Famille Dupont"
    val membersCount: Int
)
