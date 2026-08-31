package com.example.data.remote

object AuthorProfiles {
    private const val BASE =
        "https://surajit-singha-sisir.github.io/NingshingCheNew/profiles/"

    private val files = listOf(
        "Pratap Singha Profile.png",
        "Provas Chandra Singha Profile.png",
        "Abdul Gaffar Roni.png",
        "Archana Sinha.png",
        "Barmendra-singha-profile.jpg",
        "Chandra Mohon Singha.png",
        "Jitendra Kumar Singha Profile.png",
        "Kanchan Baran Singha.png",
        "Kanchanbaransingha-thumb.png",
        "Ronojit.png",
        "Subhasish Singha Samir.png"
    )

    private val byKeyword = listOf(
        listOf("প্রতাপ", "pratap") to official("Pratap Singha Profile.png"),
        listOf("প্রভাস", "provas", "prabhas") to official("Provas Chandra Singha Profile.png"),
        listOf("গাফফার", "রনি", "gaffar", "roni") to official("Abdul Gaffar Roni.png"),
        listOf("অর্চনা", "archana") to official("Archana Sinha.png"),
        listOf("বর্মেন্দ্র", "barmendra") to official("Barmendra-singha-profile.jpg"),
        listOf("চন্দ্র মোহন", "গীতশ্রী", "chandra mohon", "mohon") to official("Chandra Mohon Singha.png"),
        listOf("জিতেন্দ্র", "jitendra") to official("Jitendra Kumar Singha Profile.png"),
        listOf("কাঞ্চন", "kanchan") to official("Kanchan Baran Singha.png"),
        listOf("রণজিত", "রণজিৎ", "রণজি্ত", "ronojit") to official("Ronojit.png"),
        listOf("শুভাশিস", "subhasish", "samir") to official("Subhasish Singha Samir.png"),
        listOf("উত্তম") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgHhJqkIHB0iEHbPlmgseLcOheqzmsN8Z4ereNL3sRk5ORjOJsI0nMd7rR-736O-jC3tWFkA574sMYkuqfKal6sytKZogCQNEbHpbhGLWQrs4cn3Rc8VC5wY7fh_gYBVM3SfYpFHzA5731hyCtRkKh1ye6iAlyMruCmIQvsCwIslSPMkoGFOfxl3dAW7o9Y/s1200/Kumar-U-Sinha-Profile.png",
        listOf("কাজল") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhxUvVUukxlAFt1b8yccYx9TO_lKoUTGRpOSM23B-IBUqiiAtjKvPsXIRdx_bOtGmnx-RuEu2t62pRHaySRBNYLzr1vXBzPmbUXJQ_y1uv7JJYquKYHJwK-LrfnGaqCx2rSePqE2C_nYyo_6o3fwo9peDytHKGljCCZhTJ4aD0vYaeGRXeo7ogC4U2u278p/s519/Asset_1-removebg-preview.png",
        listOf("সতীর্থ") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEi_kUBhX6Mgyng7y3Lo3is2EhjBUp0TzdE0sElI_LaUsQKdS8nE2YBHSN8CW0Aqd1w_C8S1kgIpPlpaCnYIZkZhWDiwrgO4O1RcVxO5ZjB0g9tHCNRgjgRujDS_ixWG13O-lMT9PnrBpAdQMNtKCSIuFX-GyAvLX-RkqUjmc4runDD1XlrporbP1v6mtxEv/s1200/Satirtha-Singha-Profile.png",
        listOf("হেমন্ত", "হেমতা") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiIESQ7-0EBma8T7eDnsrfLbHQQRs1b00v1i-6pNiUJ1-T0sXpm_yDehyphenhyphen7TXo5XFlLbH7R_gOBEm_AZWuDabLu_6sihbsDUBq11V_59m25OiaTa3fJG_QOIIICrFCu63_Q5gqiLPGeWI_WHSvugRnash4Ik_kgqpPvQh41g4yYlVuhMjq4KxguZldSd4AWP/s1536/Shri-Hemata-Kumar-Singha-Profile.png",
        listOf("সুজিতা") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhh8afb7-KJCMBzHz_cDHcrR7rSVdt28xxkeaofjSa3KyElFVQGCPybgYRVYwJMRNZXNEi-cLRewsq6xPd3DhfIFGnhpwgV8flTmKE3-ZdZLIJ9bmz4D0SoIa98hgTwsTXIHAnSeMir7isxlVlsGhApV4HEPmv4Dc_LQw-v8LOeYb498XJ8jm-K-tGXzxy2/s1200/Sujita-Sinha-Profile.png",
        listOf("দিপা") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgYMe2kW5hI2eP4ZrMUn6whk1BwCMnVKOUVrEn2qsXAS9nnHZHw4XQ1WGz1r378W3CDrGbScL3fnOZrNGqos8NFslDif7WGuEOWOtNND3HrH6I8IMS3VqBVonRo2f-MHS2bHYGPO0rjsWBHVnnNY7CVGlyWTCpaJkzAwASNk2_m3sqewAfdvUo9ze4PPHtI/s1200/Dipa-Sinha-Profile.png",
        listOf("ইমন") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhlYiSutYf97IvlPpk8rQirUEpJPmdcEp8xCeLgk7pbI6GVzjJ5LEgvL5Vjj8xfdY7P3jcPyVzGfAB-fPldHiYxqJ4VgZioNNYzw0UvUC5rwVvqy6h7TZw7eRECE0Z6H4EKccR3DAgjMfFRloLPNLt3-8k73sc2GxSIqSWdUeEd5-HLePV_y566Qw_jjPRR/s531/Emon-Singha-Profile.jpg",
        listOf("সুরজিত", "শিশির") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEg2X4vQgCkkLyllys1YESSzrcXhyphenhyphen-a9kurYS2r6jzgZlBieLYgGgQg9pwSvxgIdUiK0rTBFll__-LmIl1G8lT7oe7zZmBiJBiOcmSXfeL845YthA_yDzyvMUPnerUBMUEv7pXwM9ybiwwlFpEVWXsJLwadOKjget9sfi6RGl9TmZyXX8wyXahyphenhyphenL01mKK1ZH/s533/Surajit-Singha-Sisir-Profile.png",
        listOf("দেবাঞ্জলি") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiIydsi7bYS264RbmYiV8ziAs-Rj9jsiEo2kD1t6p2in5Fx1N1eHYB5Hz3amrSNS1sqcm8zawujAlufaqdLCfRmA_ju5xlikkhrdI3wz9hJs6qUa22s3o0IdLL38mTR1bOHyP9cpcSCB5hZOWvQAKPrABSMW6JeIFDx2GSP-7PKQF_q75U8gWQLORFGBOEF/s574/upscalemedia-transformed%20(2).png",
        listOf("অরুন") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj6ZFLYYbTXsNzqMWxPbYCQzIvP5R3CxbgukpkV6rL42Bx4huuNRJgIRvHn2lyWLTtbgzrnrCpU7H2M2vOqHBDqjFiUUBifLfTP9oE6xRqevPMY_1xDO5zcX6TL0KcXt7N4ZXhQAk0Hs2DMQjhHD1HFgTYWB31tMPUEcJqT51Pgs1Xm0x4XIeiRqclpFreT/s1104/Arun-Kumar-Singha-Profile.png",
        listOf("পলাশ", "হিমাদ্র") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEha5xeDh4rTXNUxrPXBscluZdlXpfkHM1yqoIgiEuImGH8iJ-rJuQNpiC92dnxuD8q6td-Ab-9Du5bRdL2nQR71IBSnmRj3T7G28udfsSYpAhrHRr1N6rHS8pr2R7sHji4_0c3S-yfOF9Ofw9U7uWsyun2tENOJO0FJWTAkRTRf0g_725NdApXz5qmOj0gY/s773/Polash-Singha-Himadra.png",
        listOf("অপর্ণা") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEidUU7bP5UxXMwet4JYgbnjY4Ia1JcVnwP6JcRjW0mEm_CgfY7WjhJinNwutr5UoblhpLInS1Cyz9N1s1bfMJGGMR-zZ8ip8hTuUjft7XrG6xd09Lw0_695tnd3w6LTrsHQTKTcPSuVKbXcK5eZkboMoidcILo4tTzZ9DUy8bazVnmkZDOiiKunivsoBNek/s600/Aporna-Sinha-Profile.png",
        listOf("কুঙ্গ", "থাঙ") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEilCwmdXoZc2nc_Afwo1TRhdeX4qgsT2H-r31U65-vSIJJ-5uJ8aC7qdkbH9EvVqfpj0vL0SW93C4iPKAYu0bQSsObM-w6XL085zUZf4EHiT_GqID0j-33TmgzuLZWFfecSn-3lv-p20Mbda5hAv28N9TR3NuwQvRffM__7QWe8VB3b8H34FFmmFG11A1n8/s1024/male-author-placeholder.png",
        listOf("নবকুমার") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEh5OIceRbWThmZjAifJ69-D8vt1DwGFbNg4EfSy0VW7t97R0ExSinf0O8ESK8GtxpTjmqwhY7wiBg6Vx3JKGMeN9L2Yy6HOo0ehPdQMz8hz3BQU_-0huNOmfVZawvglcIQXYcI37bVtEZ1xYNU8xfT97RvOhnrCBXNwPt8Ci5_bYlfHIyXOiOpIO9vnk0fJ/s877/Adobe%20Express%20-%20file.png",
        listOf("স্বপন") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhdaoM8FqFZVgWzWoK3L4nza6SJBJEOw_OlKvrfyWIrq5Su2HEQjCkJceUrmICinHYIZ9hDxCPVQMbth0J0wSHPTJ35XjmUJZRxLPkz2nBFGGF7KaIW458ieQHwmq3xOJ_UNxH8zzV0c6RgzLsU7Mu8ufKw3mVDYmKUB2kxV5F4FOFWIRHRSr2XyKBEdOa4/s511/Dr.-Swapan-Kumar-Singha-Profile.png",
        listOf("সুকান্ত") to
            "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhGH7bQYuDSsEyRM4u9OVsnK8b7VibniG5uGqBLY2SN-4orEROolj7HY-kcdV-9eNaa6ICsI7g13K3vPlzT2nz9WopI3BkaC2s50qyfKS3RMk0KS7nb8NDwD2473n6zWadzZc3Zso0JKGnkY56Co6U188qVqVa1OGT5Jlcxr14m8hc4Q4Cj1vppLNIS6CvG/s265/sukanta-singha-profile.png"
    )

    fun urlFor(fileName: String): String = official(fileName)

    fun resolve(name: String, existing: String = ""): String {
        val live = existing.replace(" ", "%20")
        if (isUsablePhoto(live)) return live
        val key = foldName(name)
        val mapped = byKeyword.firstOrNull { (keys, _) ->
            keys.any { key.contains(foldName(it)) }
        }?.second
        return mapped ?: live
    }

    fun isOfficial(url: String): Boolean =
        url.contains("/NingshingCheNew/profiles/", ignoreCase = true) &&
            !url.contains("%E0%")

    fun isUsablePhoto(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (lower.contains("logo") && !lower.contains("profile")) return false
        return lower.startsWith("http")
    }

    fun foldName(name: String): String =
        name.lowercase()
            .replace("ডা.", "")
            .replace("ড.", "")
            .replace("শ্রী", "")
            .replace("\u200c", "")
            .replace(Regex("""\s+"""), "")

    private fun official(fileName: String): String = BASE + fileName.replace(" ", "%20")
}

object PdfCovers {
    val byReadId: Map<Int, String> = mapOf(
        1 to "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEgfFmmLDBHSEuPVLEyZxKBhnSZGXRoohaRHQl7fpUnmj0mGydBnlscam9Cjj73xJQhU4QCsB8oI36w4EHcUuWixygvFRTnMLCZ0g62YDUzQMJSwtDu9F_Ip01IbrRTWuMwp25v3aoWngGIhIorUlRoCrfhmujTx0VAiX-XqiQQeeDs8MC04rHRSgs37_Os4/s1035/NingshingChe-2024-PDF-Online.jpg",
        2 to "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhIePuf-Ausr7DMrseqNIgmr5XtYOK7Th9l2p7J1-hDOVE3ggXfex9nCAbICf0hOzrNn19dBw4patimxTwxHlLvy5gdRNMdTkzKgijwofw9KRUbO1Xq4d5JIB5DQvbFgCYexQfz5Z9HYQ383YTpR9rxrWHux-NblIJS6WeCuTAeRmM_XGp3bweurcX0lZCr/s888/NingshingChe-2023-PDF-Online.png",
        3 to "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj4Tq6GIjEUCe0TG2SyICE1IvzpbIjeGSwS9ZghrP6DjKHs-1hYL_lQaakVQGLk2TmeErvktTOhmgJofAu3W9atT3nryFWm9rtjXVdmzFKKDT3TLCOe10AUnuXPDHzUwJ0RNYocPFuW7lG6qmhfQCsuAbSdZ5xDYSH2cPmMTwpcYdyMqU6C8f3SmVSrtO4n/s1063/NingshingChe-2018-PDF-Online.jpg",
        4 to "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiL3xg8yIEcw41MiLPHe4SpqqhGE9wUNsvu8zGjt4NKKsYRqxJOmeGlL6LZreAeGAx7jfHKOs9_cd_hI7i3GiPK0E4YclQPmZ-bIZL7hv63GTr_RkIIboCRn1nNRJHgUsxMaqZqsGl5IbrNnqw4ng1HIjH76_-IrmYE7hp3MtWTjb9TgYRdzciCLWs5qe0d/s4361/Ningshingche-2022-thumbnail.jpg",
        5 to "https://surajit-singha-sisir.github.io/NingshingCheNew/PDF/%E0%A6%AE%E0%A6%B9%E0%A6%BE%E0%A6%AE%E0%A7%87%E0%A6%B2-%E0%A7%A8%E0%A7%A6%E0%A7%A7%E0%A7%A9.jpg",
        6 to "https://surajit-singha-sisir.github.io/NingshingCheNew/PDF/%E0%A6%95%E0%A7%81%E0%A6%AE%E0%A7%87%E0%A6%87-%E0%A7%A8%E0%A7%A6%E0%A7%A6%E0%A7%AF-%E0%A6%B8%E0%A6%82%E0%A6%96%E0%A7%8D%E0%A6%AF%E0%A6%BE.jpg"
    )
}
