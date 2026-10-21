package com.hermes.kidsstory.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hermes.kidsstory.data.Category
import com.hermes.kidsstory.data.LibraryScanner
import com.hermes.kidsstory.data.Prefs
import com.hermes.kidsstory.player.rememberPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun KidsStoryApp() {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var progress by remember { mutableStateOf<Prefs.Progress?>(null) }
    var themeId by remember { mutableStateOf(Prefs.loadTheme(context)) }
    var scanKey by remember { mutableIntStateOf(0) }
    val theme = themeById(themeId)
    val pc = rememberPlayerController(context)

    // 扫描放 IO 线程；scanKey 变化时重扫
    LaunchedEffect(scanKey) {
        loading = true
        categories = withContext(Dispatchers.IO) { LibraryScanner.scan(context) }
        progress = Prefs.loadProgress(context)
        loading = false
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            Prefs.saveMusicUri(context, uri.toString())
            scanKey++
            screen = Screen.Home
        }
    }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Player -> Screen.Episodes(s.category, s.story)
            is Screen.Episodes -> Screen.Stories(s.category)
            is Screen.Stories -> Screen.Home
            is Screen.Settings -> Screen.Home
            else -> Screen.Home
        }
    }

    Surface(color = theme.bg, modifier = Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                theme = theme,
                categories = categories,
                loading = loading,
                progress = progress,
                onCategory = { screen = Screen.Stories(it) },
                onSettings = { screen = Screen.Settings },
                onContinue = { cat, story, idx ->
                    pc?.playStory(story, idx)
                    screen = Screen.Player(cat, story, idx)
                }
            )

            Screen.Settings -> SettingsScreen(
                theme = theme,
                musicUri = Prefs.loadMusicUri(context),
                onBack = { screen = Screen.Home },
                onPick = { picker.launch(null) },
                onReset = {
                    Prefs.saveMusicUri(context, null)
                    scanKey++
                    screen = Screen.Home
                },
                onTheme = { id ->
                    Prefs.saveTheme(context, id)
                    themeId = id
                }
            )

            is Screen.Stories -> StoryListScreen(
                theme = theme,
                category = s.category,
                onBack = { screen = Screen.Home },
                onStory = { screen = Screen.Episodes(s.category, it) }
            )

            is Screen.Episodes -> EpisodeListScreen(
                theme = theme,
                story = s.story,
                onBack = { screen = Screen.Stories(s.category) },
                onEpisode = { idx ->
                    pc?.playStory(s.story, idx)
                    screen = Screen.Player(s.category, s.story, idx)
                }
            )

            is Screen.Player -> PlayerScreen(
                theme = theme,
                story = s.story,
                initialIndex = s.index,
                pc = pc,
                onBack = {
                    progress = Prefs.loadProgress(context)
                    screen = Screen.Episodes(s.category, s.story)
                }
            )
        }
    }
}
