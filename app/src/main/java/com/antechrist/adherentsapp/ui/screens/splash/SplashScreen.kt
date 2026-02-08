//package com.antechrist.adherentsapp.ui.screens.splash
//
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.hilt.navigation.compose.hiltViewModel
//
//@Composable
//fun SplashScreen(
//    toLogin: () -> Unit,
//    toList: () -> Unit,
//    toDenied: () -> Unit,
//    vm: SplashViewModel = hiltViewModel()
//) {
//    val nav by vm.nav.collectAsState()
//
//    LaunchedEffect(Unit) { vm.check() }
//    LaunchedEffect(nav) {
//        when (nav) {
//            is SplashNav.ToLogin -> toLogin()
//            is SplashNav.ToList -> toList()
//            is SplashNav.ToDenied -> toDenied()
//            else -> {}
//        }
//    }
//
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        CircularProgressIndicator()
//    }
//}
package com.antechrist.adherentsapp.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    toLogin: () -> Unit,
    toList: () -> Unit,
    toDenied: () -> Unit,
    toParentHome: () -> Unit,  // ← NOUVEAU
    vm: SplashViewModel = hiltViewModel()
) {
    val nav by vm.nav.collectAsState()

    LaunchedEffect(Unit) { vm.check() }
    LaunchedEffect(nav) {
        when (nav) {
            is SplashNav.ToLogin -> toLogin()
            is SplashNav.ToList -> toList()
            is SplashNav.ToDenied -> toDenied()
            is SplashNav.ToParentHome -> toParentHome()  // ← NOUVEAU
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}