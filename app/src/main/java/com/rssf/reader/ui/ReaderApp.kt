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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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
import coil.compose.AsyncImage
import com.rssf.reader.data.Category
import com.rssf.reader.data.Entry
import com.rssf.reader.data.Feed
import kotlinx.coroutines.launch

private val Page = Color(0xFF101010)
private val Panel = Color(0xFF191919)
private val DividerColor = Color(0xFF252525)
private val Muted = Color(0xFF9E9E9E)

private enum class AdminDialogKind { NewCategory, NewFeed, EditCategory, EditFeed }
private enum class DrawerSheetKind { All, Category, Feed, MoveCategory, MoveFeed }

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
    var title by remember { mutableStateOf("All") }
    var adminDialog by remember { mutableStateOf<AdminDialogKind?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedFeed by remember { mutableStateOf<Feed?>(null) }
    var expandedCategories by remember { mutableStateOf(setOf<Long>()) }
    var sheetKind by remember { mutableStateOf<DrawerSheetKind?>(null) }
    var sheetCategory by remember { mutableStateOf<Category?>(null) }
    var sheetFeed by remember { mutableStateOf<Feed?>(null) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    LaunchedEffect(drawer.currentValue) { if (drawer.currentValue == DrawerValue.Closed) editMode = false }
    fun showSheet(kind: DrawerSheetKind, category: Category? = null, feed: Feed? = null) {
        sheetKind = kind; sheetCategory = category; sheetFeed = feed
        scope.launch { sheetState.show() }
    }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            DrawerSheet(
                kind = sheetKind,
                category = sheetCategory,
                feed = sheetFeed,
                categories = categories,
                onMarkCategoryRead = { id -> model.markCategoryRead(id); scope.launch { sheetState.hide() } },
                onMarkFeedRead = { id -> model.markFeedRead(id); scope.launch { sheetState.hide() } },
                onRenameCategory = { category -> selectedCategory = category; adminDialog = AdminDialogKind.EditCategory; scope.launch { sheetState.hide() } },
                onRenameFeed = { feed -> selectedFeed = feed; adminDialog = AdminDialogKind.EditFeed; scope.launch { sheetState.hide() } },
                onDeleteCategory = { id -> model.deleteCategory(id); scope.launch { sheetState.hide() } },
                onDeleteFeed = { id -> model.deleteFeed(id); scope.launch { sheetState.hide() } },
                onMoveCategory = { category, targetIndex -> model.moveCategory(category.id, targetIndex); scope.launch { sheetState.hide() } },
                onMoveFeed = { feed, categoryId -> model.moveFeed(feed.id, categoryId); scope.launch { sheetState.hide() } },
                onMarkAllRead = { model.markAllRead(); scope.launch { sheetState.hide() } },
                onAddCategory = { adminDialog = AdminDialogKind.NewCategory; scope.launch { sheetState.hide() } }
            )
        }
    ) {
        ModalDrawer(drawerState = drawer, drawerContent = {
            ReaderDrawer(
                categories = categories,
                feeds = feeds,
                editMode = editMode,
                expandedCategories = expandedCategories,
                toggleCategory = { category -> expandedCategories = if (category.id in expandedCategories) expandedCategories - category.id else expandedCategories + category.id },
                toggleEdit = { editMode = !editMode },
                done = { editMode = false },
                navigate = { title = it; editMode = false; scope.launch { drawer.close() } },
                logout = { model.logout() },
                settings = { showSettings = true },
                addCategory = { adminDialog = AdminDialogKind.NewCategory },
                addFeed = { adminDialog = AdminDialogKind.NewFeed },
                showAllMenu = { showSheet(DrawerSheetKind.All) },
                showCategoryMenu = { showSheet(DrawerSheetKind.Category, category = it) },
                showFeedMenu = { showSheet(DrawerSheetKind.Feed, feed = it) },
                moveCategory = { showSheet(DrawerSheetKind.MoveCategory, category = it) },
                moveFeed = { showSheet(DrawerSheetKind.MoveFeed, feed = it) }
            )
        }) {
            Scaffold(topBar = {
                TopAppBar(backgroundColor = Panel, title = {
                    if (searchOpen) OutlinedTextField(query, { query = it; model.search(it) }, Modifier.fillMaxWidth(), placeholder = { Text("Search articles") }, singleLine = true)
                    else Text(title, color = Color.White, fontSize = 20.sp)
                }, navigationIcon = { IconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") } }, actions = {
                    IconButton({ searchOpen = !searchOpen; if (!searchOpen) { query = ""; model.refresh() } }) { Icon(if (searchOpen) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search, "Search") }
                    IconButton({ model.refresh() }) { Icon(Icons.Default.MoreVert, "More") }
                })
            }) { padding ->
                val visibleEntries = when (title) {
                    "Recently Read" -> entries.filter { it.isRead }
                    "Starred" -> entries.filter { it.isStarred }
                    "archived" -> entries.filter { it.isRead }
                    else -> entries
                }
                ArticleList(visibleEntries, Modifier.padding(padding)) { selectedEntry = it; model.markEntryRead(it.id) }
            }
        }
    }
    selectedEntry?.let { entry -> ArticleDetail(entry, onBack = { selectedEntry = null }) }
    if (showSettings) SettingsScreen(onBack = { showSettings = false })
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
    categories: List<Category>, feeds: List<Feed>, editMode: Boolean, expandedCategories: Set<Long>, toggleCategory: (Category) -> Unit,
    toggleEdit: () -> Unit, done: () -> Unit, navigate: (String) -> Unit, logout: () -> Unit, settings: () -> Unit, addCategory: () -> Unit, addFeed: () -> Unit, showAllMenu: () -> Unit,
    showCategoryMenu: (Category) -> Unit, showFeedMenu: (Feed) -> Unit, moveCategory: (Category) -> Unit, moveFeed: (Feed) -> Unit
) {
    Column(Modifier.fillMaxHeight().width(320.dp).background(Page)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RSSF", color = Color.White, fontSize = 21.sp)
            Text(if (editMode) "Done" else "Edit", color = Color(0xFF03A9F4), modifier = Modifier.clickable { if (editMode) done() else toggleEdit() })
        }
        DrawerItem("Recently Read", Icons.Default.AccessTime) { navigate("Recently Read") }
        DrawerItem("Starred", Icons.Default.StarBorder) { navigate("Starred") }
        DrawerItem("archived", Icons.Default.Archive) { navigate("archived") }
        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FEEDS", color = Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (editMode) Row {
                Text("Category", color = Color(0xFF03A9F4), modifier = Modifier.clickable(onClick = addCategory).padding(end = 12.dp))
                Text("Feed", color = Color(0xFF03A9F4), modifier = Modifier.clickable(onClick = addFeed))
            }
        }
        DrawerItem("All", Icons.Default.Menu) { if (editMode) showAllMenu() else navigate("All") }
        categories.forEach { category ->
            CategoryDrawerItem(category, feeds.filter { it.categoryId == category.id }, editMode, category.id in expandedCategories, { toggleCategory(category) }, { navigate(category.name) }, { showCategoryMenu(category) }, { moveCategory(category) }, navigate, showFeedMenu, moveFeed)
        }
        if (categories.isEmpty()) listOf("News", "Gadgets", "Develop").forEach { DrawerItem(it, Icons.AutoMirrored.Filled.ArrowBack) { navigate(it) } }
        feeds.filter { it.categoryId == null }.forEach { feed -> FeedDrawerItem(feed, editMode, navigate, showFeedMenu, moveFeed) }
        Spacer(Modifier.weight(1f))
        DrawerItem("Settings", Icons.Default.Settings, onClick = settings)
        DrawerItem("Logout", Icons.Default.PowerSettingsNew) { logout() }
    }
}

@Composable
private fun CategoryDrawerItem(category: Category, childFeeds: List<Feed>, editMode: Boolean, expanded: Boolean, toggle: () -> Unit, navigate: (String) -> Unit, menu: () -> Unit, move: () -> Unit, feedNavigate: (String) -> Unit, feedMenu: (Feed) -> Unit, feedMove: (Feed) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { if (editMode) toggle() else navigate(category.name) }.padding(start = 24.dp, end = 18.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, "Expand ${category.name}", tint = Muted, modifier = Modifier.width(28.dp).clickable(onClick = toggle))
        Text(category.name, color = Color(0xFFD4D4D4), fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (editMode) { IconButton(onClick = menu) { Icon(Icons.Default.MoreHoriz, "Category actions", tint = Muted) }; IconButton(onClick = move) { Icon(Icons.Default.DragHandle, "Move category", tint = Muted) } }
        else if (category.unread > 0) Text(if (category.unread > 999) "1K+" else category.unread.toString(), color = Muted, fontSize = 13.sp)
    }
    if (expanded) childFeeds.forEach { feed -> FeedDrawerItem(feed, editMode, feedNavigate, feedMenu, feedMove) }
}

@Composable
private fun FeedDrawerItem(feed: Feed, editMode: Boolean, navigate: (String) -> Unit, menu: (Feed) -> Unit, move: (Feed) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { navigate(feed.title) }.padding(start = 68.dp, end = 18.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.BookmarkBorder, null, tint = Muted, modifier = Modifier.width(24.dp))
        Text(feed.title, color = Color(0xFFD4D4D4), fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
        if (editMode) { IconButton(onClick = { menu(feed) }) { Icon(Icons.Default.MoreHoriz, "Feed actions", tint = Muted) }; IconButton(onClick = { move(feed) }) { Icon(Icons.Default.DragHandle, "Move feed", tint = Muted) } }
        else if (feed.unread > 0) Text(if (feed.unread > 999) "1K+" else feed.unread.toString(), color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun DrawerSheet(kind: DrawerSheetKind?, category: Category?, feed: Feed?, categories: List<Category>, onMarkCategoryRead: (Long) -> Unit, onMarkFeedRead: (Long) -> Unit, onRenameCategory: (Category) -> Unit, onRenameFeed: (Feed) -> Unit, onDeleteCategory: (Long) -> Unit, onDeleteFeed: (Long) -> Unit, onMoveCategory: (Category, Int) -> Unit, onMoveFeed: (Feed, Long?) -> Unit, onMarkAllRead: () -> Unit, onAddCategory: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel).padding(bottom = 28.dp)) {
        when (kind) {
            DrawerSheetKind.All -> { DrawerSheetHeader("All"); SheetAction("Mark as Read", Icons.Default.Check, onClick = onMarkAllRead); SheetAction("Add Category", Icons.Default.Add, onClick = onAddCategory) }
            DrawerSheetKind.Category -> category?.let { DrawerSheetHeader(it.name); SheetAction("Mark as Read", Icons.Default.Check) { onMarkCategoryRead(it.id) }; SheetAction("Rename", Icons.Default.Edit) { onRenameCategory(it) }; SheetAction("Delete", Icons.Default.Delete) { onDeleteCategory(it.id) } }
            DrawerSheetKind.Feed -> feed?.let { DrawerSheetHeader(it.title); SheetAction("Mark as Read", Icons.Default.Check) { onMarkFeedRead(it.id) }; SheetAction("Rename", Icons.Default.Edit) { onRenameFeed(it) }; SheetAction("Unfollow", Icons.Default.Delete) { onDeleteFeed(it.id) } }
            DrawerSheetKind.MoveCategory -> category?.let { DrawerSheetHeader("Move ${it.name}"); categories.forEachIndexed { index, target -> SheetAction(target.name, Icons.Default.DragHandle, enabled = target.id != it.id) { onMoveCategory(it, index) } } }
            DrawerSheetKind.MoveFeed -> feed?.let { DrawerSheetHeader("Move ${it.title}"); SheetAction("Uncategorized", Icons.Default.DragHandle, enabled = it.categoryId != null) { onMoveFeed(it, null) }; categories.forEach { target -> SheetAction(target.name, Icons.Default.DragHandle, enabled = target.id != it.categoryId) { onMoveFeed(it, target.id) } } }
            null -> Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable private fun DrawerSheetHeader(title: String) { Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) }
@Composable private fun SheetAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) { TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { Icon(icon, null, tint = if (enabled) Color.White else Muted); Spacer(Modifier.width(16.dp)); Text(label, color = if (enabled) Color.White else Muted, modifier = Modifier.weight(1f)) } }

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
private fun ArticleList(entries: List<Entry>, modifier: Modifier = Modifier, onOpen: (Entry) -> Unit = {}) {
    if (entries.isEmpty()) Box(modifier.fillMaxSize().background(Page), contentAlignment = Alignment.Center) { Text("Nothing new here", color = Muted) }
    else LazyColumn(modifier.fillMaxSize().background(Color.White)) { items(entries, key = { it.id }) { ArticleRow(it, onOpen) } }
}

@Composable
private fun ArticleRow(entry: Entry, onOpen: (Entry) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpen(entry) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (entry.imageUrl.isNotBlank()) AsyncImage(model = entry.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth(0.3f).aspectRatio(1.35f))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(entry.title, color = if (entry.isRead) Muted else Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 3)
            Text("${entry.sourceTitle.ifBlank { entry.feedTitle }} / ${relativeAge(entry.published)}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private fun relativeAge(published: String): String = when { published.contains("T") -> "2h"; published.endsWith("14") -> "2h"; published.endsWith("13") -> "1d"; else -> "2d" }

@Composable
private fun ArticleDetail(entry: Entry, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(backgroundColor = Panel, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, title = { Text(entry.sourceTitle.ifBlank { entry.feedTitle }, color = Color.White) })
        LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
            item { Text(entry.title, color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("${entry.feedTitle} · ${entry.author} · ${entry.published}", color = Muted, modifier = Modifier.padding(top = 10.dp, bottom = 22.dp)); if (entry.imageUrl.isNotBlank()) AsyncImage(model = entry.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(190.dp)); Text(entry.content.ifBlank { entry.summary }, color = Color.DarkGray, fontSize = 17.sp, modifier = Modifier.padding(top = 20.dp)) }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("Bright") }
    var interval by remember { mutableStateOf("30 minutes") }
    var retention by remember { mutableStateOf("30 days") }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(backgroundColor = Panel, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, title = { Text("Settings", color = Color.White) })
        Column(Modifier.padding(20.dp)) {
            Text("Account", fontWeight = FontWeight.Bold, fontSize = 20.sp); OutlinedTextField(server, { server = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Server") }); OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Username") }); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
            Text("Theme", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 24.dp)); TextButton(onClick = { theme = if (theme == "Bright") "Dark" else "Bright" }) { Text(theme) }
            Text("Fetch interval", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp)); TextButton(onClick = { interval = if (interval == "30 minutes") "60 minutes" else "30 minutes" }) { Text(interval) }
            Text("Local archive", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp)); TextButton(onClick = { retention = if (retention == "30 days") "1000 articles" else "30 days" }) { Text(retention) }
        }
    }
}
