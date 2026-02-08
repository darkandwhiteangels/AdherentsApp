package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.ui.utils.normalize
import javax.inject.Inject

class SearchAdherentsUseCase @Inject constructor() {
    operator fun invoke(source: List<Adherent>, query: String): List<Adherent> {
        val q = normalize(query.trim())
        if (q.isEmpty()) return source
        return source.filter { a ->
            val n = normalize(a.nom)
            val p = normalize(a.prenom)
            val e = normalize(a.email.orEmpty())
            n.contains(q) || p.contains(q) || e.contains(q)
        }
    }
}
