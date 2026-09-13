package com.rssf.reader.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalDrawer
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rssf.reader.data.Category
import com.rssf.reader.data.Entry
import com.rssf.reader.data.Feed
import kotlinx.coroutines.launch

private val Page = Color(0xFF101010)
private val Panel = Color(0xFF191919)
private val DividerColor = Color(0xFF252525)
private val Muted = Color(0xFF9E9E9E)

private enum class AdminDialogKind { NewCategory, NewFeed, EditCategory, EditFeed }

@Composable
fun ReaderApp(context: Context, model: ReaderViewModel = viewModel(factory = ReaderViewModel.factory(context))) {
    val authorized by model.authorized.collectAsState()
    val message by model.message.collectAsState()
    when (authorized) {
        null -> Box(Modifier.fillMaxSize().background(Page))
        false -> LoginScreen(message) { server, user, pass -> model.login(server, user, pass) }
        true -> ReaderHome(model)
    }
}

@Composable
private fun LoginScreen(message: String?, onLogin: (String, String, String) -> Unit) {
    var server by remember { mutableStateOf("rsse.muxi.de") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Page).padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("RSSF", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
        Text("Your reading, in one quiet place", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))
        OutlinedTextField(
            server,
            { server = it },
            Modifier.fillMaxWidth(),
            label = { Text("Server") },
            placeholder = { Text("rsse.muxi.de") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = loginFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true, colors = loginFieldColors())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password,
            { password = it },
            Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton({ passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle password visibility")
                }
            },
            colors = loginFieldColors()
        )
        if (!message.isNullOrBlank()) Text(message, color = Color(0xFFEF5350), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { onLogin(server, username, password) }, enabled = server.isNotBlank() && username.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("SIGN IN") }
    }
}

@Composable
private fun loginFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    cursorColor = Color(0xFF03A9F4),
    focusedBorderColor = Color(0xFF03A9F4),
    unfocusedBorderColor = Color(0xFF777777),
    focusedLabelColor = Color(0xFF03A9F4),
    unfocusedLabelColor = Color(0xFFBDBDBD),
    placeholderColor = Color(0xFF9E9E9E)
)

@Composable
private fun ReaderHome(model: ReaderViewModel) {
    val drawer = androidx.compose.material.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val categories by model.categories.collectAsState()
    val feeds by model.feeds.collectAsState()
    val entries by model.entries.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("Today") }
    var adminDialog by remember { mutableStateOf<AdminDialogKind?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedFeed by remember { mutableStateOf<Feed?>(null) }
    ModalDrawer(drawerState = drawer, drawerContent = {
        ReaderDrawer(
            categories,
            feeds,
            editMode,
            { editMode = !editMode },
            { editMode = false },
            { title = it; scope.launch { drawer.close() } },
            { model.logout() },
            { adminDialog = AdminDialogKind.NewCategory },
            { adminDialog = AdminDialogKind.NewFeed },
            { selectedCategory = it; adminDialog = AdminDialogKind.EditCategory },
            { selectedFeed = it; adminDialog = AdminDialogKind.EditFeed }
        )
    }) {
        Scaffold(topBar = {
            TopAppBar(backgroundColor = Panel, title = {
                if (searchOpen) OutlinedTextField(query, { query = it; model.search(it) }, Modifier.fillMaxWidth(), placeholder = { Text("Search articles") }, singleLine = true)
                else Text(title, color = Color.White, fontSize = 20.sp)
            }, navigationIcon = { IconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") } }, actions = {
                IconButton({ searchOpen = !searchOpen; if (!searchOpen) { query = ""; model.refresh() } }) { Icon(if (searchOpen) Icons.Default.ArrowBack else Icons.Default.Search, "Search") }
                IconButton({ model.refresh() }) { Icon(Icons.Default.MoreVert, "More") }
            })
        }) { padding -> ArticleList(entries, Modifier.padding(padding)) }
    }
    when (adminDialog) {
        AdminDialogKind.NewCategory -> AdminDialog("New category", "Name", "", { value -> model.createCategory(value) }, { adminDialog = null })
        AdminDialogKind.NewFeed -> AdminDialog("Add feed", "RSS URL", "", { value -> model.createFeed(value, null) }, { adminDialog = null })
        AdminDialogKind.EditCategory -> selectedCategory?.let { category -> AdminDialog("Edit category", "Name", category.name, { value -> model.renameCategory(category.id, value) }, { adminDialog = null }, { model.deleteCategory(category.id) }) }
        AdminDialogKind.EditFeed -> selectedFeed?.let { feed -> AdminDialog("Edit feed", "Title", feed.title, { value -> model.renameFeed(feed.id, value) }, { adminDialog = null }, { model.deleteFeed(feed.id) }) }
        null -> Unit
    }
}

@Composable
private fun ReaderDrawer(
    categories: List<Category>, feeds: List<Feed>, editMode: Boolean, toggleEdit: () -> Unit, done: () -> Unit,
    navigate: (String) -> Unit, logout: () -> Unit, addCategory: () -> Unit, addFeed: () -> Unit,
    editCategory: (Category) -> Unit, editFeed: (Feed) -> Unit
) {
    Column(Modifier.fillMaxHeight().width(320.dp).background(Page)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RSSF", color = Color.White, fontSize = 21.sp)
            Text(if (editMode) "Done" else "Edit", color = Color(0xFF03A9F4), modifier = Modifier.clickable { if (editMode) done() else toggleEdit() })
        }
        DrawerItem("Today", Icons.Default.Check) { navigate("Today") }
        DrawerItem("Read Later", Icons.Default.BookmarkBorder) { navigate("Read Later") }
        DrawerItem("Starred", Icons.Default.StarBorder) { navigate("Starred") }
        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FEEDS", color = Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (editMode) Row {
                Text("Category", color = Color(0xFF03A9F4), modifier = Modifier.clickable(onClick = addCategory).padding(end = 12.dp))
                Text("Feed", color = Color(0xFF03A9F4), modifier = Modifier.clickable(onClick = addFeed))
            }
        }
        DrawerItem("All", Icons.Default.Menu) { navigate("All") }
        categories.forEach { category -> DrawerItem(category.name, Icons.Default.ArrowBack, category.unread) { if (editMode) editCategory(category) else navigate(category.name) } }
        if (categories.isEmpty()) listOf("News", "Gadgets", "Develop").forEach { DrawerItem(it, Icons.Default.ArrowBack) { navigate(it) } }
        feeds.forEach { feed -> DrawerItem(feed.title, Icons.Default.BookmarkBorder, feed.unread) { if (editMode) editFeed(feed) else navigate(feed.title) } }
        if (editMode) { Spacer(Modifier.weight(1f)); Text("Sign out", color = Color(0xFFEF5350), modifier = Modifier.padding(24.dp).clickable { logout() }) }
    }
}

@Composable
private fun AdminDialog(title: String, label: String, initial: String, submit: (String) -> Unit, dismiss: () -> Unit, delete: (() -> Unit)? = null) {
    var value by remember(title, initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = {
            Row {
                if (delete != null) TextButton(onClick = { delete(); dismiss() }) { Text("DELETE", color = Color(0xFFEF5350)) }
                TextButton(onClick = { if (value.isNotBlank()) { submit(value.trim()); dismiss() } }) { Text("SAVE") }
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int = 0, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.width(28.dp)); Text(label, color = Color(0xFFD4D4D4), fontSize = 18.sp, modifier = Modifier.weight(1f)); if (count > 0) Text(if (count > 999) "1K+" else count.toString(), color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun ArticleList(entries: List<Entry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) Box(modifier.fillMaxSize().background(Page), contentAlignment = Alignment.Center) { Text("Nothing new here", color = Muted) }
    else LazyColumn(modifier.fillMaxSize().background(Page)) { items(entries, key = { it.id }) { ArticleRow(it) } }
}

@Composable
private fun ArticleRow(entry: Entry) {
    Column(Modifier.fillMaxWidth().clickable { }.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(entry.feedTitle.uppercase(), color = Color(0xFF03A9F4), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(entry.published, color = Muted, fontSize = 11.sp) }
        Text(entry.title, color = if (entry.isRead) Muted else Color.White, fontSize = 18.sp, fontWeight = if (entry.isRead) FontWeight.Normal else FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
        if (entry.summary.isNotBlank()) Text(entry.summary, color = Muted, fontSize = 14.sp, maxLines = 2, modifier = Modifier.padding(top = 5.dp))
        Divider(color = DividerColor, modifier = Modifier.padding(top = 15.dp))
    }
}
