package tw.idv.niugnep.pwamaker.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import tw.idv.niugnep.pwamaker.R
import tw.idv.niugnep.pwamaker.model.PwaConfig
import tw.idv.niugnep.pwamaker.model.UaMode
import tw.idv.niugnep.pwamaker.storage.PwaStorage
import tw.idv.niugnep.pwamaker.utils.ShortcutUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onOpenViewer: (PwaConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val untitledPwaStr = stringResource(R.string.untitled_pwa)

    // Form states
    var idState by remember { mutableStateOf("") }
    var nameState by remember { mutableStateOf("") }
    var urlState by remember { mutableStateOf("") }
    var iconUriState by remember { mutableStateOf<String?>(null) }
    var isAdvancedUa by remember { mutableStateOf(false) } // false = Basic, true = Advanced
    var basicUaMode by remember { mutableStateOf(UaMode.BASIC_MOBILE) } // MOBILE vs DESKTOP
    var customUaState by remember { mutableStateOf("") }

    // Saved list state
    var savedPwas by remember { mutableStateOf(emptyList<PwaConfig>()) }
    var showSavedList by remember { mutableStateOf(false) }

    fun loadSavedList() {
        savedPwas = PwaStorage.getAll(context)
    }

    LaunchedEffect(Unit) {
        loadSavedList()
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            iconUriState = uri.toString()
        }
    }

    fun getCurrentConfig(): PwaConfig {
        val selectedUaMode = if (isAdvancedUa) {
            UaMode.ADVANCED
        } else {
            basicUaMode
        }
        return PwaConfig(
            id = idState.ifBlank { UUID.randomUUID().toString() },
            name = nameState.ifBlank { untitledPwaStr },
            url = urlState,
            iconUri = iconUriState,
            uaMode = selectedUaMode,
            customUa = customUaState
        )
    }

    fun loadConfigIntoForm(config: PwaConfig) {
        idState = config.id
        nameState = config.name
        urlState = config.url
        iconUriState = config.iconUri
        if (config.uaMode == UaMode.ADVANCED) {
            isAdvancedUa = true
            customUaState = config.customUa
        } else {
            isAdvancedUa = false
            basicUaMode = config.uaMode
            customUaState = config.customUa
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.generator_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.generator_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSavedList = !showSavedList }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.cd_saved_list),
                            tint = if (savedPwas.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Saved PWAs card list expandable section
            AnimatedVisibility(visible = showSavedList) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.saved_list_title, savedPwas.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showSavedList = false }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.cd_close)
                                )
                            }
                        }

                        if (savedPwas.isEmpty()) {
                            Text(
                                text = stringResource(R.string.saved_list_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                savedPwas.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Icon preview
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!item.iconUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = item.iconUri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Language,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name.ifBlank { untitledPwaStr },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = item.url,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }

                                            // Action buttons for item
                                            IconButton(onClick = {
                                                loadConfigIntoForm(item)
                                                showSavedList = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_loaded_pwa, item.name),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = stringResource(R.string.cd_load)
                                                )
                                            }

                                            IconButton(onClick = {
                                                val config = item
                                                onOpenViewer(config)
                                            }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Launch,
                                                    contentDescription = stringResource(R.string.cd_open)
                                                )
                                            }

                                            IconButton(onClick = {
                                                PwaStorage.delete(context, item.id)
                                                loadSavedList()
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_deleted),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.cd_delete),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. App Name
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.field_name_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = { Text(stringResource(R.string.field_name_label)) },
                        placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
                        singleLine = true,
                        trailingIcon = {
                            if (nameState.isNotEmpty()) {
                                IconButton(onClick = { nameState = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.cd_clear)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. URL Input
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.field_url_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlState,
                        onValueChange = { urlState = it },
                        label = { Text(stringResource(R.string.field_url_label)) },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null)
                        },
                        trailingIcon = {
                            if (urlState.isNotEmpty()) {
                                IconButton(onClick = { urlState = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.cd_clear)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Icon Selector
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.field_icon_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Preview circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!iconUriState.isNullOrBlank()) {
                                AsyncImage(
                                    model = iconUriState,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_choose_image))
                            }

                            if (!iconUriState.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { iconUriState = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.btn_reset_icon))
                                }
                            }
                        }
                    }
                }
            }

            // 4. User-Agent Settings
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.field_ua_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode switch tabs / chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isAdvancedUa,
                            onClick = { isAdvancedUa = false },
                            label = { Text(stringResource(R.string.ua_mode_basic)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isAdvancedUa,
                            onClick = { isAdvancedUa = true },
                            label = { Text(stringResource(R.string.ua_mode_advanced)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isAdvancedUa) {
                        // Basic Mode: Radio group for Mobile vs Desktop
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { basicUaMode = UaMode.BASIC_MOBILE }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = basicUaMode == UaMode.BASIC_MOBILE,
                                    onClick = { basicUaMode = UaMode.BASIC_MOBILE }
                                )
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.ua_mobile_option))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { basicUaMode = UaMode.BASIC_DESKTOP }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = basicUaMode == UaMode.BASIC_DESKTOP,
                                    onClick = { basicUaMode = UaMode.BASIC_DESKTOP }
                                )
                                Icon(Icons.Default.Computer, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.ua_desktop_option))
                            }
                        }
                    } else {
                        // Advanced Mode: Text field for custom UA string
                        Column {
                            OutlinedTextField(
                                value = customUaState,
                                onValueChange = { customUaState = it },
                                label = { Text(stringResource(R.string.ua_custom_label)) },
                                placeholder = { Text("Mozilla/5.0 ...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(onClick = {
                                    customUaState = PwaConfig.DEFAULT_MOBILE_UA
                                }) {
                                    Text(stringResource(R.string.btn_fill_mobile_ua))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(onClick = {
                                    customUaState = PwaConfig.DEFAULT_DESKTOP_UA
                                }) {
                                    Text(stringResource(R.string.btn_fill_desktop_ua))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button: Add to Home Screen
            Button(
                onClick = {
                    val config = getCurrentConfig()
                    if (config.url.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_enter_valid_url),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    PwaStorage.save(context, config)
                    loadSavedList()
                    ShortcutUtils.createShortcut(context, config)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Shortcut, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_add_to_home_screen),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
