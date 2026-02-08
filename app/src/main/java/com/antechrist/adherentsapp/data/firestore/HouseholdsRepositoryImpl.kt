// data/firestore/HouseholdsRepositoryImpl.kt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HouseholdsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : HouseholdsRepository {
    override suspend fun listHouseholds(seasonKey: String?): List<HouseholdLite> {
        // Collection à adapter à votre schéma existant
        val snap = db.collection("cotisation_families")
            .let { ref -> if (seasonKey != null) ref.whereEqualTo("seasonKey", seasonKey) else ref }
            .get().await()
        return snap.documents.mapNotNull { d ->
            HouseholdLite(
                id = d.id,
                label = d.getString("label") ?: d.getString("name") ?: d.id,
                membersCount = (d.getLong("membersCount") ?: 0L).toInt()
            )
        }
    }
}
