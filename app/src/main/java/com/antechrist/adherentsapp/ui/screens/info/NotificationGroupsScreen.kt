//package com.antechrist.adherentsapp.ui.screens.info
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.antechrist.adherentsapp.domain.model.Adherent
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NotificationGroupsScreen(
//    onBack: () -> Unit,
//    currentUserUid: String,
//    allAdherents: List<Adherent>
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Groupes de diffusion") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) { Text("<") }
//                }
//            )
//        }
//    ) { padding ->
//        if (allAdherents.isEmpty()) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    "Aucun adhérent fourni pour l'instant.\n(uid: $currentUserUid)",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(allAdherents) { adherent ->
//                    Card(modifier = Modifier.fillMaxWidth()) {
//                        Column(modifier = Modifier.padding(12.dp)) {
//                            Text(text = "Adhérent: ${adherent.toString()}")
//                            Text(text = "Groupes: (à implémenter)")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
