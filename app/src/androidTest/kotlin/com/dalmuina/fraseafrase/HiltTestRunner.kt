package com.dalmuina.fraseafrase

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Runner de instrumentados para los tests de cableado de Hilt (design.md D2). Hilt necesita que la
 * aplicación de test sea [HiltTestApplication]; el resto del comportamiento es el de [AndroidJUnitRunner].
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
