package com.example.focusticks.ui.theme.Screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.DiscussionItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(nav: NavHostController, topicId: String) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var topic by remember { mutableStateOf<Topic?>(null) }
    var description by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf(listOf<DiscussionItem>()) }
    var input by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DiscussionItem?>(null) }

    DisposableEffect(Unit) {
        val topicListener = db.collection("topics").document(topicId)
            .addSnapshotListener { snap, _ ->
                val t = snap?.toObject(Topic::class.java)
                if (t != null) {
                    topic = t.copy(id = topicId)
                    description = t.description
                }
            }

        val commentListener = db.collection("topics").document(topicId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                comments = snap?.documents?.mapNotNull { d ->
                    d.toObject(DiscussionItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }

        onDispose {
            topicListener.remove()
            commentListener.remove()
        }
    }

    fun saveDescription() {
        db.collection("topics").document(topicId)
            .set(mapOf("description" to description.trim()), SetOptions.merge())
    }

    fun sendComment() {
        if (input.isBlank()) return
        db.collection("users").document(uid).get().addOnSuccessListener { u ->
            val name = u.getString("name") ?: "User"
            val data = DiscussionItem(
                topicId = topicId,
                text = input.trim(),
                uid = uid,
                userName = name,
                timestamp = Timestamp.now(),
                replyToId = replyTarget?.id
            )
            db.collection("topics").document(topicId)
                .collection("comments")
                .add(data)
            input = ""
            replyTarget = null
        }
    }

    fun deleteComment(id: String) {
        db.collection("topics").document(topicId)
            .collection("comments")
            .document(id)
            .delete()
    }

    @Composable
    fun RenderComment(item: DiscussionItem, depth: Int) {
        val replies = comments.filter { it.replyToId == item.id }
        val isMe = item.uid == uid
        val df = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 20).dp, top = 8.dp, bottom = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (isMe) "${item.userName} (Me)" else item.userName,
                        color = Color(0xFF1976D2),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(item.text)
                    Spacer(Modifier.height(4.dp))
                    item.timestamp?.let {
                        Text(
                            df.format(it.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Reply",
                            color = Color(0xFF1976D2),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { replyTarget = item }
                        )
                        if (isMe) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { deleteComment(item.id) }
                            )
                        }
                    }
                }
            }

            replies.forEach { reply ->
                RenderComment(reply, depth + 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEDE7F6)
                )
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { saveDescription() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
                ) {
                    Text("Save")
                }
            }

            Text(
                "Comments",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(comments.filter { it.replyToId == null }) { parent ->
                    RenderComment(parent, 0)
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = {
                        Text(
                            if (replyTarget != null)
                                "Replying to ${replyTarget!!.userName}"
                            else
                                "Add a comment"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { sendComment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
                ) {
                    Text("Post")
                }
            }
        }
    }
}
