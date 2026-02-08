// com/antechrist/adherentsapp/data/seed/KihonSeed.kt
package com.antechrist.adherentsapp.data.seed

import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.usecase.SeedKihonCatalogIfEmpty
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KihonSeed @Inject constructor() : SeedKihonCatalogIfEmpty.KihonSeedProvider {
    override fun getSeed(): List<TechniqueRef> = buildList {
        // ─── POSITIONS ───
        add(pos("HEIKO_DACHI", "HEIKO DACHI", "Debout pieds écartés et paralléles"))
        add(pos("HEISOKU_DACHI","HEISOKU DACHI","Debout les pieds l’un contre l’autre"))
        add(pos("MUSUBI_DACHI","MUSUBI DACHI","Debout, talons joints, pointes des pieds écartées"))
        add(pos("ZENKUTSU_DACHI","ZENKUTSU DACHI","Grande fente avant"))
        add(pos("MOTO_DACHI","MOTO DACHI","Petite fente avant"))
        add(pos("KOKUTSU_DACHI","KOKUTSU DACHI","Fente arrière"))
        add(pos("KIBA_DACHI","KIBA DACHI","Position du cavalier"))
        add(pos("KOSA_DACHI","KOSA/KAKE DACHI","Position pieds croisés", aliases = listOf("KAKE DACHI")))
        add(pos("FUDO_DACHI","FUDO DACHI","Position de combat"))
        add(pos("NEKO_ASHI_DACHI","NEKO ASHI DACHI","Position du chat"))
        add(pos("SHIKO_DACHI","SHIKO DACHI","Position du sumotori"))
        add(pos("SANCHIN_DACHI","SANCHIN DACHI","Position du « sablier »"))
        add(pos("SAGI_ASHI_DACHI","SAGI ASHI DACHI","Debout sur une jambe"))

        // ─── DEFENSES (UKE) ───
        add(uke("GEDAN_BARAI","GEDAN BARAI","Défense basse"))
        add(uke("AGE_UKE","AGE UKE","Défense haute"))
        add(uke("UCHI_UKE","UCHI UKE","Défense de l’intérieur vers l’extérieur"))
        add(uke("SOTO_UKE","SOTO UKE","Défense de l’extérieur vers l’intérieur"))
        add(uke("SHUTO_UKE","SHUTO UKE","Défense avec le tranchant de la main"))
        add(uke("HAISHU_UKE","HAISHU UKE","Défense avec le dos de la main"))
        add(uke("TEISHO_UKE","TEISHO UKE","Défense avec la paume"))
        add(uke("JUJI_UKE","JUJI UKE","Défense avec les deux bras croisés"))
        add(uke("KAKIWAKE_UKE","KAKIWAKE UKE","Défense double en écartant"))
        add(uke("MOROTE_UKE","MOROTE UKE","Défense double, bras arrière en protection"))
        add(uke("HEIKO_UKE","HEIKO UKE","Défense double avec les deux bras parallèles"))
        add(uke("SUKUI_UKE","SUKUI UKE","Défense en puisant"))
        add(uke("NAGASHI_UKE","NAGASHI UKE","Défense brossée en accompagnant l’attaque"))
        add(uke("OTOSHI_UKE","OTOSHI UKE","Défense en frappant avec l’avant bras vers le bas"))
        add(uke("KOKEN_UKE","KOKEN UKE","Défense avec le poignet"))

        // ─── POINGS (ZUKI) ───
        add(zuki("CHOKU_ZUKI","CHOKU ZUKI","Coup de poing fondamental"))
        add(zuki("GYAKU_ZUKI","GYAKU ZUKI","Coup de poing bras inverse à la jambe avant"))
        add(zuki("OI_ZUKI","OÏ ZUKI","Coup de poing en avançant (même bras/jambe)"))
        add(zuki("MAETE_ZUKI","MAETE ZUKI","Poing avant"))
        add(zuki("KIZAMI_ZUKI","KIZAMI ZUKI","Poing avant en effaçant le buste"))
        add(zuki("NAGASHI_ZUKI","NAGASHI ZUKI","Poing avant en esquivant"))
        add(zuki("TATE_ZUKI","TATE ZUKI","Poing vertical"))
        add(zuki("URA_ZUKI","URA ZUKI","Poing, paume vers le haut"))
        add(zuki("KAGI_ZUKI","KAGI ZUKI","Poing crochet"))
        add(zuki("MAWASHI_ZUKI","MAWASHI ZUKI","Poing circulaire"))
        add(zuki("YAMA_ZUKI","YAMA ZUKI","Poing double, jodan et gedan"))
        add(zuki("MOROTE_ZUKI","MOROTE ZUKI","Poing double au même niveau"))
        add(zuki("AGE_ZUKI","AGE ZUKI","Poing remontant"))
        // mains ouvertes
        add(TechniqueRef(id = "NUKITE",
            kind = TechniqueRef.Kind.PUNCH, // on garde PUNCH (main ouverte), sinon créer KIND.HAND
            nameJa = "NUKITE",
            nameFr = "Attaque directe en pique main ouverte",
            aliases = emptyList(),
            subType = "nukite",
            notes = null
        ))

        // ─── PIEDS (GERI) ───
        add(geri("MAE_GERI","MAE GERI","Coup de pied direct"))
        add(geri("MAWASHI_GERI","MAWASHI GERI","Coup de pied circulaire"))
        add(geri("YOKO_GERI_KEKOMI","YOKO GERI KEKOMI","Latéral défonçant"))
        add(geri("YOKO_GERI_KEAGE","YOKO GERI KEAGE","Latéral remontant"))
        add(geri("SOKUTO_GERI","SOKUTO GERI","Latéral au tranchant"))
        add(geri("URA_MAWASHI_GERI","URA MAWASHI GERI","Revers tournant"))
        add(geri("MIKAZUKI_GERI","MIKAZUKI GERI","En croissant"))
        add(geri("URA_MIKAZUKI_GERI","URA MIKAZUKI","Croissant inverse"))
        add(geri("KAKATO_GERI","KAKATO GERI","Coup de talon haut→bas"))
        add(geri("USHIRO_GERI","USHIRO GERI","Vers l’arrière"))
        add(geri("USHIRO_MAWASHI_GERI","USHIRO MAWASHI GERI","Revers tournant arrière"))
        add(geri("TOBI_GERI","TOBI GERI","Sauté"))
    }

    private fun pos(id: String, ja: String, fr: String, aliases: List<String> = emptyList()) =
        TechniqueRef(id, TechniqueRef.Kind.POSITION, ja, fr, aliases, subType = "dachi")

    private fun uke(id: String, ja: String, fr: String) =
        TechniqueRef(id, TechniqueRef.Kind.DEFENSE, ja, fr, emptyList(), subType = "uke")

    private fun zuki(id: String, ja: String, fr: String) =
        TechniqueRef(id, TechniqueRef.Kind.PUNCH, ja, fr, emptyList(), subType = "zuki")

    private fun geri(id: String, ja: String, fr: String) =
        TechniqueRef(id, TechniqueRef.Kind.KICK, ja, fr, emptyList(), subType = "geri")
}
